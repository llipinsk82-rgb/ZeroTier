package net.kaaass.zerotierfix.ui.tv;

import org.junit.Test;
import java.net.InetAddress;
import static org.junit.Assert.*;

public class TvRoutePolicyTest {
    @Test public void explicitIpv4GatewayMustDisableIpv6() {
        assertTrue(TvRoutePolicy.disableIpv6(false, true));
    }

    @Test public void preservesExistingUserPreferenceWithoutGatewayOverride() {
        assertFalse(TvRoutePolicy.disableIpv6(false, false));
        assertTrue(TvRoutePolicy.disableIpv6(true, false));
    }

    @Test public void bypassesOnlyDirectPrivateUnderlyingLanRoute() throws Exception {
        assertTrue(TvRoutePolicy.bypassUnderlyingIpv4(
            InetAddress.getByName("192.168.1.0"), 24, false));
        assertFalse(TvRoutePolicy.bypassUnderlyingIpv4(
            InetAddress.getByName("0.0.0.0"), 0, true));
        assertFalse(TvRoutePolicy.bypassUnderlyingIpv4(
            InetAddress.getByName("8.8.8.0"), 24, false));
        assertFalse(TvRoutePolicy.bypassUnderlyingIpv4(
            InetAddress.getByName("192.168.1.0"), 24, true));
    }
}
