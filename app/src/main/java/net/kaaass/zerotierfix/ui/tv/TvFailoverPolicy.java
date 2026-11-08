package net.kaaass.zerotierfix.ui.tv;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Bounded automatic choice between explicitly configured, verifiable exits. */
public final class TvFailoverPolicy {
    private static final long SWITCH_COOLDOWN_MS = 90_000;

    private TvFailoverPolicy() { }

    public static boolean maySwitch(int consecutiveFailures, long nowMs, long lastSwitchMs) {
        return consecutiveFailures >= 3 && nowMs - lastSwitchMs >= SWITCH_COOLDOWN_MS;
    }

    public static TvGatewayProfile next(List<TvGatewayProfile> profiles, String currentId,
                                        Set<String> attemptedIds, Map<String, Long> latencyMs) {
        TvGatewayProfile best = null;
        long bestLatency = Long.MAX_VALUE;
        for (TvGatewayProfile profile : profiles) {
            String id = profile.getTunerId();
            if (id.equals(currentId) || attemptedIds.contains(id)
                    || profile.getExpectedPublicIpv4() == null) continue;
            Long knownLatency = latencyMs.get(id);
            long duration = knownLatency != null && knownLatency >= 0
                    ? knownLatency : Long.MAX_VALUE;
            if (best == null || duration < bestLatency) {
                best = profile;
                bestLatency = duration;
            }
        }
        return best;
    }
}
