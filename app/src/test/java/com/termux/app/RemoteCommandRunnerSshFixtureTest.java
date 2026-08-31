package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/** 在 CI 回环 sshd 上验证应用实际使用的 OpenSSH 进程链。 */
public class RemoteCommandRunnerSshFixtureTest {
    private static final String OWNER = "11111111-2222-3333-4444-555555555555";

    private RemoteCommandRunner runner;
    private String target;
    private int port;

    @Before
    public void setUp() {
        target = System.getenv("TERMUXPRO_SSH_FIXTURE_TARGET");
        String portText = System.getenv("TERMUXPRO_SSH_FIXTURE_PORT");
        String sshPath = System.getenv("TERMUXPRO_SSH_FIXTURE_CLIENT");
        String identity = System.getenv("TERMUXPRO_SSH_FIXTURE_IDENTITY");
        String knownHosts = System.getenv("TERMUXPRO_SSH_FIXTURE_KNOWN_HOSTS");
        Assume.assumeTrue(target != null && portText != null && sshPath != null
            && identity != null && knownHosts != null);
        port = Integer.parseInt(portText);
        runner = new RemoteCommandRunner(new File(sshPath), Arrays.asList(
            "-o", "IdentityFile=" + identity,
            "-o", "IdentitiesOnly=yes",
            "-o", "StrictHostKeyChecking=yes",
            "-o", "UserKnownHostsFile=" + knownHosts,
            "-o", "ControlMaster=no"
        ));
        RemoteCommandRunner.Result workspace = runner.run(target, port, "mkdir -p ~/repo", 4096);
        assertEquals(workspace.output, 0, workspace.exitCode);
    }

    @After
    public void cleanUpTmux() {
        if (runner != null) runner.run(target, port,
            "tmux kill-session -t termuxpro-fixture 2>/dev/null || true; "
                + "tmux kill-session -t termuxpro-renamed 2>/dev/null || true; "
                + "tmux kill-session -t termuxpro-target 2>/dev/null || true; "
                + "tmux kill-session -t manual-fixture 2>/dev/null || true", 4096);
    }

    @Test
    public void executesThroughRealOpenSshAndLimitsOutput() {
        RemoteCommandRunner.Result success = runner.run(target, port,
            "printf 'fixture-ok'", 1024);
        assertEquals(success.output, 0, success.exitCode);
        assertEquals("fixture-ok", success.output);
        assertFalse(success.truncated);

        RemoteCommandRunner.Result limited = runner.run(target, port,
            "yes x | head -c 4096", 64);
        assertEquals(limited.output, 0, limited.exitCode);
        assertEquals(64, limited.output.length());
        assertTrue(limited.truncated);
    }

    @Test
    public void listsRealTmuxSessionsAndKeepsUnknownOwnershipReadOnly() {
        String fingerprint = WorkspaceCommandBuilder.workspaceFingerprint(target, port, "~/repo");
        RemoteCommandRunner.Result setup = runner.run(target, port,
            "tmux new-session -d -s manual-fixture; "
                + "tmux new-session -d -s termuxpro-fixture; "
                + "tmux set-option -t termuxpro-fixture "
                + WorkspaceCommandBuilder.TMUX_OWNER_OPTION + " '" + OWNER + "'; "
                + "tmux set-option -t termuxpro-fixture "
                + WorkspaceCommandBuilder.TMUX_WORKSPACE_OPTION + " '" + fingerprint + "'", 4096);
        assertEquals(setup.output, 0, setup.exitCode);

        RemoteCommandRunner.Result result = runner.run(target, port,
            WorkspaceCommandBuilder.buildListTmuxSessionsRemoteCommand(), 32_000);
        assertEquals(result.output, 0, result.exitCode);
        List<TmuxSessionInfo> sessions = TmuxSessionParser.parse(result.output, OWNER, fingerprint);
        TmuxSessionInfo manual = find(sessions, "manual-fixture");
        TmuxSessionInfo managed = find(sessions, "termuxpro-fixture");
        assertFalse(manual.managedByTermuxPro);
        assertTrue(managed.managedByTermuxPro);
    }

