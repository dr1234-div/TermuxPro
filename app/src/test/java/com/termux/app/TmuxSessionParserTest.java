package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class TmuxSessionParserTest {
    private static final String OWNER = "11111111-2222-3333-4444-555555555555";
    private static final String FINGERPRINT = "workspace-fingerprint";

    @Test
    public void parsesAllSessionsAndKeepsOwnershipConservative() {
        List<TmuxSessionInfo> sessions = TmuxSessionParser.parse(
            "manual\u00002\u00001\u0000100\u0000200\u0000\u0000\u0000"
                + "mobile-task-a\u00001\u00000\u0000110\u0000210\u0000wrong\u0000" + FINGERPRINT + "\u0000"
                + "termuxpro-work\u00003\u00000\u0000120\u0000220\u0000" + OWNER + "\u0000" + FINGERPRINT + "\u0000",
            OWNER, FINGERPRINT);

        assertEquals(3, sessions.size());
        assertEquals("manual", sessions.get(0).name);
        assertTrue(sessions.get(0).attached);
        assertEquals(100L, sessions.get(0).createdEpochSeconds);
        assertEquals(200L, sessions.get(0).activityEpochSeconds);
        assertFalse(sessions.get(0).managedByTermuxPro);
        assertEquals(TmuxSessionInfo.OwnershipState.UNMARKED, sessions.get(0).ownershipState);
        assertFalse(sessions.get(1).managedByTermuxPro);
        assertEquals(TmuxSessionInfo.OwnershipState.OTHER_OWNER, sessions.get(1).ownershipState);
        assertTrue(sessions.get(2).managedByTermuxPro);
        assertEquals(TmuxSessionInfo.OwnershipState.CURRENT_WORKSPACE, sessions.get(2).ownershipState);
    }

    @Test
    public void ignoresMalformedRecordsAndDetectsMissingTmux() {
        assertTrue(TmuxSessionParser.parse("broken\u0000x\u00000\u00000\u00000\u0000\u0000\u0000",
            OWNER, FINGERPRINT).isEmpty());
        assertTrue(TmuxSessionParser.reportsMissingTmux(
            TmuxSessionParser.MISSING_MARKER + "\u0000\u0000\u0000"));
    }

    @Test
    public void keepsColonInsideSessionNameBecauseRecordsUseNulFields() {
        List<TmuxSessionInfo> sessions = TmuxSessionParser.parse(
            "shared:ops\u00001\u00000\u0000300\u0000400\u0000\u0000\u0000",
            OWNER, FINGERPRINT);

        assertEquals(1, sessions.size());
        assertEquals("shared:ops", sessions.get(0).name);
        assertEquals(300L, sessions.get(0).createdEpochSeconds);
        assertEquals(400L, sessions.get(0).activityEpochSeconds);
        assertFalse(sessions.get(0).managedByTermuxPro);
    }

    @Test
    public void distinguishesOtherWorkspaceFromUnmarkedAndOtherOwner() {
        List<TmuxSessionInfo> sessions = TmuxSessionParser.parse(
            "other-project\u00001\u00000\u0000500\u0000600\u0000" + OWNER + "\u0000other-workspace\u0000"
                + "other-owner\u00001\u00000\u0000501\u0000601\u0000"
                + "99999999-8888-7777-6666-555555555555\u0000" + FINGERPRINT + "\u0000"
                + "partial\u00001\u00000\u0000502\u0000602\u0000\u0000" + FINGERPRINT + "\u0000",
            OWNER, FINGERPRINT);

        assertEquals(3, sessions.size());
        assertEquals(TmuxSessionInfo.OwnershipState.OTHER_WORKSPACE, sessions.get(0).ownershipState);
        assertFalse(sessions.get(0).managedByTermuxPro);
        assertEquals(TmuxSessionInfo.OwnershipState.OTHER_OWNER, sessions.get(1).ownershipState);
        assertFalse(sessions.get(1).managedByTermuxPro);
        assertEquals(TmuxSessionInfo.OwnershipState.INCOMPLETE_MARKER, sessions.get(2).ownershipState);
        assertFalse(sessions.get(2).managedByTermuxPro);
    }
}
