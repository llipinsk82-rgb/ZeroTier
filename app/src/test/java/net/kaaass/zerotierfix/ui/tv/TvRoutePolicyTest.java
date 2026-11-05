package net.kaaass.zerotierfix.ui.tv;

import org.junit.Test;
import static org.junit.Assert.*;

public class TvRoutePolicyTest {
    @Test public void explicitIpv4GatewayMustDisableIpv6() {
        assertTrue(TvRoutePolicy.disableIpv6(false, true));
    }

    @Test public void preservesExistingUserPreferenceWithoutGatewayOverride() {
        assertFalse(TvRoutePolicy.disableIpv6(false, false));
        assertTrue(TvRoutePolicy.disableIpv6(true, false));
    }
}
