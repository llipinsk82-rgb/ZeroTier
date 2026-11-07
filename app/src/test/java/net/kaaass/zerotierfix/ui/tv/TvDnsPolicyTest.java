package net.kaaass.zerotierfix.ui.tv;

import org.junit.Test;
import static org.junit.Assert.*;

public class TvDnsPolicyTest {
    @Test public void verifiedTunersHaveIndependentLocalDns() {
        assertEquals("192.168.18.5", TvDnsPolicy.defaultDns("#5"));
        assertEquals("192.168.100.153", TvDnsPolicy.defaultDns("#9"));
        assertNull(TvDnsPolicy.defaultDns("#23"));
    }

    @Test public void unknownTunersNeverFallBackToPublicResolver() {
        assertNull(TvDnsPolicy.defaultDns("unknown"));
    }

    @Test public void dnsSettingsMustBeLiteralIp() {
        assertEquals("192.168.18.5", TvDnsPolicy.validate(" 192.168.18.5 "));
        try {
            TvDnsPolicy.validate("dns.google");
            fail("DNS must not be resolved through UK ISP");
        } catch (IllegalArgumentException expected) { }
    }
}
