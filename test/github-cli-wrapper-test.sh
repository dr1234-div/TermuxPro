#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
wrapper="$project_dir/scripts/github-cli.sh"

if ! grep -Fq 'export GH_REPO="${GH_REPO:-heydarey/TermuxPro}"' "$wrapper"; then
    echo "GitHub CLI 封装必须默认绑定 TermuxPro 仓库，避免在 fork/upstream 间查错仓库。" >&2
    exit 1
fi
if ! grep -Fq 'set -- repo view "$GH_REPO"' "$wrapper"; then
    echo "GitHub CLI 封装必须修正 gh repo view 的默认仓库推断。" >&2
    exit 1
fi

echo "GitHub CLI 封装仓库默认值校验通过。"
