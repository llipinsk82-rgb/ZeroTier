package net.kaaass.zerotierfix.ui.tv;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TvConnectionStateTest {
    @Test
    public void anotherGatewayIsOffEvenIfOneNetworkIsActive() {
        assertEquals(TvConnectionState.Status.OFF,
                TvConnectionState.forTile(11, 12, true, true, false));
    }

    @Test
    public void selectedGatewayWithoutNetworkIsConnecting() {
        assertEquals(TvConnectionState.Status.CONNECTING,
                TvConnectionState.forTile(11, 11, false, false, false));
    }

    @Test
    public void zeroTierReadyStillWaitsForExitVerification() {
        assertEquals(TvConnectionState.Status.VERIFYING_EXIT,
                TvConnectionState.forTile(11, 11, true, false, false));
    }

    @Test
    public void confirmedExitShowsOn() {
        assertEquals(TvConnectionState.Status.ON,
                TvConnectionState.forTile(11, 11, true, true, false));
    }

    @Test
    public void reportedErrorTakesPrecedenceOverConnected() {
        assertEquals(TvConnectionState.Status.ERROR,
                TvConnectionState.forTile(11, 11, true, true, true));
    }

    @Test
    public void noSelectionIsOff() {
        assertEquals(TvConnectionState.Status.OFF,
                TvConnectionState.forTile(11, 0, false, false, false));
    }
}
