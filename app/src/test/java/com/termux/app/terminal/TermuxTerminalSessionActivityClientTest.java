package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TermuxTerminalSessionActivityClientTest {

    @Test
    public void toastTitleAlwaysContainsSessionIndexEvenWithoutNameOrTitle() {
        assertEquals("[1]", TermuxTerminalSessionActivityClient.formatToastTitle(0, null, null));
        assertEquals("[2]", TermuxTerminalSessionActivityClient.formatToastTitle(1, "", ""));
    }

    @Test
    public void toastTitleKeepsNameAndTerminalTitleReadable() {
        assertEquals("[3] ssh-153",
            TermuxTerminalSessionActivityClient.formatToastTitle(2, "ssh-153", null));
        assertEquals("[4] ~/project",
            TermuxTerminalSessionActivityClient.formatToastTitle(3, null, "~/project"));
        assertEquals("[5] ssh-153\ncodex",
            TermuxTerminalSessionActivityClient.formatToastTitle(4, "ssh-153", "codex"));
    }

    @Test
    public void invalidSessionIndexDoesNotCreateFeedbackText() {
        assertNull(TermuxTerminalSessionActivityClient.formatToastTitle(-1, "ssh-153", "codex"));
    }
}
