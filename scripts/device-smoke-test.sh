#!/usr/bin/env bash
set -euo pipefail

REPOSITORY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ANDROID_HOME:-${REPOSITORY_ROOT}/.tooling/android-sdk}/platform-tools/adb"
APK="${1:-${REPOSITORY_ROOT}/app/build/outputs/apk/release/termux-app_apt-android-7-release_arm64-v8a.apk}"
PACKAGE="com.termux"
LAUNCHER="com.termux.app.WorkspaceActivity"

if [[ ! -x "${ADB}" ]]; then
    echo "错误：未找到可执行的 adb：${ADB}" >&2
    exit 2
fi
if [[ ! -f "${APK}" ]]; then
    echo "错误：未找到 APK：${APK}" >&2
    exit 2
fi

mapfile -t DEVICES < <("${ADB}" devices | awk 'NR > 1 && $2 == "device" { print $1 }')
if [[ ${#DEVICES[@]} -ne 1 ]]; then
    echo "错误：需要且只能连接一台已授权设备，当前为 ${#DEVICES[@]} 台。" >&2
    "${ADB}" devices -l >&2
    exit 3
fi

SERIAL="${DEVICES[0]}"
echo "设备：${SERIAL}"
echo "APK：${APK}"
sha256sum "${APK}"

# 只执行覆盖安装，不自动卸载，避免误删现有 Termux 数据。
"${ADB}" -s "${SERIAL}" install -r "${APK}"
"${ADB}" -s "${SERIAL}" shell am force-stop "${PACKAGE}"
"${ADB}" -s "${SERIAL}" shell am start -W -n "${PACKAGE}/${LAUNCHER}"

PID="$("${ADB}" -s "${SERIAL}" shell pidof "${PACKAGE}" | tr -d '\r')"
if [[ -z "${PID}" ]]; then
    echo "错误：应用启动后未发现进程。" >&2
    exit 4
fi

TOP_ACTIVITY="$("${ADB}" -s "${SERIAL}" shell dumpsys activity activities | awk '/mResumedActivity/ { print; exit }')"
if [[ "${TOP_ACTIVITY}" != *"${PACKAGE}/${LAUNCHER}"* ]]; then
    echo "错误：首页未成为前台 Activity：${TOP_ACTIVITY}" >&2
    exit 5
fi

CRASHES="$("${ADB}" -s "${SERIAL}" logcat -d -t 400 AndroidRuntime:E '*:S' || true)"
if [[ "${CRASHES}" == *"FATAL EXCEPTION"* && "${CRASHES}" == *"${PACKAGE}"* ]]; then
    echo "错误：检测到应用崩溃。" >&2
    echo "${CRASHES}" >&2
    exit 6
fi

echo "通过：APK 安装、进程启动、中文工作区首页前台检查。"
