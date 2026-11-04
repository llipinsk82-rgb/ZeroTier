package net.kaaass.zerotierfix.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Persisted package-level exclusions for the Android VPN tunnel.
 *
 * A null preference uses defaults. An empty preference is an intentional
 * choice to route all installed applications through the VPN.
 */
public final class VpnBypassConfig {
    public static final String PREF_KEY = "blackserv_vpn_bypass_packages";
    private static final String DEFAULT_PACKAGES =
            "";
    private static final Pattern PACKAGE_NAME = Pattern.compile(
            "[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)+");

    private VpnBypassConfig() {
    }

    public static List<String> parse(String saved) {
        String raw = saved == null ? DEFAULT_PACKAGES : saved;
        Set<String> result = new LinkedHashSet<>();
        for (String element : raw.split(",", -1)) {
            String name = element.trim();
            if (PACKAGE_NAME.matcher(name).matches()) {
                result.add(name);
            }
        }
        return new ArrayList<>(result);
    }

    public static String serialize(Collection<String> packages) {
        if (packages == null || packages.isEmpty()) {
            return "";
        }
        Set<String> result = new LinkedHashSet<>();
        for (String element : packages) {
            if (element != null) {
                String name = element.trim();
                if (PACKAGE_NAME.matcher(name).matches()) {
                    result.add(name);
                }
            }
        }
        return String.join(",", result);
    }
}
