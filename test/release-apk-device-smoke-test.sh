#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
temp_dir="$(mktemp -d)"
trap 'rm -rf -- "$temp_dir"' EXIT
touch "$temp_dir/stable.apk" "$temp_dir/candidate.apk"

cat > "$temp_dir/fake-aapt" <<'FAKE_AAPT'
#!/usr/bin/env bash
printf "package: name='com.termux' versionCode='40001' versionName='0.4.0-rc.1'\n"
printf "application-label:'%s'\n" "${FAKE_LABEL:-TermuxPro}"
FAKE_AAPT

cat > "$temp_dir/fake-adb" <<'FAKE_ADB'
#!/usr/bin/env bash
case "$*" in
  get-state) echo device ;;
  'install '*) printf 'Performing Streamed Install\nSuccess\n' ;;
  'install -r '*) printf 'Performing Streamed Install\nSuccess\n' ;;
  'shell cmd package list packages -U com.termux') echo 'package:com.termux uid:10123' ;;
  'shell dumpsys package com.termux')
    if [[ "${FAKE_EMPTY_PACKAGE:-}" != "1" ]]; then
      printf 'versionCode=40001 minSdk=24\nversionName=0.4.0-rc.1\n'
    fi
    ;;
  'shell dumpsys activity activities')
    echo 'mResumedActivity: ActivityRecord{123 u0 com.termux/.app.WorkspaceActivity t1}'
    ;;
  'shell monkey -p com.termux -c android.intent.category.LAUNCHER 1') echo 'Events injected: 1' ;;
  'shell am start -W -n com.termux/com.termux.app.WorkspaceActivity')
    printf 'Starting: Intent { cmp=com.termux/.app.WorkspaceActivity }\nStatus: ok\n'
    ;;
  'shell pidof com.termux')
    if [[ "${FAKE_DELAY_PID:-}" == "1" ]]; then
      count_file="${FAKE_PID_COUNT_FILE:?}"
      count=0
      [[ -f "$count_file" ]] && count="$(cat "$count_file")"
      count=$((count + 1))
      printf '%s' "$count" > "$count_file"
      if [[ "$count" -lt 3 ]]; then
        exit 0
      fi
    fi
    echo 1234
    ;;
  'shell getprop ro.product.manufacturer') echo Google ;;
  'shell getprop ro.product.model') echo sdk_gphone64_x86_64 ;;
  'shell getprop ro.build.version.release') echo 15 ;;
  'shell getprop ro.build.version.sdk') echo 35 ;;
  'shell getprop ro.product.cpu.abilist') echo x86_64,arm64-v8a ;;
  'shell settings get secure default_input_method') echo com.google.android.inputmethod.latin/.LatinIME ;;
  'logcat -d -v brief AndroidRuntime:E *:S')
    if [[ "${FAKE_FATAL:-}" == "1" ]]; then
      printf 'E/AndroidRuntime: FATAL EXCEPTION: main\n'
    fi
    ;;
  'pull '*) printf '<hierarchy package="com.termux"/>\n' > "${3:-/dev/null}" ;;
  'exec-out screencap -p') printf 'PNG' ;;
  *) : ;;
esac
FAKE_ADB
chmod +x "$temp_dir/fake-aapt" "$temp_dir/fake-adb"

ADB="$temp_dir/fake-adb" AAPT="$temp_dir/fake-aapt" \
  "$project_dir/scripts/verify-release-apk-on-device.sh" \
  "$temp_dir/stable.apk" "$temp_dir/candidate.apk" 0.4.0-rc.1 40001 "$temp_dir/evidence"
