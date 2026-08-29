#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
version_name="$($project_dir/scripts/termuxpro-version.sh name)"
version_code="$($project_dir/scripts/termuxpro-version.sh code)"
release_notes="$($project_dir/scripts/termuxpro-version.sh notes)"

[[ "$version_name" == "0.1.0" ]]
[[ "$version_code" == "10000" ]]
[[ -f "$project_dir/$release_notes" ]]
[[ "$($project_dir/scripts/termuxpro-version.sh tag "v$version_name")" == "v$version_name" ]]
if "$project_dir/scripts/termuxpro-version.sh" tag "v9.9.9" >/dev/null 2>&1; then
    echo "不一致的发布标签未被拒绝。" >&2
    exit 1
fi
if grep -Eq 'dist/0\.1\.0|termuxpro-0\.1\.0-arm64' \
    "$project_dir/.github/workflows/release.yml" "$project_dir/scripts/build-termuxpro-release.sh"; then
    echo "发布链仍包含写死的 0.1.0 产物路径。" >&2
    exit 1
fi

echo "版本元数据校验通过：$version_name ($version_code)"
