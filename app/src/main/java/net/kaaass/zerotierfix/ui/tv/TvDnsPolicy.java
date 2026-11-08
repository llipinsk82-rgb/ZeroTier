package net.kaaass.zerotierfix.ui.tv;

import org.apache.commons.validator.routines.InetAddressValidator;

/** DNS selection is profile-scoped; never silently use a public resolver. */
public final class TvDnsPolicy {
    private TvDnsPolicy() { }

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
