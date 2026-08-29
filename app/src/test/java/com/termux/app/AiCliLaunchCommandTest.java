package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AiCliLaunchCommandTest {

    @Test
    public void newSessionNeverContainsImplicitResumeFlags() {
        for (AiCliLaunchCommand.Tool tool : AiCliLaunchCommand.Tool.values()) {
            String command = AiCliLaunchCommand.command(tool,
                AiCliLaunchCommand.Mode.NEW_SESSION);
            assertFalse(command.contains("continue"));
            assertFalse(command.contains("--last"));
            assertFalse(command.contains("resume"));
        }
    }

    @Test
    public void historyAlwaysOpensAnInteractivePicker() {
        assertEquals("claude --resume", AiCliLaunchCommand.command(
            AiCliLaunchCommand.Tool.CLAUDE, AiCliLaunchCommand.Mode.PICK_HISTORY));
        assertEquals("codex resume", AiCliLaunchCommand.command(
            AiCliLaunchCommand.Tool.CODEX, AiCliLaunchCommand.Mode.PICK_HISTORY));
    }
}
