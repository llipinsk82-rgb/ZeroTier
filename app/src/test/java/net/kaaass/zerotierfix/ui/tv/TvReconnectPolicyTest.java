package net.kaaass.zerotierfix.ui.tv;

import org.junit.Test;
import static org.junit.Assert.*;

public class TvReconnectPolicyTest {
    @Test public void retryIsBoundedAndThrottled() {
        assertEquals(2000L, TvReconnectPolicy.delayMs(0));
        assertEquals(5000L, TvReconnectPolicy.delayMs(1));
        assertEquals(10000L, TvReconnectPolicy.delayMs(2));
        assertEquals(30000L, TvReconnectPolicy.delayMs(3));
        assertEquals(60000L, TvReconnectPolicy.delayMs(4));
        assertEquals(300000L, TvReconnectPolicy.delayMs(9));
    }

    @Test public void reconnectNeedsUserSelectionConnectivityAndApproval() {
        assertTrue(TvReconnectPolicy.canReconnect(true, true, true));
        assertFalse(TvReconnectPolicy.canReconnect(false, true, true));
        assertFalse(TvReconnectPolicy.canReconnect(true, false, true));
        assertFalse(TvReconnectPolicy.canReconnect(true, true, false));
    }
}
