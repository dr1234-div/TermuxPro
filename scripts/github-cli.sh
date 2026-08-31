#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
project_config="$project_dir/.tooling/gh-config"

if [[ -f "$project_config/hosts.yml" ]]; then
    export GH_CONFIG_DIR="$project_config"
fi
export GH_REPO="${GH_REPO:-heydarey/TermuxPro}"

if [[ "${1:-}" == "repo" && "${2:-}" == "view" ]]; then
    if [[ -z "${3:-}" || "${3:-}" == -* ]]; then
        set -- repo view "$GH_REPO" "${@:3}"
    fi
fi

if command -v gh >/dev/null 2>&1; then
    exec gh "$@"
fi

project_gh="$project_dir/.tooling/gh/bin/gh"
if [[ -x "$project_gh" ]]; then
    exec "$project_gh" "$@"
fi

echo "未找到 GitHub CLI。请将 gh 加入 PATH，或安装到 .tooling/gh/bin/gh。" >&2
exit 127
