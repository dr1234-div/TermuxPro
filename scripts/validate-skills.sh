#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
venv_dir="$project_dir/.tooling/skill-validator-venv"
requirements="$project_dir/requirements-tools.txt"

if ! command -v python3 >/dev/null 2>&1; then
    echo "Skill 校验需要 Python 3。" >&2
    exit 2
fi

if [[ ! -x "$venv_dir/bin/python" ]]; then
    python3 -m venv "$venv_dir"
fi

if ! "$venv_dir/bin/python" -c 'import yaml; assert yaml.__version__ == "6.0.3"' >/dev/null 2>&1; then
    "$venv_dir/bin/python" -m pip install --disable-pip-version-check -r "$requirements"
fi

exec "$venv_dir/bin/python" "$project_dir/scripts/validate-skills.py"
