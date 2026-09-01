package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "zh-rCN")
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

    @Test
    public void guidanceSeparatesSharedClaudeFromUserScopedCodex() {
        assertEquals(com.termux.R.string.ai_session_claude_guidance,
            AiCliLaunchCommand.guidanceMessage(AiCliLaunchCommand.Tool.CLAUDE));
        assertEquals(com.termux.R.string.ai_session_codex_guidance,
            AiCliLaunchCommand.guidanceMessage(AiCliLaunchCommand.Tool.CODEX));
    }

    @Test
    public void launchMessageShowsTargetForTerminalAndWorkspaceEntrypoints() {
        String message = AiCliLaunchMessage.forWorkspaceTarget(RuntimeEnvironment.getApplication(),
            AiCliLaunchCommand.Tool.CODEX,
            new WorkspaceTarget("workspace-a", "远程开发", "hdr@192.168.1.153", 22,
                "~/termux-pro"));

        assertTrue(message.contains("当前目标"));
        assertTrue(message.contains("hdr@192.168.1.153:22 · ~/termux-pro"));
        assertTrue(message.contains("Codex CLI 通常按用户隔离"));
    }

    @Test
    public void launchMessageFailsClosedWhenTerminalHasNoConfiguredWorkspace() {
        String message = AiCliLaunchMessage.forWorkspaceTarget(RuntimeEnvironment.getApplication(),
            AiCliLaunchCommand.Tool.CLAUDE, null);

        assertTrue(message.contains("当前目标未完整配置"));
        assertTrue(message.contains("Claude Code 常见于共享远程账号"));
    }

    @Test
    public void terminalEntrypointWarnsCommandGoesToCurrentVisibleSession() {
        String message = AiCliLaunchMessage.forTerminalTarget(RuntimeEnvironment.getApplication(),
            AiCliLaunchCommand.Tool.CLAUDE,
            new WorkspaceTarget("workspace-a", "远程开发", "hdr@192.168.1.153", 22,
                "~/termux-pro"));

        assertTrue(message.contains("当前目标"));
        assertTrue(message.contains("命令会发送到当前正在显示的终端会话"));
        assertTrue(message.contains("正确的 SSH、tmux 和项目目录"));
    }

    @Test
    public void actionLabelsPreviewExactCliCommand() {
        assertTrue(AiCliLaunchMessage.actionLabel(RuntimeEnvironment.getApplication(),
            AiCliLaunchCommand.Tool.CLAUDE, AiCliLaunchCommand.Mode.NEW_SESSION)
            .contains("将执行：claude"));
        assertTrue(AiCliLaunchMessage.actionLabel(RuntimeEnvironment.getApplication(),
            AiCliLaunchCommand.Tool.CODEX, AiCliLaunchCommand.Mode.PICK_HISTORY)
            .contains("将执行：codex resume"));
    }
}
