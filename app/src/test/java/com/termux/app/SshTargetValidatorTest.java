package com.termux.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SshTargetValidatorTest {

    @Test
    public void acceptsAliasesHostsAndIpv6() {
        assertTrue(SshTargetValidator.isValid("company-dev"));
        assertTrue(SshTargetValidator.isValid("user@example.com"));
        assertTrue(SshTargetValidator.isValid("10.0.0.8"));
        assertTrue(SshTargetValidator.isValid("user@2001:db8::8"));
    }

    @Test
    public void rejectsOptionAndArgumentInjection() {
        assertFalse(SshTargetValidator.isValid("-oProxyCommand=evil"));
        assertFalse(SshTargetValidator.isValid("host -p 1"));
        assertFalse(SshTargetValidator.isValid("user@@host"));
        assertFalse(SshTargetValidator.isValid("user@"));
        assertFalse(SshTargetValidator.isValid("host\ncommand"));
        assertFalse(SshTargetValidator.isValid("host;command"));
        assertFalse(SshTargetValidator.isValid("host|command"));
        assertFalse(SshTargetValidator.isValid("host/../config"));
    }
}
