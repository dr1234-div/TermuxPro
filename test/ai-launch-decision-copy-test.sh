#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
zh="$project_dir/app/src/main/res/values-zh-rCN/strings.xml"
en="$project_dir/app/src/main/res/values/strings.xml"
dialog="$project_dir/app/src/main/java/com/termux/app/AiSessionDialog.java"
command="$project_dir/app/src/main/java/com/termux/app/AiCliLaunchCommand.java"

require_contains() {
    local file="$1"
    local pattern="$2"
    local message="$3"
    if ! grep -Fq "$pattern" "$file"; then
        echo "$message" >&2
        exit 1
    fi
}

require_contains "$zh" "共享 Claude 账号推荐使用" \
    "AI 新建会话文案必须明确共享 Claude 账号下的安全默认值。"
require_contains "$zh" "TermuxPro 不自动进入最近会话" \
    "AI 历史会话文案必须说明不会自动进入最近上下文。"
require_contains "$en" "Recommended for shared Claude accounts" \
    "英文 AI 新建会话文案必须保留共享账号风险提示。"
require_contains "$en" "will not auto-enter the latest session" \
    "英文 AI 历史会话文案必须保留不自动进入最近会话提示。"
require_contains "$dialog" "label.setMaxLines(3)" \
    "AI 会话弹窗选项支持说明文案后必须允许最多三行，避免 200% 字体截断。"
require_contains "$command" "claude --resume" \
    "Claude 历史入口只能打开原生命令选择器，不能改成静默最近恢复。"
require_contains "$command" "codex resume" \
    "Codex 历史入口只能打开原生命令选择器，不能改成静默最近恢复。"

echo "AI CLI 启动决策文案静态校验通过。"