    @Test
    public void refusesToStopPrefixSpoofAndRechecksOwnerBeforeKill() {
        String fingerprint = WorkspaceCommandBuilder.workspaceFingerprint(target, port, "~/repo");
        RemoteCommandRunner.Result setup = runner.run(target, port,
            "tmux new-session -d -s termuxpro-fixture; "
                + "tmux set-option -t termuxpro-fixture "
                + WorkspaceCommandBuilder.TMUX_OWNER_OPTION + " wrong-owner; "
                + "tmux set-option -t termuxpro-fixture "
                + WorkspaceCommandBuilder.TMUX_WORKSPACE_OPTION + " '" + fingerprint + "'", 4096);
        assertEquals(setup.output, 0, setup.exitCode);

        RemoteCommandRunner.Result refused = runner.run(target, port,
            WorkspaceCommandBuilder.buildStopTaskSessionRemoteCommand(
                "termuxpro-fixture", OWNER, target, port, "~/repo"), 4096);
        assertTrue(refused.exitCode != 0);
        RemoteCommandRunner.Result stillExists = runner.run(target, port,
            "tmux has-session -t '=termuxpro-fixture'", 4096);
        assertEquals(stillExists.output, 0, stillExists.exitCode);

        runner.run(target, port, "tmux set-option -t termuxpro-fixture "
            + WorkspaceCommandBuilder.TMUX_OWNER_OPTION + " '" + OWNER + "'", 4096);
        RemoteCommandRunner.Result stopped = runner.run(target, port,
            WorkspaceCommandBuilder.buildStopTaskSessionRemoteCommand(
                "termuxpro-fixture", OWNER, target, port, "~/repo"), 4096);
        assertEquals(stopped.output, 0, stopped.exitCode);
    }

    @Test
    public void createsRenamesAndStopsOwnedSessionThroughRealOpenSsh() {
        String fingerprint = WorkspaceCommandBuilder.workspaceFingerprint(target, port, "~/repo");
        RemoteCommandRunner.Result create = runner.run(target, port,
            WorkspaceCommandBuilder.buildCreateTaskSessionRemoteCommand(
                "termuxpro-fixture", OWNER, target, port, "~/repo"), 4096);
        assertEquals(create.output, 0, create.exitCode);

        RemoteCommandRunner.Result owner = runner.run(target, port,
            "test \"$(tmux show-options -v -t termuxpro-fixture "
                + WorkspaceCommandBuilder.TMUX_OWNER_OPTION + ")\" = '" + OWNER + "'"
                + " && test \"$(tmux show-options -v -t termuxpro-fixture "
                + WorkspaceCommandBuilder.TMUX_WORKSPACE_OPTION + ")\" = '" + fingerprint + "'", 4096);
        assertEquals(owner.output, 0, owner.exitCode);

        RemoteCommandRunner.Result rename = runner.run(target, port,
            WorkspaceCommandBuilder.buildRenameTaskSessionRemoteCommand(
                "termuxpro-fixture", "termuxpro-renamed", OWNER, target, port, "~/repo"), 4096);
        assertEquals(rename.output, 0, rename.exitCode);
        assertEquals(0, runner.run(target, port,
            "tmux has-session -t '=termuxpro-renamed'", 4096).exitCode);

        RemoteCommandRunner.Result stop = runner.run(target, port,
            WorkspaceCommandBuilder.buildStopTaskSessionRemoteCommand(
                "termuxpro-renamed", OWNER, target, port, "~/repo"), 4096);
        assertEquals(stop.output, 0, stop.exitCode);
    }

