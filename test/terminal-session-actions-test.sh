#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
controller="$project_dir/app/src/main/java/com/termux/app/terminal/TermuxSessionsListViewController.java"
activity="$project_dir/app/src/main/java/com/termux/app/TermuxActivity.java"
layout="$project_dir/app/src/main/res/layout/activity_termux.xml"

require_contains() {
    local file="$1"
    local pattern="$2"
    local message="$3"
    if ! grep -Fq "$pattern" "$file"; then
        echo "$message" >&2
        exit 1
    fi
}

require_contains "$controller" "showSessionActions(view, selectedSession)" \
    "终端会话列表长按必须打开显式操作菜单，不能直接重命名。"
require_contains "$controller" "R.string.action_switch_session" \
    "终端会话操作菜单必须包含切换入口。"
require_contains "$controller" "R.string.action_rename_session" \
    "终端会话操作菜单必须包含重命名入口。"
require_contains "$controller" "R.string.action_close_session" \
    "终端会话操作菜单必须包含关闭入口。"
require_contains "$controller" "mActivity.confirmCloseSession(terminalSession)" \
    "行级关闭必须进入 TermuxActivity 的确认流程，不能直接杀会话。"
require_contains "$activity" "setTitle(R.string.terminal_session_close_title)" \
    "关闭本地终端会话必须弹出确认标题。"
require_contains "$activity" "R.string.terminal_session_close_message" \
    "关闭本地终端会话必须说明只影响本地终端会话。"
require_contains "$activity" "removeFinishedSession(session)" \
    "确认后只能关闭指定的本地 Termux session。"
require_contains "$layout" "android:id=\"@+id/close_session_button\"" \
    "终端侧栏底部必须保留当前会话关闭入口。"

echo "终端本地会话操作菜单静态校验通过。"
