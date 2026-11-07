package net.kaaass.zerotierfix.ui.tv;

import android.content.Context;
import net.kaaass.zerotierfix.ZerotierFixApplication;
import net.kaaass.zerotierfix.model.Network;
import net.kaaass.zerotierfix.model.NetworkConfig;
import net.kaaass.zerotierfix.util.DatabaseUtils;

/** A profile and its ZeroTier membership are one TV workflow. */
public final class TvNetworkStore {
    private TvNetworkStore() { }
    public static void ensure(Context context, TvGatewayProfile profile) {
        var dao = ((ZerotierFixApplication) context.getApplicationContext()).getDaoSession();
        DatabaseUtils.writeLock.lock();
        try {
            dao.runInTx(() -> {
                if (dao.getNetworkDao().load(profile.getNetworkId()) != null) return;
                NetworkConfig config = new NetworkConfig();
                config.setId(profile.getNetworkId());
                config.setRouteViaZeroTier(true);
                config.setDnsMode(1);
                dao.getNetworkConfigDao().insertOrReplace(config);
                Network network = new Network();
                network.setNetworkId(profile.getNetworkId());
                network.setNetworkIdStr(profile.getNetworkIdHex());
                network.setNetworkName(profile.getDisplayName());
                network.setNetworkConfigId(profile.getNetworkId());
                dao.getNetworkDao().insert(network);
            });
        } finally { DatabaseUtils.writeLock.unlock(); }
    }
}
