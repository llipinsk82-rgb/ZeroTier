package net.kaaass.zerotierfix.ui.tv;

import org.junit.Test;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

public class TvFailoverPolicyTest {
    private final List<TvGatewayProfile> profiles = Arrays.asList(
            new TvGatewayProfile("#5", "Five", "05b1a061a1000001", "192.168.18.1", "91.227.216.230"),
            new TvGatewayProfile("#9", "Nine", "c0edc64972000001", "192.168.100.1", "185.148.67.216"),
            new TvGatewayProfile("#23", "Twenty-three", "2c1ee692a07a5d21", "192.168.111.1", "91.227.218.90"));

    @Test public void needsRepeatedFailureAndCooldown() {
        assertFalse(TvFailoverPolicy.maySwitch(2, 180000, 0));
        assertFalse(TvFailoverPolicy.maySwitch(3, 10000, 0));
        assertTrue(TvFailoverPolicy.maySwitch(3, 180000, 0));
    }

    @Test public void triesTheBestKnownNextTunerWithoutCycling() {
        Set<String> failed = new HashSet<>(Arrays.asList("#5"));
        Map<String, Long> latency = new HashMap<>();
        latency.put("#9", 190L);
        latency.put("#23", 45L);
        assertEquals("#23", TvFailoverPolicy.next(profiles, "#5", failed, latency).getTunerId());
        failed.add("#23");
        assertEquals("#9", TvFailoverPolicy.next(profiles, "#5", failed, latency).getTunerId());
        failed.add("#9");
        assertNull(TvFailoverPolicy.next(profiles, "#5", failed, latency));
    }

    @Test public void noBlindSwitchToUnverifiedExit() {
        List<TvGatewayProfile> unverified = Arrays.asList(
                profiles.get(0),
                new TvGatewayProfile("unknown", "No exit", "abcdefabcdefabcd", "10.1.1.1", null));
        assertNull(TvFailoverPolicy.next(unverified, "#5", new HashSet<>(), new HashMap<>()));
    }
}
