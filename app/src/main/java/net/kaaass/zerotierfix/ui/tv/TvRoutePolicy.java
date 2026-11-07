package net.kaaass.zerotierfix.ui.tv;

import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * A tuner gateway configured only with IPv4 must not permit IPv6 fallthrough.
 * VpnService.Builder blocks a family without any address, route or DNS for it.
 */
public final class TvRoutePolicy {
    private TvRoutePolicy() {
    }

    public static boolean disableIpv6(boolean userDisabled, boolean selectedIpv4Gateway) {
        return userDisabled || selectedIpv4Gateway;
    }

    /** Keep the directly attached private LAN outside a full-tunnel VPN. */
    public static boolean bypassUnderlyingIpv4(InetAddress address, int prefix,
                                               boolean hasGateway) {
        return !hasGateway && prefix > 0 && address instanceof Inet4Address
                && address.isSiteLocalAddress();
    }
}
