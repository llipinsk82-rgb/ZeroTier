package net.kaaass.zerotierfix.ui.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.List;

/** Persists user-verified tuner mappings separately from ZeroTier networks. */
public final class TvGatewayStore {
    public static final String PREF_GATEWAYS = "blackserv_tv_gateway_profiles_v1";
    public static final String PREF_ACTIVE_TUNER = "blackserv_tv_active_tuner_id";

    private TvGatewayStore() {
    }

    public static List<TvGatewayProfile> load(Context context) {
        String saved = prefs(context).getString(PREF_GATEWAYS, null);
        if (saved == null) return defaults();
        List<TvGatewayProfile> profiles = TvGatewayCodec.decode(saved);
        List<TvGatewayProfile> updated = new ArrayList<>(profiles.size());
        boolean migrated = false;
        for (TvGatewayProfile profile : profiles) {
            if ("#23".equals(profile.getTunerId())
                    && "Tuner #23 — Polska".equals(profile.getDisplayName())
                    && "a581878f7d6c0187".equals(profile.getNetworkIdHex())
                    && "10.32.73.190".equals(profile.getGatewayIpv4())
                    && "91.227.218.90".equals(profile.getExpectedPublicIpv4())) {
                updated.add(defaults().get(2));
                migrated = true;
            } else {
                updated.add(profile);
            }
        }
        if (migrated) save(context, updated);
        return updated;
    }

    /** Verified profiles; ID23 was reconfigured on 2026-11-07. Do not mutate tuner firmware. */
    public static List<TvGatewayProfile> defaults() {
        return java.util.Arrays.asList(
            new TvGatewayProfile("#5", "Tuner #5 — Polska", "05b1a061a1000001", "192.168.18.1", "91.227.216.230"),
            new TvGatewayProfile("#9", "Tuner #9 — Polska", "c0edc64972000001", "192.168.100.1", "185.148.67.216"),
            new TvGatewayProfile("#23", "Tuner #23 — Polska", "2c1ee692a07a5d21", "192.168.111.1", "91.227.218.90"));
    }

    public static void save(Context context, List<TvGatewayProfile> gateways) {
        prefs(context).edit().putString(PREF_GATEWAYS, TvGatewayCodec.encode(gateways)).apply();
    }

    public static String getActiveTunerId(Context context) {
        return prefs(context).getString(PREF_ACTIVE_TUNER, null);
    }

    public static void setActiveTunerId(Context context, String tunerId) {
        SharedPreferences.Editor edit = prefs(context).edit();
        if (tunerId == null) {
            edit.remove(PREF_ACTIVE_TUNER);
        } else {
            edit.putString(PREF_ACTIVE_TUNER, tunerId);
        }
        edit.apply();
    }

    private static SharedPreferences prefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
