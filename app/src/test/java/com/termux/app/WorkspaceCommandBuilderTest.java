package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorkspaceCommandBuilderTest {

    @Test
    public void buildsCustomCommandForExplicitTargetAndDirectory() {
        String result = WorkspaceCommandBuilder.buildCustomCommandSshCommand(
            "developer@example.com", 2222, "~/project", "~/project/mobile app",
            "git status --short && printf '%s\\n' \"done\"");

        assertTrue(result.contains("-p 2222 -- 'developer@example.com'"));
        assertTrue(result.contains("Command directory is unavailable"));
        assertTrue(result.contains("mobile app"));
        assertTrue(result.contains("git status --short"));
        assertFalse(result.contains("tmux"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSecretInCustomCommand() {
        WorkspaceCommandBuilder.buildCustomCommandSshCommand(
            "developer@example.com", 22, "~/project", "", "TOKEN=plain-secret npm test");
    }
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
    public void gitOverviewAndBranchSwitchUseStableProtocolAndShellQuoting() {
        String overview = WorkspaceCommandBuilder.buildGitOverviewRemoteCommand("~/team app");
        String branch = WorkspaceCommandBuilder.buildGitSwitchBranchRemoteCommand(
            "/srv/team's app", "feature/user's-work");
        String newBranch = WorkspaceCommandBuilder.buildGitCreateBranchRemoteCommand(
            "/srv/team's app", "feature/local-ui");
        String remoteBranch = WorkspaceCommandBuilder.buildGitTrackRemoteBranchCommand(
            "/srv/team's app", "origin/feature/user's-work");
        String deleteBranch = WorkspaceCommandBuilder.buildGitDeleteLocalBranchRemoteCommand(
            "/srv/team's app", "feature/local-ui");
        String fetch = WorkspaceCommandBuilder.buildGitFetchUpstreamRemoteCommand("/srv/team's app");
        String pull = WorkspaceCommandBuilder.buildGitPullFastForwardRemoteCommand("/srv/team's app");
        String push = WorkspaceCommandBuilder.buildGitPushUpstreamRemoteCommand("/srv/team's app");
        String stageFile = WorkspaceCommandBuilder.buildGitStageFileRemoteCommand(
            "/srv/team's app", "src/user's file.txt");
        String unstageFile = WorkspaceCommandBuilder.buildGitUnstageFileRemoteCommand(
            "/srv/team's app", "src/user's file.txt");
        String stashPush = WorkspaceCommandBuilder.buildGitStashPushRemoteCommand(
            "/srv/team's app", "WIP user's task");
        String stashApply = WorkspaceCommandBuilder.buildGitStashApplyRemoteCommand(
            "/srv/team's app", "stash@{0}");
        String stashDrop = WorkspaceCommandBuilder.buildGitStashDropRemoteCommand(
            "/srv/team's app", "stash@{0}");

        assertTrue(overview.contains("TP_OVERVIEW\\t"));
        assertTrue(overview.contains("TP_STASH"));
        assertTrue(overview.contains("TP_STATUS_Z\\000"));
        assertTrue(overview.contains("git status --porcelain=v1 -z"));
        assertTrue(overview.contains("upstream_name=$(git rev-parse"));
        assertTrue(overview.contains("git status --porcelain=v1 -z"));
        assertTrue(overview.contains("git diff --cached --name-only -z"));
        assertTrue(overview.contains("git ls-files --others --exclude-standard -z"));
        assertTrue(overview.contains("refs/heads"));
        assertTrue(overview.contains("grep -v '/HEAD$'"));
        assertTrue(overview.contains("git log -20"));
        assertTrue(branch.contains("'/srv/team'\\''s app'"));
        assertTrue(branch.contains("then exit 77; fi"));
        assertTrue(branch.endsWith("'feature/user'\\''s-work'"));
        assertTrue(newBranch.contains("git check-ref-format --branch 'feature/local-ui'"));
        assertTrue(newBranch.contains("git switch -c 'feature/local-ui'"));
        assertTrue(deleteBranch.contains("git branch -d -- 'feature/local-ui'"));
        assertTrue(!deleteBranch.contains("git branch -D"));
        assertTrue(!deleteBranch.contains("git push"));
        assertTrue(deleteBranch.contains("exit 79"));
        assertTrue(deleteBranch.contains("exit 80"));
        assertTrue(remoteBranch.contains("git show-ref --verify --quiet refs/heads/'feature/user'\\''s-work'"));
        assertTrue(remoteBranch.contains("then exit 77; fi"));
        assertTrue(remoteBranch.endsWith("git switch --track 'origin/feature/user'\\''s-work'"));
        assertTrue(fetch.contains("git rev-parse --abbrev-ref --symbolic-full-name '@{upstream}'"));
        assertTrue(fetch.contains("if ! upstream=$(git rev-parse"));
        assertTrue(fetch.contains("git fetch --prune \"$remote\""));
        assertTrue(pull.contains("'/srv/team'\\''s app'"));
        assertTrue(pull.contains("staged=$(git diff --cached --name-only -z"));
        assertTrue(pull.contains("then exit 77; fi"));
        assertTrue(pull.contains("then exit 76; fi"));
        assertTrue(pull.endsWith("git pull --ff-only"));
        assertTrue(push.contains("if ! current=$(git symbolic-ref --short HEAD"));
        assertTrue(push.contains("if ! upstream=$(git rev-parse"));
        assertTrue(push.contains("git push \"$remote\" \"HEAD:$branch\""));
        assertTrue(push.endsWith("git update-ref \"refs/remotes/$upstream\" HEAD"));
        assertTrue(stageFile.contains("unstaged_tracked=$(git diff --name-only -z"));
        assertTrue(stageFile.contains("untracked=$(git ls-files --others --exclude-standard -z"));
        assertTrue(stageFile.contains("git add -A -- 'src/user'\\''s file.txt'"));
        assertTrue(!stageFile.contains("git commit"));
        assertTrue(unstageFile.contains("git restore --staged -- 'src/user'\\''s file.txt'"));
        assertTrue(!unstageFile.contains("reset --hard"));
        assertTrue(stashPush.contains("git stash push -u -m 'WIP user'\\''s task'"));
        assertTrue(!stashPush.contains("git commit"));
        assertTrue(!stashPush.contains("git push"));
        assertTrue(stashApply.contains("git stash apply --index 'stash@{0}'"));
        assertTrue(!stashApply.contains("git stash pop"));
        assertTrue(stashApply.contains("then exit 77; fi"));
        assertTrue(stashDrop.contains("git stash drop 'stash@{0}'"));
        assertTrue(!stashDrop.contains("git stash clear"));
        String commit = WorkspaceCommandBuilder.buildGitCommitStagedRemoteCommand(
            "/srv/team's app", "fix: user's mobile flow");
        assertTrue(commit.contains("git diff --cached --name-only -z"));
        assertTrue(commit.contains("git commit -m 'fix: user'\\''s mobile flow'"));
        assertThrows(IllegalArgumentException.class,
            () -> WorkspaceCommandBuilder.buildGitSwitchBranchRemoteCommand("~/app", "bad\nbranch"));
        assertThrows(IllegalArgumentException.class,
            () -> WorkspaceCommandBuilder.buildGitTrackRemoteBranchCommand("~/app", "origin/HEAD"));
        assertThrows(IllegalArgumentException.class,
            () -> WorkspaceCommandBuilder.buildGitCreateBranchRemoteCommand("~/app", "bad branch"));
        assertThrows(IllegalArgumentException.class,
            () -> WorkspaceCommandBuilder.buildGitCreateBranchRemoteCommand("~/app", "-bad"));
        assertThrows(IllegalArgumentException.class,
            () -> WorkspaceCommandBuilder.buildGitDeleteLocalBranchRemoteCommand("~/app", "bad branch"));
        assertThrows(IllegalArgumentException.class,
            () -> WorkspaceCommandBuilder.buildGitDeleteLocalBranchRemoteCommand("~/app", "-bad"));
        assertThrows(IllegalArgumentException.class,
            () -> WorkspaceCommandBuilder.buildGitCommitStagedRemoteCommand("~/app", ""));
        assertThrows(IllegalArgumentException.class,
            () -> WorkspaceCommandBuilder.buildGitCommitStagedRemoteCommand("~/app", "bad\nmessage"));
        assertThrows(IllegalArgumentException.class,
            () -> WorkspaceCommandBuilder.buildGitStageFileRemoteCommand("~/app", ""));
        assertThrows(IllegalArgumentException.class,
            () -> WorkspaceCommandBuilder.buildGitUnstageFileRemoteCommand("~/app", "bad\000file"));
        assertThrows(IllegalArgumentException.class,
            () -> WorkspaceCommandBuilder.buildGitStashPushRemoteCommand("~/app", "bad\nstash"));
        assertThrows(IllegalArgumentException.class,
            () -> WorkspaceCommandBuilder.buildGitStashApplyRemoteCommand("~/app", "stash@{bad}"));
        assertThrows(IllegalArgumentException.class,
            () -> WorkspaceCommandBuilder.buildGitStashDropRemoteCommand("~/app", "stash@{0};rm"));
    }

    @Test
    public void gitOverviewRunsAgainstRealRepositoryIncludingUnbornHead() throws Exception {
        Assume.assumeTrue(new File("/usr/bin/git").canExecute());
        Path repository = Files.createTempDirectory("termuxpro git repo");
        assertEquals(0, runShell("git init -q -- " + WorkspaceCommandBuilder.shellQuote(
            repository.toString()), new HashMap<>()));

        ShellOutput unborn = runShellCapture(WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(
            repository.toString()));
        assertEquals(0, unborn.exitCode);
        GitRepositoryOverview unbornOverview = GitRepositoryOverview.parse(unborn.output);
        assertEquals("master", unbornOverview.head);
        assertTrue(unbornOverview.commits.isEmpty());

        String setup = "cd -- " + WorkspaceCommandBuilder.shellQuote(repository.toString())
            + " && git config user.name test && git config user.email test@example.com"
            + " && printf content > README.md && git add README.md && git commit -q -m initial"
            + " && git branch feature && printf changed >> README.md";
        assertEquals(0, runShell(setup, new HashMap<>()));
        ShellOutput populated = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(repository.toString()));
        assertEquals(0, populated.exitCode);
        GitRepositoryOverview overview = GitRepositoryOverview.parse(populated.output);
        assertEquals(1, overview.changedFiles);
        assertEquals(0, overview.stagedFiles);
        assertEquals(1, overview.unstagedFiles);
        assertEquals("README.md", overview.fileChanges.get(0).path);
        assertTrue(overview.fileChanges.get(0).hasUnstagedChange());
        assertTrue(overview.localBranches.contains("feature"));
        assertEquals(1, overview.commits.size());
        assertEquals(77, runShell(WorkspaceCommandBuilder.buildGitSwitchBranchRemoteCommand(
            repository.toString(), "feature"), new HashMap<>()));

        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitStageFileRemoteCommand(
            repository.toString(), "README.md"), new HashMap<>()));
        ShellOutput singleStaged = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(repository.toString()));
        GitRepositoryOverview singleStagedOverview = GitRepositoryOverview.parse(singleStaged.output);
        assertEquals(1, singleStagedOverview.stagedFiles);
        assertEquals(0, singleStagedOverview.unstagedFiles);
        assertTrue(singleStagedOverview.fileChanges.get(0).hasStagedChange());
        assertEquals(75, runShell(WorkspaceCommandBuilder.buildGitStageFileRemoteCommand(
            repository.toString(), "README.md"), new HashMap<>()));

        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitUnstageFileRemoteCommand(
            repository.toString(), "README.md"), new HashMap<>()));
        ShellOutput singleUnstaged = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(repository.toString()));
        GitRepositoryOverview singleUnstagedOverview = GitRepositoryOverview.parse(singleUnstaged.output);
        assertEquals(0, singleUnstagedOverview.stagedFiles);
        assertEquals(1, singleUnstagedOverview.unstagedFiles);
        assertEquals(75, runShell(WorkspaceCommandBuilder.buildGitUnstageFileRemoteCommand(
            repository.toString(), "README.md"), new HashMap<>()));

        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitStashPushRemoteCommand(
            repository.toString(), "WIP mobile stash"), new HashMap<>()));
        ShellOutput stashed = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(repository.toString()));
        GitRepositoryOverview stashedOverview = GitRepositoryOverview.parse(stashed.output);
        assertEquals(0, stashedOverview.changedFiles);
        assertEquals(1, stashedOverview.stashes.size());
        assertEquals("stash@{0}", stashedOverview.stashes.get(0).ref);
        assertTrue(stashedOverview.stashes.get(0).subject.contains("WIP mobile stash"));
        assertEquals(75, runShell(WorkspaceCommandBuilder.buildGitStashPushRemoteCommand(
            repository.toString(), "nothing to stash"), new HashMap<>()));
        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitStashApplyRemoteCommand(
            repository.toString(), "stash@{0}"), new HashMap<>()));
        ShellOutput appliedStash = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(repository.toString()));
        GitRepositoryOverview appliedStashOverview = GitRepositoryOverview.parse(appliedStash.output);
        assertEquals(1, appliedStashOverview.changedFiles);
        assertEquals(1, appliedStashOverview.stashes.size());
        assertEquals(77, runShell(WorkspaceCommandBuilder.buildGitStashApplyRemoteCommand(
            repository.toString(), "stash@{0}"), new HashMap<>()));
        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitStashDropRemoteCommand(
            repository.toString(), "stash@{0}"), new HashMap<>()));
        assertEquals(81, runShell(WorkspaceCommandBuilder.buildGitStashDropRemoteCommand(
            repository.toString(), "stash@{0}"), new HashMap<>()));

        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitStageAllRemoteCommand(
            repository.toString()), new HashMap<>()));
        ShellOutput staged = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(repository.toString()));
        GitRepositoryOverview stagedOverview = GitRepositoryOverview.parse(staged.output);
        assertEquals(1, stagedOverview.changedFiles);
        assertEquals(1, stagedOverview.stagedFiles);
        assertEquals(0, stagedOverview.unstagedFiles);
        assertEquals(75, runShell(WorkspaceCommandBuilder.buildGitStageAllRemoteCommand(
            repository.toString()), new HashMap<>()));

        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitUnstageAllRemoteCommand(
            repository.toString()), new HashMap<>()));
        ShellOutput unstaged = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(repository.toString()));
        GitRepositoryOverview unstagedOverview = GitRepositoryOverview.parse(unstaged.output);
        assertEquals(1, unstagedOverview.changedFiles);
        assertEquals(0, unstagedOverview.stagedFiles);
        assertEquals(1, unstagedOverview.unstagedFiles);
        assertEquals(75, runShell(WorkspaceCommandBuilder.buildGitUnstageAllRemoteCommand(
            repository.toString()), new HashMap<>()));

        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitStageAllRemoteCommand(
            repository.toString()), new HashMap<>()));
        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitCommitStagedRemoteCommand(
            repository.toString(), "fix: mobile git workbench"), new HashMap<>()));
        ShellOutput committed = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(repository.toString()));
        GitRepositoryOverview committedOverview = GitRepositoryOverview.parse(committed.output);
        assertEquals(0, committedOverview.changedFiles);
        assertEquals("fix: mobile git workbench", committedOverview.commits.get(0).subject);
        assertEquals(75, runShell(WorkspaceCommandBuilder.buildGitCommitStagedRemoteCommand(
            repository.toString(), "fix: nothing staged"), new HashMap<>()));
        assertEquals(76, runShell(WorkspaceCommandBuilder.buildGitFetchUpstreamRemoteCommand(
            repository.toString()), new HashMap<>()));
        assertEquals(76, runShell(WorkspaceCommandBuilder.buildGitPullFastForwardRemoteCommand(
            repository.toString()), new HashMap<>()));
        assertEquals(76, runShell(WorkspaceCommandBuilder.buildGitPushUpstreamRemoteCommand(
            repository.toString()), new HashMap<>()));

        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitSwitchBranchRemoteCommand(
            repository.toString(), "feature"), new HashMap<>()));
        ShellOutput switched = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(repository.toString()));
        assertEquals("feature", GitRepositoryOverview.parse(switched.output).head);

        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitCreateBranchRemoteCommand(
            repository.toString(), "mobile-ui"), new HashMap<>()));
        ShellOutput created = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(repository.toString()));
        GitRepositoryOverview createdOverview = GitRepositoryOverview.parse(created.output);
        assertEquals("mobile-ui", createdOverview.head);
        assertTrue(createdOverview.localBranches.contains("mobile-ui"));
        assertEquals(74, runShell(WorkspaceCommandBuilder.buildGitCreateBranchRemoteCommand(
            repository.toString(), "mobile-ui"), new HashMap<>()));
        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitDeleteLocalBranchRemoteCommand(
            repository.toString(), "feature"), new HashMap<>()));
        ShellOutput deleted = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(repository.toString()));
        assertTrue(!GitRepositoryOverview.parse(deleted.output).localBranches.contains("feature"));
        assertEquals(80, runShell(WorkspaceCommandBuilder.buildGitDeleteLocalBranchRemoteCommand(
            repository.toString(), "feature"), new HashMap<>()));
        assertEquals(79, runShell(WorkspaceCommandBuilder.buildGitDeleteLocalBranchRemoteCommand(
            repository.toString(), "mobile-ui"), new HashMap<>()));

        String unmerged = "cd -- " + WorkspaceCommandBuilder.shellQuote(repository.toString())
            + " && git switch -q -c unmerged-delete"
            + " && printf unmerged > unmerged.txt && git add unmerged.txt"
            + " && git commit -q -m unmerged-delete"
            + " && git switch -q master";
        assertEquals(0, runShell(unmerged, new HashMap<>()));
        assertTrue(runShell(WorkspaceCommandBuilder.buildGitDeleteLocalBranchRemoteCommand(
            repository.toString(), "unmerged-delete"), new HashMap<>()) != 0);
        ShellOutput preserved = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(repository.toString()));
        assertTrue(GitRepositoryOverview.parse(preserved.output).localBranches
            .contains("unmerged-delete"));
    }

    @Test
    public void gitTrackRemoteBranchCreatesLocalTrackingBranchWithoutOverwriting() throws Exception {
        Assume.assumeTrue(new File("/usr/bin/git").canExecute());
        Path remote = Files.createTempDirectory("termuxpro git remote");
        Path seed = Files.createTempDirectory("termuxpro git seed");
        Path clone = Files.createTempDirectory("termuxpro git clone");
        assertEquals(0, runShell("git init --bare -q -- " + WorkspaceCommandBuilder.shellQuote(
            remote.toString()), new HashMap<>()));
        String setup = "git init -q -- " + WorkspaceCommandBuilder.shellQuote(seed.toString())
            + " && cd -- " + WorkspaceCommandBuilder.shellQuote(seed.toString())
            + " && git config user.name test && git config user.email test@example.com"
            + " && printf content > README.md && git add README.md && git commit -q -m initial"
            + " && git branch feature/mobile"
            + " && git remote add origin " + WorkspaceCommandBuilder.shellQuote(remote.toString())
            + " && git push -q origin master feature/mobile";
        assertEquals(0, runShell(setup, new HashMap<>()));
        assertEquals(0, runShell("git clone -q " + WorkspaceCommandBuilder.shellQuote(remote.toString())
            + " " + WorkspaceCommandBuilder.shellQuote(clone.toString()), new HashMap<>()));

        ShellOutput overview = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(clone.toString()));
        assertEquals(0, overview.exitCode);
        GitRepositoryOverview initialOverview = GitRepositoryOverview.parse(overview.output);
        assertEquals("origin/master", initialOverview.upstream);
        assertTrue(initialOverview.remoteBranches.contains("origin/feature/mobile"));
        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitFetchUpstreamRemoteCommand(
            clone.toString()), new HashMap<>()));

        String upstreamChange = "cd -- " + WorkspaceCommandBuilder.shellQuote(seed.toString())
            + " && printf upstream >> README.md && git add README.md"
            + " && git commit -q -m upstream-change"
            + " && git push -q origin master";
        assertEquals(0, runShell(upstreamChange, new HashMap<>()));
        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitFetchUpstreamRemoteCommand(
            clone.toString()), new HashMap<>()));
        ShellOutput behind = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(clone.toString()));
        GitRepositoryOverview behindOverview = GitRepositoryOverview.parse(behind.output);
        assertEquals(Integer.valueOf(1), behindOverview.behind);
        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitPullFastForwardRemoteCommand(
            clone.toString()), new HashMap<>()));
        ShellOutput pulled = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(clone.toString()));
        GitRepositoryOverview pulledOverview = GitRepositoryOverview.parse(pulled.output);
        assertEquals(Integer.valueOf(0), pulledOverview.behind);
        String localChange = "cd -- " + WorkspaceCommandBuilder.shellQuote(clone.toString())
            + " && git config user.name test && git config user.email test@example.com"
            + " && printf local >> README.md && git add README.md"
            + " && git commit -q -m local-change";
        assertEquals(0, runShell(localChange, new HashMap<>()));
        ShellOutput ahead = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(clone.toString()));
        GitRepositoryOverview aheadOverview = GitRepositoryOverview.parse(ahead.output);
        assertEquals(Integer.valueOf(1), aheadOverview.ahead);
        assertEquals(Integer.valueOf(0), aheadOverview.behind);
        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitPushUpstreamRemoteCommand(
            clone.toString()), new HashMap<>()));
        ShellOutput pushed = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(clone.toString()));
        GitRepositoryOverview pushedOverview = GitRepositoryOverview.parse(pushed.output);
        assertEquals(Integer.valueOf(0), pushedOverview.ahead);

        assertEquals(0, runShell("cd -- " + WorkspaceCommandBuilder.shellQuote(clone.toString())
            + " && printf dirty >> README.md", new HashMap<>()));
        assertEquals(77, runShell(WorkspaceCommandBuilder.buildGitTrackRemoteBranchCommand(
            clone.toString(), "origin/feature/mobile"), new HashMap<>()));
        assertEquals(0, runShell("cd -- " + WorkspaceCommandBuilder.shellQuote(clone.toString())
            + " && git restore -- README.md", new HashMap<>()));

        assertEquals(0, runShell(WorkspaceCommandBuilder.buildGitTrackRemoteBranchCommand(
            clone.toString(), "origin/feature/mobile"), new HashMap<>()));
        ShellOutput switched = runShellCapture(
            WorkspaceCommandBuilder.buildGitOverviewRemoteCommand(clone.toString()));
        GitRepositoryOverview parsed = GitRepositoryOverview.parse(switched.output);
        assertEquals("feature/mobile", parsed.head);
        assertTrue(parsed.localBranches.contains("feature/mobile"));

        assertEquals(74, runShell(WorkspaceCommandBuilder.buildGitTrackRemoteBranchCommand(
            clone.toString(), "origin/feature/mobile"), new HashMap<>()));
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
        assertTrue(command.contains("#{session_created}"));
        assertTrue(command.contains("#{session_activity}"));
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

    @Test
    public void managedTmuxLifecycleCreatesAndRenamesWithoutServerWideKill() {
        String create = WorkspaceCommandBuilder.buildCreateTaskSessionRemoteCommand(
            "feature-login", OWNER, "dev@example.com", 22, "~/repo");
        String rename = WorkspaceCommandBuilder.buildRenameTaskSessionRemoteCommand(
            "feature-login", "feature-done", OWNER, "dev@example.com", 22, "~/repo");

        assertTrue(create.contains("tmux new-session -d -s 'feature-login'"));
        assertTrue(create.contains(WorkspaceCommandBuilder.TMUX_OWNER_OPTION));
        assertTrue(create.contains(WorkspaceCommandBuilder.TMUX_WORKSPACE_OPTION));
        assertTrue(create.contains("has-session -t '=feature-login'"));
        assertTrue(rename.contains("rename-session -t '$sid' 'feature-done'"));
        assertTrue(rename.contains("if-shell -F -t \"$sid\""));
        assertTrue(rename.contains("session ownership changed"));
        assertFalse(create.contains("kill-server"));
        assertFalse(rename.contains("kill-server"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void managedTmuxCreateRejectsUnsafeName() {
        WorkspaceCommandBuilder.buildCreateTaskSessionRemoteCommand(
            "unsafe; touch /tmp/pwned", OWNER, "dev@example.com", 22, "~/repo");
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

    @Test(expected = IllegalArgumentException.class)
    public void createRejectsDotBeforeTmuxCanSilentlyRewriteIt() {
        WorkspaceCommandBuilder.buildCreateTaskSessionRemoteCommand(
            "feature.2", OWNER, "fixture-host", 22, ".");
    }

    @Test
    public void realTmuxRejectsWrongMarkerAndStopsMatchingSession() throws Exception {
        Assume.assumeTrue(new File("/usr/bin/tmux").canExecute());
        String socketName = "termuxpro-test-" + UUID.randomUUID().toString().replace("-", "");
        Path shimDirectory = Files.createTempDirectory("termuxpro-tmux-shim");
        Path tmuxShim = shimDirectory.resolve("tmux");
        Files.write(tmuxShim, ("#!/bin/sh\nexec /usr/bin/tmux -L " + socketName
            + " \"$@\"\n").getBytes(StandardCharsets.UTF_8));
        Assume.assumeTrue("无法创建隔离 tmux 测试入口", tmuxShim.toFile().setExecutable(true));
        Map<String, String> environment = new HashMap<>();
        environment.put("TERMUXPRO_TMUX_TEST_BIN", shimDirectory.toString());
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

            String createdFingerprint = WorkspaceCommandBuilder.workspaceFingerprint("fixture-host", 22, ".");
            String create = WorkspaceCommandBuilder.buildCreateTaskSessionRemoteCommand(
                "termuxpro-created", OWNER, "fixture-host", 22, ".");
            assertEquals(0, runShell(create, environment));
            assertEquals(0, runShell("test \"$(tmux show-options -v -t termuxpro-created "
                + WorkspaceCommandBuilder.TMUX_OWNER_OPTION + ")\" = '" + OWNER + "'", environment));
            assertEquals(0, runShell("test \"$(tmux show-options -v -t termuxpro-created "
                + WorkspaceCommandBuilder.TMUX_WORKSPACE_OPTION + ")\" = '" + createdFingerprint + "'", environment));
            String rename = WorkspaceCommandBuilder.buildRenameTaskSessionRemoteCommand(
                "termuxpro-created", "termuxpro-renamed", OWNER, "fixture-host", 22, ".");
            assertEquals(0, runShell(rename, environment));
            assertEquals(0, runShell("tmux has-session -t '=termuxpro-renamed'", environment));
            String stopRenamed = WorkspaceCommandBuilder.buildStopTaskSessionRemoteCommand(
                "termuxpro-renamed", OWNER, "fixture-host", 22, ".");
            assertEquals(0, runShell(stopRenamed, environment));
            assertTrue(runShell("tmux has-session -t '=termuxpro-renamed'", environment) != 0);

            assertThrows(IllegalArgumentException.class, () ->
                WorkspaceCommandBuilder.buildCreateTaskSessionRemoteCommand(
                    "feature.2", OWNER, "fixture-host", 22, "."));
            assertTrue(runShell("tmux has-session -t '=feature_2'", environment) != 0);
        } finally {
            runShell("/usr/bin/tmux -L " + WorkspaceCommandBuilder.shellQuote(socketName)
                + " kill-server 2>/dev/null || true", new HashMap<>());
            Files.deleteIfExists(tmuxShim);
            Files.deleteIfExists(shimDirectory);
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
        String testBin = environment.get("TERMUXPRO_TMUX_TEST_BIN");
        if (testBin != null) {
            processEnvironment.put("PATH", testBin + File.pathSeparator + processEnvironment.get("PATH"));
        }
        Process process = builder.redirectErrorStream(true).start();
        while (process.getInputStream().read() != -1) {
            // 消费有限测试输出，避免子进程因管道写满而阻塞。
        }
        return process.waitFor();
    }

    private static ShellOutput runShellCapture(String command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", command)
            .redirectErrorStream(true);
        // 所有测试子进程都禁止继承真实 tmux socket，避免未来新增命令时误连用户会话。
        builder.environment().remove("TMUX");
        Process process = builder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = process.getInputStream().read(buffer)) != -1) output.write(buffer, 0, count);
        return new ShellOutput(process.waitFor(), new String(output.toByteArray(), StandardCharsets.UTF_8));
    }

    private static final class ShellOutput {
        final int exitCode;
        final String output;

        ShellOutput(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}
