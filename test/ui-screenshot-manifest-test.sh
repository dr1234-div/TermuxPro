#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
manifest="$project_dir/test/plans/ui-screenshot-manifest.txt"
temp_dir="$(mktemp -d)"
trap 'rm -rf -- "$temp_dir"' EXIT

while IFS= read -r name; do
    [[ -n "$name" ]] && : > "$temp_dir/$name-default.png"
done < <(sed -e 's/[[:space:]]*#.*$//' -e '/^[[:space:]]*$/d' "$manifest")

"$project_dir/scripts/verify-ui-screenshots.sh" "$temp_dir" default
: > "$temp_dir/unexpected-default.png"
if "$project_dir/scripts/verify-ui-screenshots.sh" "$temp_dir" default >/dev/null 2>&1; then
    echo "截图清单校验器没有拒绝额外截图。" >&2
    exit 1
fi
