package net.kaaass.zerotierfix.ui.tv;

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
}
