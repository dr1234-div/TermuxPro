package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class TmuxSessionParserTest {

    @Test
    public void parsesAllSessionsAndKeepsOwnershipConservative() {
        List<TmuxSessionInfo> sessions = TmuxSessionParser.parse(
            "manual\u00002\u00001\u0000mobile-task-a\u00001\u00000\u0000termuxpro-work\u00003\u00000\u0000");

        assertEquals(3, sessions.size());
        assertEquals("manual", sessions.get(0).name);
        assertTrue(sessions.get(0).attached);
        assertFalse(sessions.get(0).managedByTermuxPro);
        assertTrue(sessions.get(1).managedByTermuxPro);
        assertTrue(sessions.get(2).managedByTermuxPro);
    }

    @Test
    public void ignoresMalformedRecordsAndDetectsMissingTmux() {
        assertTrue(TmuxSessionParser.parse("broken\u0000x\u00000\u0000").isEmpty());
        assertTrue(TmuxSessionParser.reportsMissingTmux(
            TmuxSessionParser.MISSING_MARKER + "\u0000\u0000\u0000"));
    }
}
