package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AiTerminalActionTest {

    @Test
    public void approvalIsNeverImplicit() {
        assertEquals("\r", AiTerminalAction.payload(AiTerminalAction.Type.CONFIRM_SELECTION));
        assertTrue(AiTerminalAction.requiresConfirmation(AiTerminalAction.Type.CONFIRM_SELECTION));
    }

    @Test
    public void rejectAndInterruptUseTerminalControlCharacters() {
        assertEquals("\u001b", AiTerminalAction.payload(AiTerminalAction.Type.REJECT_OR_BACK));
        assertEquals("\u0003", AiTerminalAction.payload(AiTerminalAction.Type.INTERRUPT));
        assertFalse(AiTerminalAction.requiresConfirmation(AiTerminalAction.Type.INTERRUPT));
    }
}
