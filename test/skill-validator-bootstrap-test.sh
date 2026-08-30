#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

# -S 禁用系统 site-packages，稳定复现当前 Python 没有 PyYAML。
python3 -S scripts/validate-skills.py >/dev/null
