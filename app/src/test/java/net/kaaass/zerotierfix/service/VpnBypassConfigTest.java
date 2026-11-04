package net.kaaass.zerotierfix.service;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class VpnBypassConfigTest {
    @Test
    public void defaultsExcludePlayStoreAndPlanetaPlTv() {
        assertEquals(
                Arrays.asList("com.android.vending", "com.planetapl.tv.Player"),
                VpnBypassConfig.parse(null));
    }

    @Test
    public void emptySavedPreferenceMeansNoExclusions() {
        assertEquals(Collections.emptyList(), VpnBypassConfig.parse(""));
    }

    @Test
    public void removesDuplicatesWithoutReorderingUserChoices() {
        assertEquals(
                Arrays.asList("com.example.player", "com.planetapl.tv.Player"),
                VpnBypassConfig.parse(
                        " com.example.player,com.planetapl.tv.Player,com.example.player "));
    }

    @Test
    public void ignoresInvalidPackageNames() {
        assertEquals(
                Collections.singletonList("com.example.player"),
                VpnBypassConfig.parse("com.example.player,../bad,x, ,com..broken"));
    }

    @Test
    public void roundTripsSavedSelection() {
        assertEquals(
                Arrays.asList("com.one.app", "com.two.app"),
                VpnBypassConfig.parse(
                        VpnBypassConfig.serialize(
                                Arrays.asList("com.one.app", "com.two.app"))));
    }
}
