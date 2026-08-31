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

        assertEquals("项目与 Git", menu.getItem(4).getTitle().toString());
        assertFalse(menu.getItem(4).isEnabled());
        assertEquals(TerminalProjectToolsMenu.TOOL_GIT_STATUS, menu.getItem(5).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_GIT_DIFF, menu.getItem(6).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_PROJECT_CHECK, menu.getItem(7).getItemId());

        assertEquals("AI 安全操作", menu.getItem(8).getTitle().toString());
        assertFalse(menu.getItem(8).isEnabled());
        assertEquals(TerminalProjectToolsMenu.TOOL_AI_CONFIRM, menu.getItem(9).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_AI_REJECT, menu.getItem(10).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_INTERRUPT, menu.getItem(11).getItemId());

        assertEquals("键区切换", menu.getItem(12).getTitle().toString());
        assertFalse(menu.getItem(12).isEnabled());
        assertEquals(TerminalProjectToolsMenu.TOOL_KEYS_SHELL, menu.getItem(13).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_KEYS_AI, menu.getItem(14).getItemId());
        assertEquals(TerminalProjectToolsMenu.TOOL_KEYS_VIM, menu.getItem(15).getItemId());
    }
}
