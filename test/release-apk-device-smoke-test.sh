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
  'install '*) echo Success ;;
  'install -r '*) echo Success ;;
  'shell cmd package list packages -U com.termux') echo 'package:com.termux uid:10123' ;;
  'shell dumpsys package com.termux') printf 'versionCode=40001 minSdk=24\nversionName=0.4.0-rc.1\n' ;;
  'shell monkey -p com.termux -c android.intent.category.LAUNCHER 1') echo 'Events injected: 1' ;;
  'shell pidof com.termux') echo 1234 ;;
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

if FAKE_LABEL=Wrong ADB="$temp_dir/fake-adb" AAPT="$temp_dir/fake-aapt" \
  "$project_dir/scripts/verify-release-apk-on-device.sh" \
  "$temp_dir/stable.apk" "$temp_dir/candidate.apk" 0.4.0-rc.1 40001 \
  "$temp_dir/wrong-label" >/dev/null 2>&1; then
    echo '错误桌面名称未被拒绝。' >&2
    exit 1
fi

echo 'Release APK 设备冒烟脚本测试通过。'
