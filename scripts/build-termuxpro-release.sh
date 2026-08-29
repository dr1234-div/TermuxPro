#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

bundled_jdk="$project_dir/.tooling/jdk-deb/full/usr/lib/jvm/java-17-openjdk-amd64"
if [[ -x "$bundled_jdk/bin/java" ]]; then
    export JAVA_HOME="$bundled_jdk"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

if [[ ! -f .signing/termuxpro-release.p12 || ! -f .signing/termuxpro-release.pass ]]; then
    echo "缺少 Release 签名文件，请阅读 docs/RELEASE_SIGNING.md。" >&2
    exit 1
fi

export TERMUX_SPLIT_APKS_FOR_RELEASE_BUILDS=1
gradle_args=(--no-daemon --max-workers=2)
if [[ "${TERMUXPRO_OFFLINE:-0}" == "1" ]]; then
    gradle_args+=(--offline)
fi
./gradlew "${gradle_args[@]}" test lint :app:assembleRelease

apk="app/build/outputs/apk/release/termux-app_apt-android-7-release_arm64-v8a.apk"
if [[ ! -f "$apk" ]]; then
    echo "构建结束但未找到 ARM64 APK：$apk" >&2
    exit 2
fi

sdk_root="${ANDROID_HOME:-$project_dir/.tooling/android-sdk}"
build_tools="$sdk_root/build-tools/35.0.0"
if [[ ! -x "$build_tools/apksigner" || ! -x "$build_tools/aapt" ]]; then
    echo "缺少 Android Build Tools 35.0.0，无法验证发布产物。" >&2
    exit 3
fi

expected_certificate="10c820d392d5bd9672317067a5d11fc1250217824166961df7b385128d4b49cd"
badging="$($build_tools/aapt dump badging "$apk")"
signature="$($build_tools/apksigner verify --verbose --print-certs "$apk")"

if [[ "$badging" != *"package: name='com.termux' versionCode='10000' versionName='0.1.0'"* ]]; then
    echo "APK 包名或版本不符合 0.1.0 发布约束。" >&2
    exit 4
fi
if [[ "$badging" != *"launchable-activity: name='com.termux.app.WorkspaceActivity'"* ]]; then
    echo "APK 启动页不是 WorkspaceActivity。" >&2
    exit 5
fi
if [[ "$badging" != *"native-code: 'arm64-v8a'"* ]]; then
    echo "APK 不是纯 ARM64 产物。" >&2
    exit 6
fi
if [[ "$signature" != *"Verified using v1 scheme (JAR signing): true"* ||
      "$signature" != *"Verified using v2 scheme (APK Signature Scheme v2): true"* ||
      "$signature" != *"Signer #1 certificate SHA-256 digest: $expected_certificate"* ]]; then
    echo "APK 签名方案或证书指纹不符合发布约束。" >&2
    exit 7
fi

dist_dir="$project_dir/dist/0.1.0"
dist_apk="$dist_dir/termuxpro-0.1.0-arm64-v8a.apk"
mkdir -p "$dist_dir"
cp "$apk" "$dist_apk"
cp "$project_dir/docs/RELEASE_NOTES_0.1.0.md" "$dist_dir/RELEASE_NOTES.md"
cp "$project_dir/docs/DEVICE_ACCEPTANCE.md" "$dist_dir/DEVICE_ACCEPTANCE.md"
sha256sum "$dist_apk" > "$dist_dir/SHA256SUMS"
printf '%s\n' "$signature" > "$dist_dir/APK_SIGNATURE.txt"
{
    echo "产品：TermuxPro"
    echo "版本：0.1.0 (10000)"
    echo "包名：com.termux"
    echo "架构：arm64-v8a"
    echo "Git：$(git rev-parse --short HEAD)$(git diff --quiet && git diff --cached --quiet || printf '%s' '-dirty')"
    echo "构建时间 UTC：$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
    echo "证书 SHA-256：$expected_certificate"
} > "$dist_dir/BUILD_INFO.txt"

echo "发布产物已生成："
echo "$dist_apk"
cat "$dist_dir/SHA256SUMS"
