package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "zh-rCN")
public class TerminalProjectToolsMenuTest {
    @Test
    public void toolboxUsesDeveloperTaskGroupsBeforeRawActions() {
        PopupMenu popup = new PopupMenu(RuntimeEnvironment.getApplication(),
            new View(RuntimeEnvironment.getApplication()));
        Menu menu = popup.getMenu();

        TerminalProjectToolsMenu.populate(RuntimeEnvironment.getApplication(), menu);

        assertEquals("当前上下文", menu.getItem(0).getTitle().toString());
        assertFalse(menu.getItem(0).isEnabled());
        assertEquals(TerminalProjectToolsMenu.TOOL_TMUX_SESSIONS, menu.getItem(1).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_CUSTOM_COMMANDS, menu.getItem(2).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_SEARCH_OUTPUT, menu.getItem(3).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_TOUCH_SCROLL_MODE, menu.getItem(4).getItemId());
        assertEquals("当前：终端历史；点此切换到 AI/TUI 面板", menu.getItem(4).getTitle().toString());

        assertEquals("项目与 Git", menu.getItem(5).getTitle().toString());
        assertFalse(menu.getItem(5).isEnabled());
        assertEquals(TerminalProjectToolsMenu.TOOL_GIT_STATUS, menu.getItem(6).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_GIT_DIFF, menu.getItem(7).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_REMOTE_FILES, menu.getItem(8).getItemId());
        assertEquals("远端文件", menu.getItem(8).getTitle().toString());
        assertEquals(TerminalProjectToolsMenu.TOOL_PROJECT_CHECK, menu.getItem(9).getItemId());
        assertEquals("项目任务", menu.getItem(9).getTitle().toString());
        assertEquals(TerminalProjectToolsMenu.TOOL_START_WEB_PREVIEW,
            menu.getItem(10).getItemId());
        assertEquals("启动 Web 隧道", menu.getItem(10).getTitle().toString());
        assertEquals(TerminalProjectToolsMenu.TOOL_OPEN_WEB_PREVIEW,
            menu.getItem(11).getItemId());
        assertEquals("打开 Web 预览", menu.getItem(11).getTitle().toString());

        assertEquals("连接与安全", menu.getItem(12).getTitle().toString());
        assertFalse(menu.getItem(12).isEnabled());
        assertEquals(TerminalProjectToolsMenu.TOOL_CONNECTION_DIAGNOSTIC,
            menu.getItem(13).getItemId());
        assertEquals("环境诊断", menu.getItem(13).getTitle().toString());
        assertEquals(TerminalProjectToolsMenu.TOOL_SSH_KEYS, menu.getItem(14).getItemId());
        assertEquals("SSH 密钥", menu.getItem(14).getTitle().toString());

        assertEquals("AI 工作", menu.getItem(15).getTitle().toString());
        assertFalse(menu.getItem(15).isEnabled());
        assertEquals(TerminalProjectToolsMenu.TOOL_AI_CLAUDE, menu.getItem(16).getItemId());
        assertEquals("启动 Claude Code", menu.getItem(16).getTitle().toString());
        assertEquals(TerminalProjectToolsMenu.TOOL_AI_CODEX, menu.getItem(17).getItemId());
        assertEquals("启动 Codex CLI", menu.getItem(17).getTitle().toString());
        assertEquals(TerminalProjectToolsMenu.TOOL_AI_CONFIRM, menu.getItem(18).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_AI_REJECT, menu.getItem(19).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_INTERRUPT, menu.getItem(20).getItemId());

        assertEquals("键区切换", menu.getItem(21).getTitle().toString());
        assertFalse(menu.getItem(21).isEnabled());
        assertEquals(TerminalProjectToolsMenu.TOOL_KEYS_SHELL, menu.getItem(22).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_KEYS_AI, menu.getItem(23).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_KEYS_VIM, menu.getItem(24).getItemId());
    }

    @Test
    public void toolboxShowsCurrentTouchScrollModeAction() {
        PopupMenu popup = new PopupMenu(RuntimeEnvironment.getApplication(),
            new View(RuntimeEnvironment.getApplication()));

        TerminalProjectToolsMenu.populate(RuntimeEnvironment.getApplication(), popup.getMenu(), true);

        assertEquals(TerminalProjectToolsMenu.TOOL_TOUCH_SCROLL_MODE,
            popup.getMenu().getItem(4).getItemId());
        assertEquals("当前：AI/TUI 面板；点此切换到终端历史",
            popup.getMenu().getItem(4).getTitle().toString());
    }

    @Test
    public void toolsButtonShowsCurrentTouchScrollMode() {
        assertEquals(com.termux.R.string.workspace_tools_scrollback_action,
            TerminalProjectToolsMenu.toolsButtonLabel(false));
        assertEquals(com.termux.R.string.workspace_tools_tui_scroll_action,
            TerminalProjectToolsMenu.toolsButtonLabel(true));
    }
}
