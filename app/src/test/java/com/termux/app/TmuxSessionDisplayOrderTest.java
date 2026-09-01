package com.termux.app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;

public final class TmuxSessionDisplayOrderTest {
    private static final String OWNER = "11111111-2222-3333-4444-555555555555";
    private static final String FINGERPRINT = "workspace-fingerprint";

    @Test
    public void currentWorkspaceSessionsAreShownBeforeSharedOrUnknownSessions() {
        List<TmuxSessionInfo> sessions = TmuxSessionParser.parse(
            "manual-latest\u00001\u00000\u0000100\u0000900\u0000\u0000\u0000"
                + "other-workspace\u00001\u00000\u0000100\u0000800\u0000" + OWNER + "\u0000other\u0000"
                + "other-owner\u00001\u00000\u0000100\u0000700\u0000"
                + "99999999-8888-7777-6666-555555555555\u0000" + FINGERPRINT + "\u0000"
                + "partial\u00001\u00000\u0000100\u0000600\u0000\u0000" + FINGERPRINT + "\u0000"
                + "current-old\u00001\u00000\u0000100\u0000500\u0000" + OWNER + "\u0000" + FINGERPRINT + "\u0000",
            OWNER, FINGERPRINT);

        List<TmuxSessionInfo> sorted = TmuxSessionDisplayOrder.sorted(sessions);

        assertEquals("current-old", sorted.get(0).name);
        assertEquals("other-workspace", sorted.get(1).name);
        assertEquals("other-owner", sorted.get(2).name);
        assertEquals("partial", sorted.get(3).name);
        assertEquals("manual-latest", sorted.get(4).name);
    }

    @Test
    public void sameOwnershipUsesRecentActivityThenName() {
        List<TmuxSessionInfo> sessions = TmuxSessionParser.parse(
            "current-b\u00001\u00000\u0000100\u0000500\u0000" + OWNER + "\u0000" + FINGERPRINT + "\u0000"
                + "current-a\u00001\u00000\u0000100\u0000700\u0000" + OWNER + "\u0000" + FINGERPRINT + "\u0000"
                + "current-c\u00001\u00000\u0000200\u0000700\u0000" + OWNER + "\u0000" + FINGERPRINT + "\u0000",
            OWNER, FINGERPRINT);

        List<TmuxSessionInfo> sorted = TmuxSessionDisplayOrder.sorted(sessions);

        assertEquals("current-c", sorted.get(0).name);
        assertEquals("current-a", sorted.get(1).name);
        assertEquals("current-b", sorted.get(2).name);
    }
}
