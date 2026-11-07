package net.kaaass.zerotierfix.ui.tv;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class TvGatewaySelectionTest {
    private final TvGatewayProfile romek =
            new TvGatewayProfile("u39.romek", "Romek", "0123456789abcdef", "10.147.17.5", null);
    private final TvGatewayProfile mama =
            new TvGatewayProfile("u23.mama", "Mama", "0123456789abcdef", "10.147.17.8", null);

    @Test public void selectsExactTunerDespiteSharedNetwork() {
        assertSame(mama, TvGatewaySelection.find(Arrays.asList(romek, mama),
                "u23.mama", romek.getNetworkId()));
    }
    @Test public void rejectsStaleSelectionForOtherNetwork() {
        assertNull(TvGatewaySelection.find(Arrays.asList(romek, mama),
                "u23.mama", 0x1111111111111111L));
    }
    @Test public void rejectsAbsentTuner() {
        assertNull(TvGatewaySelection.find(Collections.singletonList(romek),
                "unknown", romek.getNetworkId()));
    }
    @Test public void noSelectionMeansNoRouteOverride() {
        assertNull(TvGatewaySelection.find(Collections.singletonList(romek),
                null, romek.getNetworkId()));
    }
}
