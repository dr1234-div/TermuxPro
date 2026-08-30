#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
manifest="$project_dir/test/plans/ui-screenshot-manifest.txt"
screenshot_dir="${1:?用法: verify-ui-screenshots.sh <截图目录> <后缀>}"
suffix="${2:?用法: verify-ui-screenshots.sh <截图目录> <后缀>}"

if [[ ! -d "$screenshot_dir" ]]; then
    echo "截图目录不存在：$screenshot_dir" >&2
    exit 2
fi

temp_dir="$(mktemp -d)"
trap 'rm -rf -- "$temp_dir"' EXIT

sed -e 's/[[:space:]]*#.*$//' -e '/^[[:space:]]*$/d' "$manifest" \
    | LC_ALL=C sort -u > "$temp_dir/expected"
find "$screenshot_dir" -maxdepth 1 -type f -name "*-${suffix}.png" -printf '%f\n' \
    | sed "s/-${suffix}\\.png$//" \
    | LC_ALL=C sort -u > "$temp_dir/actual"

if ! diff -u "$temp_dir/expected" "$temp_dir/actual"; then
    echo "UI 截图集合与清单不一致（后缀：$suffix）。" >&2
    exit 1
fi
