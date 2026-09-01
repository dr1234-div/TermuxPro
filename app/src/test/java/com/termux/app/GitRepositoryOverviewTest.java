package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class GitRepositoryOverviewTest {

    @Test
    public void parsesBranchChangesUpstreamBranchesAndLog() {
        GitRepositoryOverview result = GitRepositoryOverview.parse(
            "TP_OVERVIEW\tdev\t0\t3\t1\t2\t2\t1\t1\torigin/dev\n"
                + "TP_LOCAL\tdev\nTP_LOCAL\tmaster\n"
                + "TP_REMOTE\torigin/dev\n"
                + "TP_REMOTE\torigin/HEAD\n"
                + "TP_LOG\ta1b2c3d\t2 hours ago\t修复切换逻辑\n"
                + "TP_STASH\tstash@{0}\t3 minutes ago\tWIP before review\n");

        assertEquals("dev", result.head);
        assertFalse(result.detached);
        assertEquals(3, result.changedFiles);
        assertEquals(1, result.stagedFiles);
        assertEquals(2, result.unstagedFiles);
        assertEquals(Integer.valueOf(2), result.ahead);
        assertEquals(Integer.valueOf(1), result.behind);
        assertEquals("origin/dev", result.upstream);
        assertEquals(2, result.localBranches.size());
        assertEquals("origin/dev", result.remoteBranches.get(0));
        assertEquals("修复切换逻辑", result.commits.get(0).subject);
        assertEquals("stash@{0}", result.stashes.get(0).ref);
        assertEquals("WIP before review", result.stashes.get(0).subject);
        assertTrue(result.fileChanges.isEmpty());
    }

    @Test
    public void parsesNulSeparatedFileChanges() {
        GitRepositoryOverview result = GitRepositoryOverview.parse(
            "TP_OVERVIEW\tdev\t0\t3\t2\t2\t\t\t0\n"
                + "TP_STATUS_Z\000"
                + "M  staged.txt\000"
                + " M unstaged.txt\000"
                + "MM mixed.txt\000"
                + "?? new file.txt\000");

        assertEquals(4, result.fileChanges.size());
        assertTrue(result.fileChanges.get(0).hasStagedChange());
        assertFalse(result.fileChanges.get(0).hasUnstagedChange());
        assertFalse(result.fileChanges.get(1).hasStagedChange());
        assertTrue(result.fileChanges.get(1).hasUnstagedChange());
        assertTrue(result.fileChanges.get(2).hasStagedChange());
        assertTrue(result.fileChanges.get(2).hasUnstagedChange());
        assertEquals("new file.txt", result.fileChanges.get(3).path);
    }

    @Test
    public void parsesDetachedHeadWithoutUpstream() {
        GitRepositoryOverview result = GitRepositoryOverview.parse(
            "TP_OVERVIEW\t1a2b3c4\t1\t0\t0\t0\t\t\t0\n");

        assertTrue(result.detached);
        assertEquals(0, result.stagedFiles);
        assertEquals(0, result.unstagedFiles);
        assertNull(result.ahead);
        assertNull(result.behind);
        assertNull(result.upstream);
    }

    @Test
    public void keepsBackwardCompatibilityWithOlderOverviewProtocol() {
        GitRepositoryOverview result = GitRepositoryOverview.parse(
            "TP_OVERVIEW\tdev\t0\t2\t\t\t0\n");

        assertEquals(2, result.changedFiles);
        assertEquals(0, result.stagedFiles);
        assertEquals(2, result.unstagedFiles);
        assertNull(result.upstream);
    }

    @Test
    public void rejectsMissingOrMalformedOverview() {
        assertThrows(IllegalArgumentException.class,
            () -> GitRepositoryOverview.parse("TP_LOCAL\tdev\n"));
        assertThrows(IllegalArgumentException.class,
            () -> GitRepositoryOverview.parse("TP_OVERVIEW\tdev\t0\t-1\t0\t0\t\t\t0\n"));
        assertThrows(IllegalArgumentException.class,
            () -> GitRepositoryOverview.parse("TP_OVERVIEW\tdev\t0\t0\t0\t0\t\t\t0\n"
                + "TP_STASH\tstash@{bad}\tnow\tinvalid\n"));
    }
}
