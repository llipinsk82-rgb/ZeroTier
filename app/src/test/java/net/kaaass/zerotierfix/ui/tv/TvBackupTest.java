package net.kaaass.zerotierfix.ui.tv;
import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;
public class TvBackupTest {
    private String valid() throws Exception {
        return TvBackup.encode(Collections.singletonList(new TvGatewayProfile("#5", "Zażółć — Polska", "0123456789abcdef", "10.1.1.1", "91.1.2.3")),
            "1.1.1.1,1.0.0.1", "com.example.player", true);
    }
    @Test public void profilesUseControllerNextHopsNotTunersLanAddresses() {
        var p = TvGatewayStore.defaults();
        assertEquals("192.168.18.1", p.get(0).getGatewayIpv4());
        assertEquals("192.168.100.1", p.get(1).getGatewayIpv4());
        assertEquals("05b1a061a1000001", p.get(0).getNetworkIdHex());
        assertEquals("c0edc64972000001", p.get(1).getNetworkIdHex());
        assertEquals("a581878f7d6c0187", p.get(2).getNetworkIdHex());
        assertEquals("10.32.73.190", p.get(2).getGatewayIpv4());
    }
    @Test public void roundTripPreservesProfilesDnsAndExclusions() throws Exception {
        var s = TvBackup.decode(valid());
        assertEquals("Zażółć — Polska", s.profiles.get(0).getDisplayName());
        assertEquals("10.1.1.1", s.profiles.get(0).getGatewayIpv4());
        assertEquals("91.1.2.3", s.profiles.get(0).getExpectedPublicIpv4());
        assertEquals("1.1.1.1,1.0.0.1", s.dns);
        assertEquals("com.example.player", s.exclusions); assertTrue(s.boot);
    }
    @Test public void backupRoundTripKeepsIndependentTunerDns() throws Exception {
        org.json.JSONObject backup = new org.json.JSONObject(valid());
        backup.put("dnsByTuner", new org.json.JSONObject()
                .put("#5", "192.168.18.5"));
        var restored = TvBackup.decode(backup.toString());
        assertEquals("192.168.18.5", restored.dnsByTuner.get("#5"));
        assertFalse(restored.dnsByTuner.containsKey("#9"));
    }

    @Test public void emptySelectionRemainsEmpty() throws Exception {
        var s = TvBackup.decode(TvBackup.encode(Collections.emptyList(), "1.1.1.1", "", false));
        assertTrue(s.profiles.isEmpty()); assertEquals("", s.exclusions); assertFalse(s.boot);
    }
    @Test public void backupHasNoIdentityOrSession() throws Exception {
        assertFalse(valid().contains("identity")); assertFalse(valid().contains("activeTuner"));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsUnknownVersion() throws Exception {
        TvBackup.decode(valid().replace("\"version\": 1", "\"version\": 99"));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsInvalidGateway() throws Exception {
        TvBackup.decode(valid().replace("10.1.1.1", "999.1.1.1"));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsDnsHostname() throws Exception {
        TvNetworkSettings.parseDns("example.com");
    }
    @Test public void acceptsIpv6DnsAndDeduplicates() {
        assertEquals(2, TvNetworkSettings.parseDns("1.1.1.1,2606:4700:4700::1111,1.1.1.1").size());
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsTooLargeBackup() throws Exception {
        TvBackup.decode(new String(new char[TvBackup.MAX_BYTES+1]));
    }
}
