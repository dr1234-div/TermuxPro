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
            "manual\u00002\u00001\u0000\u0000\u0000"
                + "mobile-task-a\u00001\u00000\u0000wrong\u0000" + FINGERPRINT + "\u0000"
                + "termuxpro-work\u00003\u00000\u0000" + OWNER + "\u0000" + FINGERPRINT + "\u0000",
            OWNER, FINGERPRINT);

        assertEquals(3, sessions.size());
        assertEquals("manual", sessions.get(0).name);
        assertTrue(sessions.get(0).attached);
        assertFalse(sessions.get(0).managedByTermuxPro);
        assertFalse(sessions.get(1).managedByTermuxPro);
        assertTrue(sessions.get(2).managedByTermuxPro);
    }

    @Test
    public void ignoresMalformedRecordsAndDetectsMissingTmux() {
        assertTrue(TmuxSessionParser.parse("broken\u0000x\u00000\u0000\u0000\u0000",
            OWNER, FINGERPRINT).isEmpty());
        assertTrue(TmuxSessionParser.reportsMissingTmux(
            TmuxSessionParser.MISSING_MARKER + "\u0000\u0000\u0000"));
    }
}
