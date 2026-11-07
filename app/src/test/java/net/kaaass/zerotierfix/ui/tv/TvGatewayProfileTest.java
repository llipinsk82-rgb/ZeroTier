package net.kaaass.zerotierfix.ui.tv;

import org.junit.Test;
import static org.junit.Assert.*;

public class TvGatewayProfileTest {
    @Test
    public void keepsTunerNameSeparateFromZeroTierNetworkId() {
        TvGatewayProfile profile = new TvGatewayProfile(
                "u39.romek", "Romek", "0123456789abcdef", "10.147.17.5", null);
        assertEquals("u39.romek", profile.getTunerId());
        assertEquals("Romek", profile.getDisplayName());
        assertEquals("0123456789abcdef", profile.getNetworkIdHex());
        assertEquals("10.147.17.5", profile.getGatewayIpv4());
        assertEquals(0x0123456789abcdefL, profile.getNetworkId());
    }

    @Test
    public void twoTunersCanUseOneZeroTierNetworkWithDifferentGateways() {
        TvGatewayProfile first = new TvGatewayProfile(
                "u39.romek", "Romek", "0123456789abcdef", "10.147.17.5", null);
        TvGatewayProfile second = new TvGatewayProfile(
                "u23.mama", "Mama", "0123456789abcdef", "10.147.17.8", null);
        assertEquals(first.getNetworkId(), second.getNetworkId());
        assertNotEquals(first.getGatewayIpv4(), second.getGatewayIpv4());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidNetworkId() {
        new TvGatewayProfile("u39.romek", "Romek", "network", "10.147.17.5", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidGatewayIp() {
        new TvGatewayProfile("u39.romek", "Romek", "0123456789abcdef", "127.0.0.999", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingIdentity() {
        new TvGatewayProfile(" ", "Romek", "0123456789abcdef", "10.147.17.5", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingDisplayName() {
        new TvGatewayProfile("u39.romek", "  ", "0123456789abcdef", "10.147.17.5", null);
    }

    @Test
    public void optionalPublicExitAddressIsValidatedIfPresent() {
        TvGatewayProfile profile = new TvGatewayProfile(
                "u39.romek", "Romek", "0123456789abcdef",
                "10.147.17.5", "83.10.20.30");
        assertEquals("83.10.20.30", profile.getExpectedPublicIpv4());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidPublicExitAddress() {
        new TvGatewayProfile("u39.romek", "Romek", "0123456789abcdef",
                "10.147.17.5", "example.com");
    }
}
