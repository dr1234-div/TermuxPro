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
            "TP_OVERVIEW\tdev\t0\t3\t1\t2\t2\t1\t1\n"
                + "TP_LOCAL\tdev\nTP_LOCAL\tmaster\n"
                + "TP_REMOTE\torigin/dev\n"
                + "TP_REMOTE\torigin/HEAD\n"
                + "TP_LOG\ta1b2c3d\t2 hours ago\t修复切换逻辑\n");

        assertEquals("dev", result.head);
        assertFalse(result.detached);
        assertEquals(3, result.changedFiles);
        assertEquals(1, result.stagedFiles);
        assertEquals(2, result.unstagedFiles);
        assertEquals(Integer.valueOf(2), result.ahead);
        assertEquals(Integer.valueOf(1), result.behind);
        assertEquals(2, result.localBranches.size());
        assertEquals("origin/dev", result.remoteBranches.get(0));
        assertEquals("修复切换逻辑", result.commits.get(0).subject);
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
    }

    @Test
    public void keepsBackwardCompatibilityWithOlderOverviewProtocol() {
        GitRepositoryOverview result = GitRepositoryOverview.parse(
            "TP_OVERVIEW\tdev\t0\t2\t\t\t0\n");

        assertEquals(2, result.changedFiles);
        assertEquals(0, result.stagedFiles);
        assertEquals(2, result.unstagedFiles);
    }

    @Test
    public void rejectsMissingOrMalformedOverview() {
        assertThrows(IllegalArgumentException.class,
            () -> GitRepositoryOverview.parse("TP_LOCAL\tdev\n"));
        assertThrows(IllegalArgumentException.class,
            () -> GitRepositoryOverview.parse("TP_OVERVIEW\tdev\t0\t-1\t0\t0\t\t\t0\n"));
    }
}
