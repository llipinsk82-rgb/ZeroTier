package net.kaaass.zerotierfix.ui.tv;

import java.util.Locale;

/**
 * One identifiable tuner that can act as a ZeroTier exit gateway.
 * Several profiles may share a ZeroTier network without sharing an exit IP.
 */
public final class TvGatewayProfile {
    private final String tunerId;
    private final String displayName;
    private final String networkIdHex;
    private final String gatewayIpv4;
    private final String expectedPublicIpv4;

    public TvGatewayProfile(String tunerId, String displayName, String networkIdHex,
                            String gatewayIpv4, String expectedPublicIpv4) {
        this.tunerId = requireText(tunerId, "tunerId");
        this.displayName = requireText(displayName, "displayName");
        String hex = requireText(networkIdHex, "networkIdHex").toLowerCase(Locale.ROOT);
        if (!hex.matches("[0-9a-f]{16}")) {
            throw new IllegalArgumentException("ZeroTier network ID must be 16 hexadecimal digits");
        }
        this.networkIdHex = hex;
        this.gatewayIpv4 = validateIpv4(gatewayIpv4, "gatewayIpv4");
        this.expectedPublicIpv4 = expectedPublicIpv4 == null || expectedPublicIpv4.trim().isEmpty()
                ? null : validateIpv4(expectedPublicIpv4, "expectedPublicIpv4");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return value.trim();
    }

    private static String validateIpv4(String raw, String field) {
        String address = requireText(raw, field);
        if (!address.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) {
            throw new IllegalArgumentException(field + " must be a literal IPv4 address");
        }
        for (String octet : address.split("\\.")) {
            if (Integer.parseInt(octet) > 255) {
                throw new IllegalArgumentException(field + " contains invalid octet");
            }
        }
        return address;
    }

    public String getTunerId() { return tunerId; }
    public String getDisplayName() { return displayName; }
    public String getNetworkIdHex() { return networkIdHex; }
    public long getNetworkId() { return Long.parseUnsignedLong(networkIdHex, 16); }
    public String getGatewayIpv4() { return gatewayIpv4; }
    public String getExpectedPublicIpv4() { return expectedPublicIpv4; }
}
