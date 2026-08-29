package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WorkspaceCommandBuilderTest {

    @Test
    public void shellQuoteEscapesSingleQuotes() {
        assertEquals("'/srv/team'\\''s app'", WorkspaceCommandBuilder.shellQuote("/srv/team's app"));
    }

    @Test
    public void regularWorkspaceRestoresStableTmuxSession() {
        String command = WorkspaceCommandBuilder.buildSshCommand(
            "developer@example.com", 2222, "/srv/my project", null);

        assertTrue(command.startsWith("ssh -t -o ControlMaster=auto -o ControlPersist=600"));
        assertTrue(command.contains("termuxpro-%C"));
        assertTrue(command.contains("-p 2222 -- 'developer@example.com'"));
        assertTrue(command.contains("'\\''/srv/my project'\\''"));
        assertTrue(command.contains("tmux new-session -A -s"));
        assertTrue(command.contains("exec ${SHELL:-sh}"));
    }

    @Test
    public void claudeWorkspaceKeepsFallbackInsideRemoteCommand() {
        String command = WorkspaceCommandBuilder.buildSshCommand(
            "dev@example.com", 22, "/repo; touch /tmp/unsafe", "claude");

        assertTrue(command.contains("mobile-claude"));
        assertTrue(command.contains("claude --continue || exec claude"));
        assertTrue(command.contains("exec claude"));
        assertTrue(command.contains("'\\''/repo; touch /tmp/unsafe'\\''"));
    }

    @Test
    public void codexWorkspaceUsesResumeLast() {
        String command = WorkspaceCommandBuilder.buildSshCommand(
            "dev@example.com", 22, "~/repo", "codex");

        assertTrue(command.contains("mobile-codex"));
        assertTrue(command.contains("codex resume --last || exec codex"));
        assertTrue(command.contains("exec codex"));
        assertTrue(command.contains("\"$HOME\"/"));
        assertTrue(command.contains("'\\''repo'\\''"));
    }

    @Test
    public void portForwardBindsOnlyToLoopback() {
        String command = WorkspaceCommandBuilder.buildPortForwardCommand(
            "dev@example.com", 2222, 15173, 5173);

        assertTrue(command.startsWith("ssh -N -T -o ExitOnForwardFailure=yes"));
        assertTrue(command.contains("-p 2222 -L '127.0.0.1:15173:127.0.0.1:5173'"));
        assertTrue(command.endsWith("-- 'dev@example.com'"));
    }

    @Test
    public void gitDiffCommandQuotesWorkspacePath() {
        String command = WorkspaceCommandBuilder.buildGitDiffRemoteCommand("/srv/team's app");

        assertTrue(command.startsWith("cd -- '/srv/team'\\''s app'"));
        assertTrue(command.contains("git status --short --branch"));
        assertTrue(command.contains("git diff --no-ext-diff --no-color"));
        assertTrue(command.contains("git diff --cached --no-ext-diff --no-color"));
    }

    @Test
    public void fileCommandsQuoteProjectAndRelativePaths() {
        String list = WorkspaceCommandBuilder.buildListFilesRemoteCommand("~/team app", "./src dir");
        String read = WorkspaceCommandBuilder.buildReadFileRemoteCommand("~/team app", "./a'; rm -rf x");

        assertTrue(list.contains("\"$HOME\"/'team app'"));
        assertTrue(list.contains("'./src dir'"));
        assertTrue(list.contains("%y\\0%f\\0"));
        assertTrue(read.contains("'./a'\\''; rm -rf x'"));
        assertTrue(read.contains("grep -Iq"));
    }

    @Test
    public void metadataReadDoesNotExecuteProjectCode() {
        String command = WorkspaceCommandBuilder.buildProjectMetadataCommand("~/repo");

        assertTrue(command.contains("head -c 500000 -- package.json"));
        assertTrue(command.contains("MAVEN_WRAPPER"));
        assertTrue(command.contains("GRADLE_WRAPPER"));
        assertTrue(!command.contains("npm install"));
    }

    @Test
    public void taskCommandKeepsScriptNameQuotedInsideRemoteSession() {
        String safeTask = "pnpm run " + WorkspaceCommandBuilder.shellQuote("test; touch /tmp/nope");
        String command = WorkspaceCommandBuilder.buildSshTaskCommand(
            "dev@example.com", 22, "~/repo", safeTask);

        assertTrue(command.contains("mobile-task-"));
        assertTrue(command.contains("test; touch /tmp/nope"));
        assertTrue(command.contains("ControlMaster=auto"));
    }

    @Test
    public void diagnosticCommandIsReadOnlyAndQuotesProjectPath() {
        String command = WorkspaceCommandBuilder.buildConnectionDiagnosticCommand("~/team's repo");

        assertTrue(command.contains("\"$HOME\"/'team'\\''s repo'"));
        assertTrue(command.contains("command -v claude"));
        assertTrue(command.contains("command -v codex"));
        assertTrue(!command.contains("install"));
    }

    @Test
    public void sshKeyCommandsKeepCredentialsInteractiveAndTargetQuoted() {
        String generate = WorkspaceCommandBuilder.buildGenerateSshKeyCommand();
        String copy = WorkspaceCommandBuilder.buildCopySshKeyCommand("dev'alias", 2222);

        assertTrue(generate.contains("ssh-keygen -t ed25519 -a 64"));
        assertTrue(!generate.contains("-N"));
        assertTrue(copy.contains("-p 2222 -- 'dev'\\''alias'"));
    }

    @Test
    public void projectTasksUseStableIndependentSessions() {
        String first = WorkspaceCommandBuilder.taskSessionName("pnpm run 'dev'");
        String same = WorkspaceCommandBuilder.taskSessionName("pnpm run 'dev'");
        String other = WorkspaceCommandBuilder.taskSessionName("pnpm run 'test'");

        assertEquals(first, same);
        assertTrue(first.startsWith("mobile-task-"));
        assertTrue(!first.equals(other));
        assertTrue(WorkspaceCommandBuilder.buildListTaskSessionsRemoteCommand().contains("mobile-task-*"));
        assertTrue(WorkspaceCommandBuilder.buildStopTaskSessionRemoteCommand(first).contains("'" + first + "'"));
    }
}
