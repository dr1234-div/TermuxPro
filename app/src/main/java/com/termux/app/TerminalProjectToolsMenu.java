package com.termux.app;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.termux.R;

/** 终端工具箱菜单的信息架构。 */
final class TerminalProjectToolsMenu {
    static final int TOOL_GIT_STATUS = 1;
    static final int TOOL_GIT_DIFF = 2;
    static final int TOOL_PROJECT_CHECK = 3;
    static final int TOOL_TMUX_SESSIONS = 4;
    static final int TOOL_INTERRUPT = 5;
    static final int TOOL_KEYS_SHELL = 6;
    static final int TOOL_KEYS_AI = 7;
    static final int TOOL_KEYS_VIM = 8;
    static final int TOOL_AI_CONFIRM = 9;
    static final int TOOL_AI_REJECT = 10;
    static final int TOOL_SEARCH_OUTPUT = 11;
    static final int TOOL_CUSTOM_COMMANDS = 12;
    static final int TOOL_TOUCH_SCROLL_MODE = 13;
    static final int TOOL_REMOTE_FILES = 14;

    private TerminalProjectToolsMenu() {}

    static void populate(@NonNull Context context, @NonNull Menu menu) {
        populate(context, menu, false);
    }

    static void populate(@NonNull Context context, @NonNull Menu menu, boolean tuiTouchScrollMode) {
        menu.clear();
        addHeader(context, menu, R.string.terminal_tools_section_context);
        menu.add(Menu.NONE, TOOL_TMUX_SESSIONS, Menu.NONE, R.string.workspace_tmux_sessions_action);
        menu.add(Menu.NONE, TOOL_CUSTOM_COMMANDS, Menu.NONE, R.string.terminal_custom_commands_action);
        menu.add(Menu.NONE, TOOL_SEARCH_OUTPUT, Menu.NONE, R.string.terminal_search_action);
        menu.add(Menu.NONE, TOOL_TOUCH_SCROLL_MODE, Menu.NONE, tuiTouchScrollMode ?
            R.string.terminal_touch_scroll_switch_to_scrollback :
            R.string.terminal_touch_scroll_switch_to_tui);

        addHeader(context, menu, R.string.terminal_tools_section_project);
        menu.add(Menu.NONE, TOOL_GIT_STATUS, Menu.NONE, R.string.workspace_git_status_action);
        menu.add(Menu.NONE, TOOL_GIT_DIFF, Menu.NONE, R.string.workspace_git_diff_action);
        menu.add(Menu.NONE, TOOL_REMOTE_FILES, Menu.NONE, R.string.workspace_remote_files_action);
        menu.add(Menu.NONE, TOOL_PROJECT_CHECK, Menu.NONE, R.string.workspace_project_tasks_action);

        addHeader(context, menu, R.string.terminal_tools_section_ai);
        menu.add(Menu.NONE, TOOL_AI_CONFIRM, Menu.NONE, R.string.ai_action_confirm_selection);
        menu.add(Menu.NONE, TOOL_AI_REJECT, Menu.NONE, R.string.ai_action_reject_or_back);
        menu.add(Menu.NONE, TOOL_INTERRUPT, Menu.NONE, R.string.workspace_interrupt_action);

        addHeader(context, menu, R.string.terminal_tools_section_keyboard);
        menu.add(Menu.NONE, TOOL_KEYS_SHELL, Menu.NONE, R.string.workspace_keys_shell_action);
        menu.add(Menu.NONE, TOOL_KEYS_AI, Menu.NONE, R.string.workspace_keys_ai_action);
        menu.add(Menu.NONE, TOOL_KEYS_VIM, Menu.NONE, R.string.workspace_keys_vim_action);
    }

    private static void addHeader(@NonNull Context context, @NonNull Menu menu, int title) {
        MenuItem header = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, context.getString(title));
        header.setEnabled(false);
    }

    static int toolsButtonLabel(boolean tuiTouchScrollMode) {
        return tuiTouchScrollMode ? R.string.workspace_tools_tui_scroll_action :
            R.string.workspace_tools_scrollback_action;
    }
}
