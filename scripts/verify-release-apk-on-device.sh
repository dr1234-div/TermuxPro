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

require_contains() {
    local haystack="${1-}"
    local needle="${2:?}"
    local message="${3:?}"
    if [[ "$haystack" != *"$needle"* ]]; then
        echo "$message" >&2
        echo "期望包含：$needle" >&2
        exit 1
    fi
}

require_regex() {
    local haystack="${1-}"
    local pattern="${2:?}"
    local message="${3:?}"
    if ! grep -Eq "$pattern" <<<"$haystack"; then
        echo "$message" >&2
        echo "期望匹配：$pattern" >&2
        exit 1
    fi
}

read_adb_shell_line() {
    "$adb_bin" shell "$@" 2>/dev/null | tr -d '\r' | sed -n '1p' || true
}

install_apk() {
    local mode="${1:?}"
    local apk="${2:?}"
    local output_file="${3:?}"
    local install_output
    if [[ "$mode" == "replace" ]]; then
        install_output="$("$adb_bin" install -r "$apk" 2>&1 || true)"
    else
        install_output="$("$adb_bin" install "$apk" 2>&1 || true)"
    fi
    printf '%s\n' "$install_output" | tee "$output_file"
    if ! grep -Fxq Success <<<"$install_output"; then
        echo "APK 安装失败：$apk" >&2
        exit 1
    fi
}

device_state="$("$adb_bin" get-state 2>&1 || true)"
if [[ "$device_state" != "device" ]]; then
    echo "ADB 设备未就绪：$device_state" >&2
    exit 1
fi
badging="$($aapt_bin dump badging "$candidate_apk")"
require_contains "$badging" "package: name='com.termux' versionCode='$expected_code' versionName='$expected_name'" \
    "候选 APK 包名或版本不符合发布期望。"
require_contains "$badging" "application-label:'TermuxPro'" \
    "候选 APK 桌面名称不是 TermuxPro。"

"$adb_bin" uninstall com.termux >/dev/null 2>&1 || true
install_apk fresh "$baseline_apk" "$evidence_dir/install-baseline.txt"
baseline_uid="$($adb_bin shell cmd package list packages -U com.termux | tr -d '\r' | sed -n 's/.*uid://p')"
[[ -n "$baseline_uid" ]] || { echo '无法读取稳定版应用 UID。' >&2; exit 1; }

install_apk replace "$candidate_apk" "$evidence_dir/install-candidate.txt"
candidate_uid="$($adb_bin shell cmd package list packages -U com.termux | tr -d '\r' | sed -n 's/.*uid://p')"
[[ "$candidate_uid" == "$baseline_uid" ]] || {
    echo "覆盖升级后 UID 变化：$baseline_uid -> $candidate_uid" >&2
    exit 1
}

package_state="$($adb_bin shell dumpsys package com.termux | tr -d '\r')"
require_regex "$package_state" "versionCode=${expected_code}([[:space:]]|$)" \
    "覆盖升级后设备上的 versionCode 不正确。"
require_contains "$package_state" "versionName=$expected_name" \
    "覆盖升级后设备上的 versionName 不正确。"

"$adb_bin" logcat -c
"$adb_bin" shell am force-stop com.termux
launch_output="$($adb_bin shell monkey -p com.termux -c android.intent.category.LAUNCHER 1 2>&1)"
require_contains "$launch_output" 'Events injected: 1' "启动候选 APK 失败。"
for _ in $(seq 1 20); do
    if "$adb_bin" shell pidof com.termux | grep -Eq '[0-9]'; then break; fi
    sleep 1
done
pid_output="$("$adb_bin" shell pidof com.termux 2>&1 || true)"
require_regex "$pid_output" '[0-9]' "候选 APK 启动后未检测到 com.termux 进程。"
sleep 3

android_runtime="$($adb_bin logcat -d -v brief AndroidRuntime:E '*:S' | tr -d '\r')"
fatal_exception=false
if grep -Fq 'FATAL EXCEPTION' <<<"$android_runtime"; then
    fatal_exception=true
fi
{
    printf 'deviceManufacturer=%s\n' "$(read_adb_shell_line getprop ro.product.manufacturer)"
    printf 'deviceModel=%s\n' "$(read_adb_shell_line getprop ro.product.model)"
    printf 'androidRelease=%s\n' "$(read_adb_shell_line getprop ro.build.version.release)"
    printf 'androidSdk=%s\n' "$(read_adb_shell_line getprop ro.build.version.sdk)"
    printf 'supportedAbis=%s\n' "$(read_adb_shell_line getprop ro.product.cpu.abilist)"
    printf 'defaultInputMethod=%s\n' "$(read_adb_shell_line settings get secure default_input_method)"
    printf 'packageName=com.termux\n'
    printf 'versionName=%s\n' "$expected_name"
    printf 'versionCode=%s\n' "$expected_code"
    printf 'processPid=%s\n' "$pid_output"
    printf 'androidRuntimeFatalException=%s\n' "$fatal_exception"
    if [[ -n "$android_runtime" ]]; then
        printf 'androidRuntimeLog<<EOF\n%s\nEOF\n' "$android_runtime"
    else
        printf 'androidRuntimeLog=EMPTY_NO_FATAL_EXCEPTION\n'
    fi
} > "$evidence_dir/android-runtime.txt"
if [[ "$fatal_exception" == true ]]; then
    echo '候选 APK 启动后出现 AndroidRuntime FATAL EXCEPTION。' >&2
    exit 1
fi

printf 'baselineUid=%s\ncandidateUid=%s\nversionName=%s\nversionCode=%s\n' \
    "$baseline_uid" "$candidate_uid" "$expected_name" "$expected_code" \
    > "$evidence_dir/result.txt"
echo "正式签名 APK 覆盖升级与启动冒烟通过：$expected_name ($expected_code)"
