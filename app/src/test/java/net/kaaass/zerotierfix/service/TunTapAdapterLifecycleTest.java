package net.kaaass.zerotierfix.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TunTapAdapterLifecycleTest {
    @Test
    public void idleAdapterDoesNotStartArpOrNdpWorkers() throws Exception {
        int arpBefore = liveWorkers("ARP Timeout Thread");
        int ndpBefore = liveWorkers("NDP Timeout Thread");
        TunTapAdapter adapter = new TunTapAdapter(null, 123L);
        try {
            assertEquals("ARP worker should only start with the TUN receive loop",
                    arpBefore, liveWorkers("ARP Timeout Thread"));
            assertEquals("NDP worker should only start with the TUN receive loop",
                    ndpBefore, liveWorkers("NDP Timeout Thread"));
        } finally {
            adapter.interrupt();
            cleanupLegacyTable(adapter, "arpTable");
            cleanupLegacyTable(adapter, "ndpTable");
        }
    }

    @Test
    public void interruptWithoutTunLoopLeavesNoLookupWorkers() throws Exception {
        int arpBefore = liveWorkers("ARP Timeout Thread");
        int ndpBefore = liveWorkers("NDP Timeout Thread");
        TunTapAdapter adapter = new TunTapAdapter(null, 456L);
        try {
            adapter.interrupt();
            assertEquals("No orphaned ARP lookup thread", arpBefore, liveWorkers("ARP Timeout Thread"));
            assertEquals("No orphaned NDP lookup thread", ndpBefore, liveWorkers("NDP Timeout Thread"));
        } finally {
            cleanupLegacyTable(adapter, "arpTable");
            cleanupLegacyTable(adapter, "ndpTable");
        }
    }

    private static int liveWorkers(String name) {
        int count = 0;
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.isAlive() && name.equals(thread.getName())) {
                count++;
            }
        }
        return count;
    }

    // Release resources even while exercising the pre-fix implementation during RED.
    private static void cleanupLegacyTable(TunTapAdapter adapter, String fieldName) throws Exception {
        Field field = TunTapAdapter.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object table = field.get(adapter);
        if (table instanceof ARPTable) {
            ((ARPTable) table).stop();
        } else if (table instanceof NDPTable) {
            ((NDPTable) table).stop();
        }
    }
}