grep -Fqx 'baselineUid=10123' "$temp_dir/evidence/result.txt"
grep -Fqx 'candidateUid=10123' "$temp_dir/evidence/result.txt"
grep -Fqx 'versionName=0.4.0-rc.1' "$temp_dir/evidence/result.txt"
grep -Fqx 'deviceManufacturer=Google' "$temp_dir/evidence/android-runtime.txt"
grep -Fqx 'deviceModel=sdk_gphone64_x86_64' "$temp_dir/evidence/android-runtime.txt"
grep -Fqx 'androidRelease=15' "$temp_dir/evidence/android-runtime.txt"
grep -Fqx 'androidSdk=35' "$temp_dir/evidence/android-runtime.txt"
grep -Fqx 'supportedAbis=x86_64,arm64-v8a' "$temp_dir/evidence/android-runtime.txt"
grep -Fqx 'defaultInputMethod=com.google.android.inputmethod.latin/.LatinIME' "$temp_dir/evidence/android-runtime.txt"
grep -Fqx 'androidRuntimeFatalException=false' "$temp_dir/evidence/android-runtime.txt"
grep -Fqx 'androidRuntimeLog=EMPTY_NO_FATAL_EXCEPTION' "$temp_dir/evidence/android-runtime.txt"
grep -Fq 'Status: ok' "$temp_dir/evidence/launch-candidate.txt"
grep -Fq 'attempt=1' "$temp_dir/evidence/process-wait.txt"

delayed_pid_count="$temp_dir/delayed-pid-count"
FAKE_DELAY_PID=1 FAKE_PID_COUNT_FILE="$delayed_pid_count" \
  ADB="$temp_dir/fake-adb" AAPT="$temp_dir/fake-aapt" \
  "$project_dir/scripts/verify-release-apk-on-device.sh" \
  "$temp_dir/stable.apk" "$temp_dir/candidate.apk" 0.4.0-rc.1 40001 "$temp_dir/delayed-pid"
grep -Fq 'attempt=3' "$temp_dir/delayed-pid/process-wait.txt"

if FAKE_FATAL=1 ADB="$temp_dir/fake-adb" AAPT="$temp_dir/fake-aapt" \
  "$project_dir/scripts/verify-release-apk-on-device.sh" \
  "$temp_dir/stable.apk" "$temp_dir/candidate.apk" 0.4.0-rc.1 40001 \
  "$temp_dir/fatal" >/tmp/termuxpro-release-fatal.log 2>&1; then
    echo 'AndroidRuntime Fatal Exception 不应通过。' >&2
    exit 1
fi
grep -Fq '候选 APK 启动后出现 AndroidRuntime FATAL EXCEPTION' /tmp/termuxpro-release-fatal.log
grep -Fqx 'androidRuntimeFatalException=true' "$temp_dir/fatal/android-runtime.txt"

if FAKE_LABEL=Wrong ADB="$temp_dir/fake-adb" AAPT="$temp_dir/fake-aapt" \
  "$project_dir/scripts/verify-release-apk-on-device.sh" \
  "$temp_dir/stable.apk" "$temp_dir/candidate.apk" 0.4.0-rc.1 40001 \
  "$temp_dir/wrong-label" >/dev/null 2>&1; then
    echo '错误桌面名称未被拒绝。' >&2
    exit 1
fi

if ADB="$temp_dir/missing-adb" AAPT="$temp_dir/fake-aapt" \
  "$project_dir/scripts/verify-release-apk-on-device.sh" \
  "$temp_dir/stable.apk" "$temp_dir/candidate.apk" 0.4.0-rc.1 40001 \
  "$temp_dir/no-device" >/tmp/termuxpro-release-no-device.log 2>&1; then
    echo 'ADB 不可用时不应通过。' >&2
    exit 1
fi
grep -Fq 'ADB 设备未就绪' /tmp/termuxpro-release-no-device.log

if FAKE_EMPTY_PACKAGE=1 ADB="$temp_dir/fake-adb" AAPT="$temp_dir/fake-aapt" \
  "$project_dir/scripts/verify-release-apk-on-device.sh" \
  "$temp_dir/stable.apk" "$temp_dir/candidate.apk" 0.4.0-rc.1 40001 \
  "$temp_dir/empty-package" >/tmp/termuxpro-release-empty-package.log 2>&1; then
    echo '设备 package 信息为空时不应通过。' >&2
    exit 1
fi
grep -Fq '覆盖升级后设备上的 versionCode 不正确。' /tmp/termuxpro-release-empty-package.log
if grep -Fq 'parameter null or not set' /tmp/termuxpro-release-empty-package.log; then
    echo '空设备输出不应暴露 Bash 参数错误。' >&2
    exit 1
fi

echo 'Release APK 设备冒烟脚本测试通过。'
