package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class SshTargetParserTest {

    @Test
    public void acceptsPlainTargetAndPortField() {
        SshTargetParser.ParsedTarget target = SshTargetParser.parse(
            " hdr@192.168.1.153 ", "2222", 22);

        assertEquals("hdr@192.168.1.153", target.host);
        assertEquals(2222, target.port);
    }

    @Test
    public void normalizesInlineHostPortBeforePortField() {
        SshTargetParser.ParsedTarget target = SshTargetParser.parse(
            "hdr@192.168.1.153:2222", "22", 22);

        assertEquals("hdr@192.168.1.153", target.host);
        assertEquals(2222, target.port);
    }

    @Test
    public void normalizesCommonSshCommandPaste() {
        SshTargetParser.ParsedTarget target = SshTargetParser.parse(
            "ssh -p 22022 hdr@192.168.1.153", "22", 22);

        assertEquals("hdr@192.168.1.153", target.host);
        assertEquals(22022, target.port);
    }

    @Test
    public void normalizesSshUriPaste() {
        SshTargetParser.ParsedTarget target = SshTargetParser.parse(
            "ssh://hdr@192.168.1.153:22022/home/hdr/project", "", 22);

        assertEquals("hdr@192.168.1.153", target.host);
        assertEquals(22022, target.port);
    }

    @Test
    public void keepsIpv6WithoutTreatingAddressPartAsPort() {
        SshTargetParser.ParsedTarget target = SshTargetParser.parse(
            "hdr@2001:db8::8", "22", 22);

        assertEquals("hdr@2001:db8::8", target.host);
        assertEquals(22, target.port);
    }

    @Test
    public void rejectsCommandInjectionAndUnsupportedOptions() {
        assertNull(SshTargetParser.parse("ssh -oProxyCommand=evil hdr@host", "22", 22));
        assertNull(SshTargetParser.parse("ssh hdr@host whoami", "22", 22));
        assertNull(SshTargetParser.parse("hdr@host;whoami", "22", 22));
        assertNull(SshTargetParser.parse("hdr@host", "70000", 22));
    }
}
