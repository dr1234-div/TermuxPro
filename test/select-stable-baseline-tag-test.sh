#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
selector="$project_dir/scripts/select-stable-baseline-tag.sh"

actual="$(
    printf '%s\n' v0.1.0 v0.2.0 v0.4.0-rc.1 v0.3.0 |
        "$selector" 0.4.0-rc.1
)"
if [[ "$actual" != "v0.3.0" ]]; then
    echo "候选版应选择低于候选基线的最高稳定版，实际为：$actual" >&2
    exit 1
fi

actual="$(
    printf '%s\n' v0.2.0 v0.4.0 v0.5.0 |
        "$selector" 0.4.0
)"
if [[ "$actual" != "v0.2.0" ]]; then
    echo "稳定候选发布前不得选择自身或更高版本，实际为：$actual" >&2
    exit 1
fi

actual="$(printf '%s\n' v0.4.0-rc.1 latest stable | "$selector" 0.1.0)"
if [[ -n "$actual" ]]; then
    echo "没有更低稳定版时应输出空值，实际为：$actual" >&2
    exit 1
fi

echo "稳定 Release 基线选择测试通过。"
