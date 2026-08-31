#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

dependabot_file="$project_dir/.github/dependabot.yml"
if ! grep -Fq "target-branch: dev" "$dependabot_file"; then
    echo "Dependabot 依赖更新必须先进入 dev，禁止直接向 master 创建研发 PR。" >&2
    exit 1
fi
if ! grep -Fq 'prefix: "ci"' "$dependabot_file"; then
    echo "Dependabot GitHub Actions 更新提交必须使用 ci 前缀，保持提交语义清晰。" >&2
    exit 1
fi

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
if ! grep -Fq "verifyReleaseUpgrade" "$project_dir/.github/workflows/ui-emulator.yml"; then
    echo "UI workflow_dispatch 必须默认跳过 Release 覆盖升级，避免发布 PR 阶段依赖尚未创建的标签 Release。" >&2
    exit 1
fi
if ! grep -Fq "github.event_name == 'workflow_dispatch' && inputs.verifyReleaseUpgrade" "$project_dir/.github/workflows/ui-emulator.yml"; then
    echo "Release 覆盖升级只能在显式开启 verifyReleaseUpgrade 时运行。" >&2
    exit 1
fi
if ! grep -Fq 'gate_abi="x86_64"' "$project_dir/.github/workflows/release.yml"; then
    echo "Release 覆盖升级必须使用与 GitHub 模拟器匹配的 x86_64 验收 APK，不能把 arm64 发布 APK 装到 x86_64 模拟器。" >&2
    exit 1
fi
if ! grep -Fq "public_candidate_apk" "$project_dir/.github/workflows/release.yml"; then
    echo "Release workflow 必须继续验证公开发布的 arm64 APK，不能用 x86_64 验收产物替代正式资产。" >&2
    exit 1
fi
if ! grep -Fq "build-release-abi-apk.sh" "$project_dir/.github/workflows/release.yml"; then
    echo "Release workflow 必须从稳定标签构建同签名 ABI 验收基线 APK。" >&2
    exit 1
fi

echo "研发 PR 与发布 PR 的 workflow 触发策略校验通过。"
