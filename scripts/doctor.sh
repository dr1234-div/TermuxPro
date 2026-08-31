#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$project_dir/.tooling/android-sdk}}"
failed=0

source "$project_dir/scripts/resolve-jdk17.sh"

check_file() {
    if [[ ! -e "$1" ]]; then
        echo "缺失：$1" >&2
        failed=1
    fi
}

if ! command -v git >/dev/null; then echo "缺失：git" >&2; failed=1; fi
if ! command -v java >/dev/null || ! java -version 2>&1 | head -n 1 | grep -q '"17\.' ||
   ! command -v javac >/dev/null || ! javac -version 2>&1 | grep -q '^javac 17\.'; then
    echo "JDK：需要同时包含 java 和 javac 的完整 JDK 17" >&2
    failed=1
fi
check_file "$sdk_root/platforms/android-36/android.jar"
check_file "$sdk_root/build-tools/35.0.0/aapt"
check_file "$sdk_root/platform-tools/adb"
check_file "$sdk_root/ndk/29.0.14206865/source.properties"
check_file "$project_dir/gradlew"

if (( failed != 0 )); then
    echo "环境检查未通过，请阅读 docs/DEVELOPMENT.md。" >&2
    exit 1
fi
echo "TermuxPro 开发环境检查通过。"
