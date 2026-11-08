package net.kaaass.zerotierfix.ui.tv;

import org.junit.Test;
import static org.junit.Assert.*;

/** The selected tuner gets bounded reconnect attempts before another exit is used. */
public class TvAutomaticRecoveryTest {
    @Test public void noRecoveryBeforeRepeatedFailures() {
        assertFalse(TvReconnectPolicy.shouldRestartBeforeFailover(2, 0, 30000, 0));
    }

    @Test public void firstAndSecondRecoveryRetrySameTuner() {
        assertTrue(TvReconnectPolicy.shouldRestartBeforeFailover(3, 0, 30000, 0));
        assertTrue(TvReconnectPolicy.shouldRestartBeforeFailover(3, 1, 60000, 30000));
    }

    @Test public void retryIsThrottledAndEventuallyAllowsFailover() {
        assertFalse(TvReconnectPolicy.shouldRestartBeforeFailover(3, 1, 32000, 30000));
        assertFalse(TvReconnectPolicy.shouldRestartBeforeFailover(3, 2, 90000, 30000));
    }
}
