package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class ConnectionDiagnosticReportTest {

    @Test
    public void parsesAvailabilityWithoutDependingOnFieldOrder() {
        List<ConnectionDiagnosticReport.Item> items = ConnectionDiagnosticReport.parse(
            "SYSTEM\0Linux aarch64\0PROJECT\0OK\0CODEX\0MISSING\0TMUX\0OK\0");

        assertEquals("系统", items.get(0).label);
        assertEquals("项目目录", items.get(1).label);
        assertTrue(items.get(1).available);
        assertTrue(items.get(2).available);
        assertFalse(items.get(3).available);
    }
}
