#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
script="$project_dir/scripts/pre-push-smoke.sh"

if [[ ! -x "$script" ]]; then
    echo "scripts/pre-push-smoke.sh 必须可执行。" >&2
    exit 1
fi

require_contains() {
    local output="$1"
    local expected="$2"
    local message="$3"
    if ! grep -Fq -- "$expected" <<<"$output"; then
        echo "$message" >&2
        echo "缺少：$expected" >&2
        exit 1
    fi
}

source_text="$(sed -n '1,220p' "$script")"

require_contains "$source_text" "collect_changed_files" \
    "推送前冒烟必须同时覆盖已提交、已暂存和未暂存变更。"
require_contains "$source_text" "git diff --name-only \"\$base_ref\"...HEAD" \
    "推送前冒烟必须比较功能分支相对 origin/dev 的提交。"
require_contains "$source_text" "git diff --name-only --cached" \
    "推送前冒烟必须覆盖已暂存但未提交的文件。"
require_contains "$source_text" "git diff --name-only" \
    "推送前冒烟必须覆盖未暂存的文件。"
require_contains "$source_text" "git ls-files --others --exclude-standard" \
    "推送前冒烟必须覆盖未跟踪的新文件。"
require_contains "$source_text" "WorkspaceActivitySmokeTest" \
    "工作台、资源或 AI 弹窗变更必须触发 WorkspaceActivitySmokeTest。"
require_contains "$source_text" "CustomLayoutsSmokeTest" \
    "资源/UI 变更必须触发布局 smoke 测试。"
require_contains "$source_text" "TermuxTerminalSessionActivityClientTest" \
    "终端反馈或触摸相关变更必须触发终端会话反馈测试。"
require_contains "$source_text" "GitDiffActivityTest" \
    "Git 工作台变更必须触发 Git UI 测试。"
require_contains "$source_text" "TaskSessionsActivityTest" \
    "tmux 会话中心变更必须触发会话中心测试。"
require_contains "$source_text" "ProjectTaskDetectorTest" \
    "项目任务入口或检测器变更必须触发项目任务检测测试。"
require_contains "$source_text" "scripts/validate-skills.sh" \
    "推送前冒烟必须包含 Skill 校验，避免规则漂移。"
require_contains "$source_text" "scripts/resource-guard.sh normal" \
    "真实 Gradle 冒烟前必须执行资源守卫。"
require_contains "$source_text" "scripts/resolve-jdk17.sh" \
    "Gradle 冒烟必须使用项目 JDK 解析脚本。"

echo "推送前冒烟映射静态校验通过。"
