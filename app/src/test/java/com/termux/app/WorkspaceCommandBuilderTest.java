package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class WorkspaceCommandBuilderTest {
    private static final String OWNER = "11111111-2222-3333-4444-555555555555";

    @Test
    public void shellQuoteEscapesSingleQuotes() {
        assertEquals("'/srv/team'\\''s app'", WorkspaceCommandBuilder.shellQuote("/srv/team's app"));
    }

    @Test
    public void regularWorkspaceDoesNotTouchTmuxByDefault() {
        String command = WorkspaceCommandBuilder.buildSshCommand(
            "developer@example.com", 2222, "/srv/my project", null,
            WorkspaceCommandBuilder.POLICY_SSH_ONLY, "");

        assertTrue(command.startsWith("ssh -t -o ControlMaster=auto -o ControlPersist=600"));
        assertTrue(command.contains("termuxpro-%C"));
        assertTrue(command.contains("-p 2222 -- 'developer@example.com'"));
        assertTrue(command.contains("'\\''/srv/my project'\\''"));
        assertTrue(!command.contains("tmux"));
        assertTrue(command.contains("exec ${SHELL:-sh}"));
        assertTrue(command.contains("SSH authenticated; workspace ready"));
        assertTrue(command.contains("workspace path is unavailable"));
    }

    @Test
    public void claudeStartsNewContextWithoutGlobalContinue() {
        String command = WorkspaceCommandBuilder.buildSshCommand(
            "dev@example.com", 22, "/repo; touch /tmp/unsafe", "claude",
            WorkspaceCommandBuilder.POLICY_SSH_ONLY, "");

        assertTrue(command.contains("exec claude"));
        assertTrue(!command.contains("--continue"));
        assertTrue(!command.contains("tmux"));
        assertTrue(command.contains("'\\''/repo; touch /tmp/unsafe'\\''"));
    }

    @Test
    public void codexStartsNewContextWithoutResumeLast() {
        String command = WorkspaceCommandBuilder.buildSshCommand(
            "dev@example.com", 22, "~/repo", "codex",
            WorkspaceCommandBuilder.POLICY_SSH_ONLY, "");

        assertTrue(command.contains("exec codex"));
        assertTrue(!command.contains("resume --last"));
        assertTrue(command.contains("\"$HOME\"/"));
        assertTrue(command.contains("'\\''repo'\\''"));
    }

    @Test
    public void listPolicyShowsSessionsWithoutAttaching() {
        String command = WorkspaceCommandBuilder.buildSshCommand(
            "dev@example.com", 22, "~/repo", null,
            WorkspaceCommandBuilder.POLICY_LIST_SESSIONS, "");

        assertTrue(command.contains("tmux list-sessions"));
        assertTrue(command.contains("not attached"));
        assertTrue(!command.contains("attach-session"));
        assertTrue(!command.contains("new-session"));
    }

    @Test
    public void attachPolicyUsesOnlyExactConfiguredSessionAndNeverCreatesIt() {
        String command = WorkspaceCommandBuilder.buildSshCommand(
            "dev@example.com", 22, "~/repo", "claude",
            WorkspaceCommandBuilder.POLICY_ATTACH_SESSION, "team.project");

        assertTrue(command.contains("tmux has-session -t"));
        assertTrue(command.contains("tmux attach-session -t"));
        assertTrue(command.contains("team.project"));
        assertTrue(!command.contains("new-session"));
        assertTrue(!command.contains("--continue"));
    }

    @Test
    public void createPolicyCreatesOnlyConfiguredSessionWithFreshAiContext() {
        String command = WorkspaceCommandBuilder.buildSshCommand(
            "dev@example.com", 22, "~/repo", "claude",
            WorkspaceCommandBuilder.POLICY_CREATE_OR_ATTACH, "termuxpro-mine", OWNER);

        assertTrue(command.contains("tmux list-sessions -F"));
        assertTrue(command.contains("tmux new-session -d -s"));
        assertTrue(command.contains("termuxpro-mine"));
        assertTrue(command.contains(WorkspaceCommandBuilder.TMUX_OWNER_OPTION));
        assertTrue(command.contains(OWNER));
        assertTrue(command.contains("session ownership changed"));
        assertTrue(command.contains("exec claude"));
        assertTrue(!command.contains("--continue"));
        assertTrue(!command.contains("new-session -A"));
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
            "dev@example.com", 22, "~/repo", safeTask, OWNER);

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
        String first = WorkspaceCommandBuilder.taskSessionName("~/repo", "pnpm run 'dev'", OWNER);
        String same = WorkspaceCommandBuilder.taskSessionName("~/repo", "pnpm run 'dev'", OWNER);
        String other = WorkspaceCommandBuilder.taskSessionName("~/repo", "pnpm run 'test'", OWNER);
        String otherProject = WorkspaceCommandBuilder.taskSessionName("~/other", "pnpm run 'dev'", OWNER);
        String javaCollision = WorkspaceCommandBuilder.taskSessionName("~/repo", "pnpm run 'BB'", OWNER);
        String javaCollisionPeer = WorkspaceCommandBuilder.taskSessionName("~/repo", "pnpm run 'Aa'", OWNER);

        assertEquals(first, same);
        assertTrue(first.startsWith("mobile-task-"));
        assertTrue(!first.equals(other));
        assertTrue(!first.equals(otherProject));
        assertTrue(!javaCollision.equals(javaCollisionPeer));
        assertTrue(WorkspaceCommandBuilder.buildListTaskSessionsRemoteCommand(OWNER).contains("mobile-task-*"));
        String stop = WorkspaceCommandBuilder.buildStopTaskSessionRemoteCommand(
            first, OWNER, "dev@example.com", 22, "~/repo");
        assertTrue(stop.contains(WorkspaceCommandBuilder.TMUX_OWNER_OPTION));
        assertTrue(stop.contains(OWNER));
        assertTrue(stop.contains("#{pid}:#{session_id}:#{session_created}:#{session_name}"));
        assertTrue(stop.contains("if-shell -F -t \"$sid\""));
        assertTrue(stop.contains("kill-session"));
    }

    @Test
    public void tmuxSessionCenterListsAllSessionsWithoutAttachingOrReadingPanes() {
        String command = WorkspaceCommandBuilder.buildListTmuxSessionsRemoteCommand();

        assertTrue(command.contains("tmux list-sessions"));
        assertTrue(command.contains("#{session_name}"));
        assertTrue(command.contains("#{session_windows}"));
        assertTrue(command.contains("#{session_attached}"));
        assertTrue(command.contains(TmuxSessionParser.MISSING_MARKER));
        assertTrue(!command.contains("mobile-task-*"));
        assertTrue(!command.contains("attach-session"));
        assertTrue(!command.contains("capture-pane"));
    }

    @Test
    public void tmuxSessionActionsQuoteTheExactSelectedSession() {
        String session = "termuxpro-user'; touch /tmp/unsafe; #";
        String attach = WorkspaceCommandBuilder.buildAttachTaskSessionCommand(
            "dev@example.com", 22, session, null, "~/repo");
        String stop = WorkspaceCommandBuilder.buildStopTaskSessionRemoteCommand(
            session, OWNER, "dev@example.com", 22, "~/repo");

        assertTrue(attach.contains("termuxpro-user"));
        assertTrue(attach.contains("'\\''; touch /tmp/unsafe; #'"));
        assertTrue(stop.contains("'termuxpro-user'\\''; touch /tmp/unsafe; #'"));
        assertTrue(stop.contains(OWNER));
        assertTrue(stop.contains("#{pid}:#{session_id}:#{session_created}:#{session_name}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void managedPolicyRejectsMissingOwnerToken() {
        WorkspaceCommandBuilder.buildSshCommand("dev@example.com", 22, "~/repo", null,
            WorkspaceCommandBuilder.POLICY_CREATE_OR_ATTACH, "termuxpro-work");
    }

    @Test
    public void workspaceFingerprintChangesWithHostOrPath() {
        String first = WorkspaceCommandBuilder.workspaceFingerprint("dev@example.com", 22, "~/repo");
        assertTrue(!first.equals(WorkspaceCommandBuilder.workspaceFingerprint("dev@example.com", 22, "~/other")));
        assertTrue(!first.equals(WorkspaceCommandBuilder.workspaceFingerprint("other@example.com", 22, "~/repo")));
        assertTrue(!first.equals(WorkspaceCommandBuilder.workspaceFingerprint("dev@example.com", 2222, "~/repo")));
    }

    @Test
    public void realTmuxRejectsWrongMarkerAndStopsMatchingSession() throws Exception {
        Assume.assumeTrue(new File("/usr/bin/tmux").canExecute());
        File tmuxRoot = Files.createTempDirectory("termuxpro-tmux-test").toFile();
        Map<String, String> environment = new HashMap<>();
        environment.put("TMUX_TMPDIR", tmuxRoot.getAbsolutePath());
        String fingerprint = WorkspaceCommandBuilder.workspaceFingerprint("fixture-host", 22, "~/repo");
        try {
            int setup = runShell("tmux new-session -d -s termuxpro-guard 'sleep 30'; "
                + "tmux set-option -t termuxpro-guard " + WorkspaceCommandBuilder.TMUX_OWNER_OPTION
                + " wrong-owner; tmux set-option -t termuxpro-guard "
                + WorkspaceCommandBuilder.TMUX_WORKSPACE_OPTION + " '" + fingerprint + "'", environment);
            // 某些 Codex 沙箱禁止 Unix socket；CI 的 SSH fixture 仍会强制执行真实 tmux 门禁。
            Assume.assumeTrue("当前执行环境不允许创建隔离 tmux socket", setup == 0);

            String stop = WorkspaceCommandBuilder.buildStopTaskSessionRemoteCommand(
                "termuxpro-guard", OWNER, "fixture-host", 22, "~/repo");
            assertEquals(73, runShell(stop, environment));
            assertEquals(0, runShell("tmux has-session -t termuxpro-guard", environment));

            assertEquals(0, runShell("tmux set-option -t termuxpro-guard "
                + WorkspaceCommandBuilder.TMUX_OWNER_OPTION + " '" + OWNER + "'", environment));
            assertEquals(0, runShell(stop, environment));
            assertTrue(runShell("tmux has-session -t termuxpro-guard", environment) != 0);

            String create = WorkspaceCommandBuilder.buildCreateManagedTmuxSessionCommand(
                "termuxpro-created", null, OWNER, fingerprint);
            // attach 会等待终端；超时只终止客户端，之前同一服务端命令队列写入的标记必须已经生效。
            runShell("timeout 2 /bin/sh -c " + WorkspaceCommandBuilder.shellQuote(create), environment);
            assertEquals(0, runShell("test \"$(tmux show-options -v -t termuxpro-created "
                + WorkspaceCommandBuilder.TMUX_OWNER_OPTION + ")\" = '" + OWNER + "'", environment));
            assertEquals(0, runShell("test \"$(tmux show-options -v -t termuxpro-created "
                + WorkspaceCommandBuilder.TMUX_WORKSPACE_OPTION + ")\" = '" + fingerprint + "'", environment));
        } finally {
            runShell("tmux kill-server 2>/dev/null || true", environment);
        }
    }

    /**
     * 在隔离环境中执行 tmux 测试命令，避免继承当前真实 tmux 服务器套接字。
     *
     * @param command Shell 命令，类型为 String，无默认值。
     * @param environment 子进程环境变量，类型为 Map&lt;String, String&gt;，无默认值。
     * @return 子进程退出码，类型为 int，无默认值。
     */
    private static int runShell(String command, Map<String, String> environment)
        throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", command);
        Map<String, String> processEnvironment = builder.environment();
        // TMUX 比 TMUX_TMPDIR 优先级更高；不清除会让隔离测试误连并终止父级 tmux 服务器。
        processEnvironment.remove("TMUX");
        processEnvironment.putAll(environment);
        Process process = builder.redirectErrorStream(true).start();
        while (process.getInputStream().read() != -1) {
            // 消费有限测试输出，避免子进程因管道写满而阻塞。
        }
        return process.waitFor();
    }
}
