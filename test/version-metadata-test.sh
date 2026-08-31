#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
version_name="$($project_dir/scripts/termuxpro-version.sh name)"
version_code="$($project_dir/scripts/termuxpro-version.sh code)"
release_notes="$($project_dir/scripts/termuxpro-version.sh notes)"

if [[ ! "$version_name" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]]; then
    echo "版本名不是合法语义版本：$version_name" >&2
    exit 1
fi
if [[ ! "$version_code" =~ ^[1-9][0-9]*$ ]]; then
    echo "versionCode 必须为正整数：$version_code" >&2
    exit 1
fi
expected_release_notes="docs/RELEASE_NOTES_${version_name}.md"
if [[ "$release_notes" != "$expected_release_notes" || ! -f "$project_dir/$release_notes" ]]; then
    echo "发布说明必须存在并与版本一致：$expected_release_notes" >&2
    exit 1
fi
[[ "$($project_dir/scripts/termuxpro-version.sh tag "v$version_name")" == "v$version_name" ]]
if "$project_dir/scripts/termuxpro-version.sh" tag "v9.9.9" >/dev/null 2>&1; then
    echo "不一致的发布标签未被拒绝。" >&2
    exit 1
fi
if grep -Eq 'dist/[0-9]+\.[0-9]+\.[0-9]+|termuxpro-[0-9]+\.[0-9]+\.[0-9]+[^$]*arm64' \
    "$project_dir/.github/workflows/release.yml" "$project_dir/scripts/build-termuxpro-release.sh"; then
    echo "发布链仍包含写死的 0.1.0 产物路径。" >&2
    exit 1
fi
if grep -Fq 'sha256sum "$dist_apk"' "$project_dir/scripts/build-termuxpro-release.sh"; then
    echo "SHA256SUMS 仍会写入构建机器绝对路径。" >&2
    exit 1
fi
release_workflow="$project_dir/.github/workflows/release.yml"
if grep -Fq 'gh workflow run ui-emulator.yml' "$release_workflow"; then
    echo "Release 仍通过跨工作流调度验收，存在并发关联和竞态风险。" >&2
    exit 1
fi
gate_line="$(grep -n '验收待发布签名 APK 的覆盖升级' "$release_workflow" | cut -d: -f1)"
publish_line="$(grep -n '创建 GitHub Release' "$release_workflow" | cut -d: -f1)"
if [[ -z "$gate_line" || -z "$publish_line" || "$gate_line" -ge "$publish_line" ]]; then
    echo "Release 必须先验收待发布签名 APK，再创建公开 GitHub Release。" >&2
    exit 1
fi
if ! grep -Fq './scripts/select-stable-baseline-tag.sh "$version_name"' "$release_workflow"; then
    echo "Release 未显式选择低于候选版本的稳定基线。" >&2
    exit 1
fi
if grep -Fq 'actions: write' "$release_workflow"; then
    echo "Release 不应再申请跨工作流调度所需的 actions: write 权限。" >&2
    exit 1
fi

echo "版本元数据校验通过：$version_name ($version_code)"
