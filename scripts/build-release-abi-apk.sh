#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
git_ref="${1:?用法: build-release-abi-apk.sh <git-ref> <abi> <输出 APK>}"
abi="${2:?缺少 ABI，例如 x86_64 或 arm64-v8a}"
output_apk="${3:?缺少输出 APK 路径}"

case "$abi" in
    x86|x86_64|armeabi-v7a|arm64-v8a) ;;
    *)
        echo "不支持的 ABI：$abi" >&2
        exit 2
        ;;
esac

if [[ ! -f "$project_dir/.signing/termuxpro-release.p12" ||
      ! -f "$project_dir/.signing/termuxpro-release.pass" ]]; then
    echo "缺少 Release 签名材料，无法构建 ABI 验收 APK。" >&2
    exit 3
fi

worktree_parent="$(mktemp -d)"
worktree_dir="$worktree_parent/source"
cleanup() {
    git -C "$project_dir" worktree remove --force "$worktree_dir" >/dev/null 2>&1 || true
    rm -rf -- "$worktree_parent"
}
trap cleanup EXIT

git -C "$project_dir" worktree add --detach "$worktree_dir" "$git_ref" >/dev/null
install -d -m 700 "$worktree_dir/.signing"
install -m 600 "$project_dir/.signing/termuxpro-release.p12" "$worktree_dir/.signing/termuxpro-release.p12"
install -m 600 "$project_dir/.signing/termuxpro-release.pass" "$worktree_dir/.signing/termuxpro-release.pass"

cd "$worktree_dir"
source "$worktree_dir/scripts/resolve-jdk17.sh"
export ANDROID_HOME="${ANDROID_HOME:-$project_dir/.tooling/android-sdk}"
export TERMUX_SPLIT_APKS_FOR_RELEASE_BUILDS=1
gradle_args=(--no-daemon --max-workers=2)
if [[ "${TERMUXPRO_OFFLINE:-0}" == "1" ]]; then
    gradle_args+=(--offline)
fi
./gradlew "${gradle_args[@]}" :app:downloadBootstraps :app:assembleRelease

apk="app/build/outputs/apk/release/termux-app_apt-android-7-release_${abi}.apk"
if [[ ! -f "$apk" ]]; then
    echo "构建结束但未找到 ${abi} APK：$apk" >&2
    exit 4
fi

sdk_root="${ANDROID_HOME:-$project_dir/.tooling/android-sdk}"
build_tools="$sdk_root/build-tools/35.0.0"
if [[ ! -x "$build_tools/aapt" ]]; then
    echo "缺少 Android Build Tools 35.0.0 aapt，无法验证 ABI APK。" >&2
    exit 5
fi
badging="$("$build_tools/aapt" dump badging "$apk")"
if [[ "$badging" != *"package: name='com.termux'"* ||
      "$badging" != *"application-label:'TermuxPro'"* ||
      "$badging" != *"native-code: '$abi'"* ]]; then
    echo "${abi} APK 元数据不符合发布验收要求。" >&2
    exit 6
fi

mkdir -p "$(dirname "$output_apk")"
cp "$apk" "$output_apk"
echo "ABI 验收 APK 已生成：$output_apk"
