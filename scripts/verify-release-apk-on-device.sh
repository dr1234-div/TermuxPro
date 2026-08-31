#!/usr/bin/env bash
set -euo pipefail

baseline_apk="${1:?用法: verify-release-apk-on-device.sh <稳定版 APK> <候选 APK> <versionName> <versionCode> <证据目录>}"
candidate_apk="${2:?用法: verify-release-apk-on-device.sh <稳定版 APK> <候选 APK> <versionName> <versionCode> <证据目录>}"
expected_name="${3:?缺少候选 versionName}"
expected_code="${4:?缺少候选 versionCode}"
evidence_dir="${5:?缺少证据目录}"
adb_bin="${ADB:-adb}"
aapt_bin="${AAPT:-aapt}"

[[ -f "$baseline_apk" ]] || { echo "稳定版 APK 不存在：$baseline_apk" >&2; exit 2; }
[[ -f "$candidate_apk" ]] || { echo "候选 APK 不存在：$candidate_apk" >&2; exit 2; }
[[ "$expected_code" =~ ^[1-9][0-9]*$ ]] || { echo "versionCode 无效：$expected_code" >&2; exit 2; }
mkdir -p "$evidence_dir"

collect_evidence() {
    "$adb_bin" logcat -d -v threadtime > "$evidence_dir/logcat.txt" 2>&1 || true
    "$adb_bin" shell uiautomator dump /sdcard/termuxpro-release-window.xml >/dev/null 2>&1 || true
    "$adb_bin" pull /sdcard/termuxpro-release-window.xml "$evidence_dir/window.xml" >/dev/null 2>&1 || true
    "$adb_bin" exec-out screencap -p > "$evidence_dir/screenshot.png" 2>/dev/null || true
    "$adb_bin" shell dumpsys package com.termux > "$evidence_dir/package.txt" 2>&1 || true
}
trap collect_evidence EXIT

"$adb_bin" get-state | grep -Fxq device
badging="$($aapt_bin dump badging "$candidate_apk")"
grep -Fq "package: name='com.termux' versionCode='$expected_code' versionName='$expected_name'" <<<"$badging"
grep -Fq "application-label:'TermuxPro'" <<<"$badging"

"$adb_bin" uninstall com.termux >/dev/null 2>&1 || true
"$adb_bin" install "$baseline_apk" | tee "$evidence_dir/install-baseline.txt" | grep -Fxq Success
baseline_uid="$($adb_bin shell cmd package list packages -U com.termux | tr -d '\r' | sed -n 's/.*uid://p')"
[[ -n "$baseline_uid" ]] || { echo '无法读取稳定版应用 UID。' >&2; exit 1; }

"$adb_bin" install -r "$candidate_apk" | tee "$evidence_dir/install-candidate.txt" | grep -Fxq Success
candidate_uid="$($adb_bin shell cmd package list packages -U com.termux | tr -d '\r' | sed -n 's/.*uid://p')"
[[ "$candidate_uid" == "$baseline_uid" ]] || {
    echo "覆盖升级后 UID 变化：$baseline_uid -> $candidate_uid" >&2
    exit 1
}

package_state="$($adb_bin shell dumpsys package com.termux | tr -d '\r')"
grep -Eq "versionCode=${expected_code}([[:space:]]|$)" <<<"$package_state"
grep -Fq "versionName=$expected_name" <<<"$package_state"

"$adb_bin" logcat -c
"$adb_bin" shell am force-stop com.termux
launch_output="$($adb_bin shell monkey -p com.termux -c android.intent.category.LAUNCHER 1 2>&1)"
grep -Fq 'Events injected: 1' <<<"$launch_output"
for _ in $(seq 1 20); do
    if "$adb_bin" shell pidof com.termux | grep -Eq '[0-9]'; then break; fi
    sleep 1
done
"$adb_bin" shell pidof com.termux | grep -Eq '[0-9]'
sleep 3

android_runtime="$($adb_bin logcat -d -v brief AndroidRuntime:E '*:S' | tr -d '\r')"
printf '%s\n' "$android_runtime" > "$evidence_dir/android-runtime.txt"
if grep -Fq 'FATAL EXCEPTION' <<<"$android_runtime"; then
    echo '候选 APK 启动后出现 AndroidRuntime FATAL EXCEPTION。' >&2
    exit 1
fi

printf 'baselineUid=%s\ncandidateUid=%s\nversionName=%s\nversionCode=%s\n' \
    "$baseline_uid" "$candidate_uid" "$expected_name" "$expected_code" \
    > "$evidence_dir/result.txt"
echo "正式签名 APK 覆盖升级与启动冒烟通过：$expected_name ($expected_code)"
