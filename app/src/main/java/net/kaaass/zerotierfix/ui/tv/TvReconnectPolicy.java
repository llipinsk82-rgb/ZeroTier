package net.kaaass.zerotierfix.ui.tv;

/** Backoff without a tight retry loop or VPN attempts before user approval. */
public final class TvReconnectPolicy {
    private static final long[] RETRY_MS = {2000, 5000, 10000, 30000, 60000, 120000, 300000};
    private TvReconnectPolicy() { }

    public static long delayMs(int failures) {
        return RETRY_MS[Math.min(Math.max(0, failures), RETRY_MS.length - 1)];
    }

    public static boolean canReconnect(boolean selected, boolean connectivity, boolean vpnApproved) {
        return selected && connectivity && vpnApproved;
    }
}
