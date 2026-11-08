package net.kaaass.zerotierfix.ui.tv;

import org.apache.commons.validator.routines.InetAddressValidator;

/** DNS selection is profile-scoped; never silently use a public resolver. */
public final class TvDnsPolicy {
    public static final String AUTO = "auto";
    private TvDnsPolicy() { }

    /** Public resolvers are opt-in, never a fallback when network DNS is absent. */
    public static String providerDns(String provider) {
        if ("cloudflare".equals(provider)) return "1.1.1.1";
        if ("google".equals(provider)) return "8.8.8.8";
        if ("orange".equals(provider)) return "194.204.152.34";
        return null;
    }

    public static String defaultDns(String tunerId) {
        if ("#5".equals(tunerId)) return "192.168.18.5";
        if ("#9".equals(tunerId)) return "192.168.100.153";
        if ("#23".equals(tunerId)) return "194.204.152.34";
        return null;
    }

    public static String validate(String raw) {
        if (raw == null) throw new IllegalArgumentException("DNS is required");
        String ip = raw.trim();
        if (!InetAddressValidator.getInstance().isValid(ip)) {
            throw new IllegalArgumentException("DNS must be a literal IPv4 or IPv6 address");
        }
        return ip;
    }
}