    @Test
    public void duplicateTargetAndMissingSourceFailWithoutTouchingOtherSessions() {
        RemoteCommandRunner.Result setup = runner.run(target, port,
            "tmux new-session -d -s manual-fixture; tmux new-session -d -s termuxpro-target", 4096);
        assertEquals(setup.output, 0, setup.exitCode);

        RemoteCommandRunner.Result duplicateCreate = runner.run(target, port,
            WorkspaceCommandBuilder.buildCreateTaskSessionRemoteCommand(
                "termuxpro-target", OWNER, target, port, "~/repo"), 4096);
        assertEquals(74, duplicateCreate.exitCode);

        RemoteCommandRunner.Result missingRename = runner.run(target, port,
            WorkspaceCommandBuilder.buildRenameTaskSessionRemoteCommand(
                "termuxpro-fixture", "termuxpro-renamed", OWNER, target, port, "~/repo"), 4096);
        assertEquals(72, missingRename.exitCode);
        assertEquals(0, runner.run(target, port,
            "tmux has-session -t '=manual-fixture' && tmux has-session -t '=termuxpro-target'", 4096).exitCode);
    }

    @Test
    public void renameRechecksOwnershipAndRejectsExistingTarget() {
        String fingerprint = WorkspaceCommandBuilder.workspaceFingerprint(target, port, "~/repo");
        RemoteCommandRunner.Result setup = runner.run(target, port,
            "tmux new-session -d -s manual-fixture; "
                + "tmux new-session -d -s termuxpro-fixture; "
                + "tmux set-option -t termuxpro-fixture "
                + WorkspaceCommandBuilder.TMUX_OWNER_OPTION + " wrong-owner; "
                + "tmux set-option -t termuxpro-fixture "
                + WorkspaceCommandBuilder.TMUX_WORKSPACE_OPTION + " '" + fingerprint + "'", 4096);
        assertEquals(setup.output, 0, setup.exitCode);

        RemoteCommandRunner.Result changedOwner = runner.run(target, port,
            WorkspaceCommandBuilder.buildRenameTaskSessionRemoteCommand(
                "termuxpro-fixture", "termuxpro-renamed", OWNER, target, port, "~/repo"), 4096);
        assertTrue(changedOwner.exitCode != 0);
        assertEquals(0, runner.run(target, port,
            "tmux has-session -t '=termuxpro-fixture' && tmux has-session -t '=manual-fixture'", 4096).exitCode);

        RemoteCommandRunner.Result conflictSetup = runner.run(target, port,
            "tmux set-option -t termuxpro-fixture " + WorkspaceCommandBuilder.TMUX_OWNER_OPTION
                + " '" + OWNER + "'; tmux new-session -d -s termuxpro-target", 4096);
        assertEquals(conflictSetup.output, 0, conflictSetup.exitCode);
        RemoteCommandRunner.Result targetConflict = runner.run(target, port,
            WorkspaceCommandBuilder.buildRenameTaskSessionRemoteCommand(
                "termuxpro-fixture", "termuxpro-target", OWNER, target, port, "~/repo"), 4096);
        assertEquals(74, targetConflict.exitCode);
        assertEquals(0, runner.run(target, port,
            "tmux has-session -t '=termuxpro-fixture' && tmux has-session -t '=termuxpro-target'", 4096).exitCode);
    }

    @Test
    public void reportsMissingTmuxAndClassifiesRealRefusedConnection() {
        RemoteCommandRunner.Result missing = runner.run(target, port,
            "PATH=/nonexistent; " + WorkspaceCommandBuilder.buildListTmuxSessionsRemoteCommand(),
            4096);
        assertEquals(missing.output, 0, missing.exitCode);
        assertTrue(TmuxSessionParser.reportsMissingTmux(missing.output));

        RemoteCommandRunner.Result refused = runner.run(target, 1, "true", 4096);
        assertTrue(refused.exitCode != 0);
        assertEquals(SshFailureClassifier.Reason.REFUSED,
            SshFailureClassifier.classify(refused));
    }

    private static TmuxSessionInfo find(List<TmuxSessionInfo> sessions, String name) {
        for (TmuxSessionInfo session : sessions) {
            if (name.equals(session.name)) return session;
        }
        throw new AssertionError("缺少 tmux 会话：" + name);
    }
}
