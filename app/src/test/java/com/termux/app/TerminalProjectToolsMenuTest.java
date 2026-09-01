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
        assertEquals(TerminalProjectToolsMenu.TOOL_PROJECT_CHECK, menu.getItem(8).getItemId());
        assertEquals("项目任务", menu.getItem(8).getTitle().toString());

        assertEquals("AI 安全操作", menu.getItem(9).getTitle().toString());
        assertFalse(menu.getItem(9).isEnabled());
        assertEquals(TerminalProjectToolsMenu.TOOL_AI_CONFIRM, menu.getItem(10).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_AI_REJECT, menu.getItem(11).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_INTERRUPT, menu.getItem(12).getItemId());

        assertEquals("键区切换", menu.getItem(13).getTitle().toString());
        assertFalse(menu.getItem(13).isEnabled());
        assertEquals(TerminalProjectToolsMenu.TOOL_KEYS_SHELL, menu.getItem(14).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_KEYS_AI, menu.getItem(15).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_KEYS_VIM, menu.getItem(16).getItemId());
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
