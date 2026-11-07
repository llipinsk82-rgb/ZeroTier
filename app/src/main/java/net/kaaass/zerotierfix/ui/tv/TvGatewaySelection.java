package net.kaaass.zerotierfix.ui.tv;

import java.util.Collection;

/** Prevents a stale tuner preference from redirecting the wrong network. */
public final class TvGatewaySelection {
    private TvGatewaySelection() {
    }

    public static TvGatewayProfile find(Collection<TvGatewayProfile> profiles,
                                        String tunerId, long networkId) {
        if (tunerId == null || tunerId.isEmpty()) {
            return null;
        }
        for (TvGatewayProfile profile : profiles) {
            if (tunerId.equals(profile.getTunerId())
                    && networkId == profile.getNetworkId()) {
                return profile;
            }
        }
        return null;
    }
}
