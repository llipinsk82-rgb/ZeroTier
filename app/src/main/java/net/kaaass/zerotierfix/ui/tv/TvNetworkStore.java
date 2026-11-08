package net.kaaass.zerotierfix.ui.tv;

import android.content.Context;
import android.preference.PreferenceManager;
import net.kaaass.zerotierfix.ZerotierFixApplication;
import net.kaaass.zerotierfix.model.DnsServer;
import net.kaaass.zerotierfix.model.DnsServerDao;
import net.kaaass.zerotierfix.model.Network;
import net.kaaass.zerotierfix.model.NetworkConfig;
import net.kaaass.zerotierfix.util.DatabaseUtils;

/** Persist TV membership and DNS via the original ZeroTier application model. */
public final class TvNetworkStore {
    private TvNetworkStore() { }

    /** Returns a verified per-tuner resolver or null, never the legacy global DNS. */
    public static String dnsForTuner(Context context, String tunerId) {
        if (tunerId == null) return null;
        String configuredDns = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("blackserv_tv_dns_profile_" + tunerId, TvDnsPolicy.AUTO);
        if (configuredDns == null || configuredDns.trim().isEmpty()
                || TvDnsPolicy.AUTO.equalsIgnoreCase(configuredDns)) return null;
        return TvDnsPolicy.validate(configuredDns);
    }

    /** Auto delegates to ZeroTier controller DNS; manual entries never carry across tuners. */
    public static boolean usesControllerDns(Context context, String tunerId) {
        return dnsForTuner(context, tunerId) == null;
    }

    public static void ensure(Context context, TvGatewayProfile profile) {
        String dns = dnsForTuner(context, profile.getTunerId());
        boolean fromController = dns == null;
        var dao = ((ZerotierFixApplication) context.getApplicationContext()).getDaoSession();
        DatabaseUtils.writeLock.lock();
        try {
            dao.runInTx(() -> {
                NetworkConfig config = dao.getNetworkConfigDao().load(profile.getNetworkId());
                if (config == null) {
                    config = new NetworkConfig();
                    config.setId(profile.getNetworkId());
                }
                config.setRouteViaZeroTier(true);
                config.setDnsMode(fromController ? 1 : 2); // NETWORK_DNS / CUSTOM_DNS
                config.setUseCustomDNS(!fromController);
                dao.getNetworkConfigDao().insertOrReplace(config);

                dao.queryBuilder(DnsServer.class)
                        .where(DnsServerDao.Properties.NetworkId.eq(profile.getNetworkId()))
                        .buildDelete().executeDeleteWithoutDetachingEntities();
                if (!fromController) {
                    DnsServer server = new DnsServer();
                    server.setNetworkId(profile.getNetworkId());
                    server.setNameserver(dns);
                    dao.getDnsServerDao().insert(server);
                }

                Network network = dao.getNetworkDao().load(profile.getNetworkId());
                if (network == null) {
                    network = new Network();
                    network.setNetworkId(profile.getNetworkId());
                    network.setNetworkIdStr(profile.getNetworkIdHex());
                    network.setNetworkName(profile.getDisplayName());
                    network.setNetworkConfigId(profile.getNetworkId());
                    dao.getNetworkDao().insert(network);
                }
            });
        } finally {
            DatabaseUtils.writeLock.unlock();
        }
    }
}
