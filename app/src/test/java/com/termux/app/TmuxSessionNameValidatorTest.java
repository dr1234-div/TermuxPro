package com.termux.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TmuxSessionNameValidatorTest {
    @Test
    public void acceptsPortableNamesAndRejectsShellSyntaxOrOversizedValues() {
        assertTrue(TmuxSessionNameValidator.isValid("feature-login_2"));
        assertFalse(TmuxSessionNameValidator.isValid("feature-login.2"));
        assertFalse(TmuxSessionNameValidator.isValid("  codex-work  "));
        assertFalse(TmuxSessionNameValidator.isValid(""));
        assertFalse(TmuxSessionNameValidator.isValid("-starts-with-dash"));
        assertFalse(TmuxSessionNameValidator.isValid("name with spaces"));
        assertFalse(TmuxSessionNameValidator.isValid("work; tmux kill-server"));
        assertFalse(TmuxSessionNameValidator.isValid(
            "a1234567890123456789012345678901234567890123456789012345678901234"));
    }
}
