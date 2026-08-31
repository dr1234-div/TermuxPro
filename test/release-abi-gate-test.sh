#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
script="$project_dir/scripts/build-release-abi-apk.sh"
workflow="$project_dir/.github/workflows/release.yml"

if [[ ! -x "$script" ]]; then
    echo "ABI 验收 APK 构建脚本必须可执行：$script" >&2
    exit 1
fi
if ! grep -Fq "worktree add --detach" "$script"; then
    echo "ABI 验收基线必须从稳定标签隔离 worktree 构建，禁止污染当前工作树。" >&2
    exit 1
fi
if ! grep -Fq "TERMUX_SPLIT_APKS_FOR_RELEASE_BUILDS=1" "$script"; then
    echo "ABI 验收构建必须启用 split APK，确保能生成 x86_64 门禁 APK。" >&2
    exit 1
fi
if ! grep -Fq ":app:downloadBootstraps" "$script"; then
    echo "ABI 验收基线从干净 worktree 构建时必须先下载 bootstrap，否则 NDK incbin 会缺文件。" >&2
    exit 1
fi
if ! grep -Fq 'native-code: '"'"'$abi'"'" "$script"; then
    echo "ABI 验收构建必须校验 APK native-code 与目标 ABI 一致。" >&2
    exit 1
fi
if ! grep -Fq "public_candidate_apk" "$workflow"; then
    echo "Release workflow 必须保留公开 arm64 APK 校验。" >&2
    exit 1
fi
if ! grep -Fq "gate_abi=\"x86_64\"" "$workflow"; then
    echo "Release workflow 必须显式使用 x86_64 模拟器验收 ABI。" >&2
    exit 1
fi

echo "Release ABI 验收门禁脚本测试通过。"
