#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"

if [[ -z "$sdk_root" ]]; then
    echo "请先设置 ANDROID_SDK_ROOT（或 ANDROID_HOME）指向 Android SDK。" >&2
    exit 1
fi
if [[ ! -x "$sdk_root/cmdline-tools/latest/bin/sdkmanager" ]]; then
    echo "未找到 cmdline-tools/latest/bin/sdkmanager，请先通过 Android Studio 安装 Command-line Tools。" >&2
    exit 2
fi
if ! command -v java >/dev/null || ! java -version 2>&1 | head -n 1 | grep -q '"17\.' ||
   ! command -v javac >/dev/null || ! javac -version 2>&1 | grep -q '^javac 17\.'; then
    echo "需要完整 JDK 17，并确保 java 与 javac 均位于 PATH。" >&2
    exit 3
fi

"$sdk_root/cmdline-tools/latest/bin/sdkmanager" \
    "platforms;android-36" \
    "build-tools;35.0.0" \
    "platform-tools" \
    "ndk;29.0.14206865"

escaped_sdk="${sdk_root//\\/\\\\}"
escaped_sdk="${escaped_sdk//:/\\:}"
printf 'sdk.dir=%s\n' "$escaped_sdk" > "$project_dir/local.properties"
echo "Android 开发环境已准备完成。"
