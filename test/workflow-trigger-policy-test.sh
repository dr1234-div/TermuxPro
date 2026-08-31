#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

for workflow in ci ui-emulator; do
    file="$project_dir/.github/workflows/$workflow.yml"
    if ! grep -Fq "branches: [master]" "$file"; then
        echo "$workflow 的 pull_request 门禁只能绑定 master，dev 研发 PR 使用同 SHA push 门禁。" >&2
        exit 1
    fi
done

if ! grep -Fq "branches: [dev, master, 'dev_*', 'hotfix_*']" "$project_dir/.github/workflows/ci.yml"; then
    echo "CI push 门禁必须覆盖 dev、master、dev_* 和 hotfix_*。" >&2
    exit 1
fi
if ! grep -Fq "branches: ['dev_*', 'hotfix_*']" "$project_dir/.github/workflows/ui-emulator.yml"; then
    echo "UI push 门禁必须覆盖 dev_* 和 hotfix_*。" >&2
    exit 1
fi
if ! grep -Fq "'terminal-view/src/main/**'" "$project_dir/.github/workflows/ui-emulator.yml"; then
    echo "UI pull_request 门禁必须覆盖 terminal-view 触摸、渲染和输入层变更。" >&2
    exit 1
fi

echo "研发 PR 与发布 PR 的 workflow 触发策略校验通过。"
