package net.kaaass.zerotierfix.ui.tv;

import android.content.Context;
import android.preference.PreferenceManager;
import net.kaaass.zerotierfix.service.VpnBypassConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local settings only. Deliberately excludes identity.secret, tokens, active sessions and network grants. */
public final class TvBackup {
    public static final int MAX_BYTES = 256 * 1024;
    public static final class Settings {
        public final List<TvGatewayProfile> profiles;
        public final String dns, exclusions;
        public final boolean boot;
        public final Map<String, String> dnsByTuner;
        Settings(List<TvGatewayProfile> p, String d, String e, boolean b, Map<String, String> dnsMap) {
            profiles = p; dns = d; exclusions = e; boot = b;
            dnsByTuner = dnsMap;
        }
    }
    private TvBackup() { }
    public static Settings decode(String text) throws JSONException {
        if (text.length() > MAX_BYTES) throw new IllegalArgumentException("Plik jest zbyt duży");
        JSONObject json = new JSONObject(text);
        if (!"blackserv-zerotier-tv".equals(json.getString("format")) || json.getInt("version") != 1)
            throw new IllegalArgumentException("Nieobsługiwany format backupu");
        JSONArray data = json.getJSONArray("profiles");
        if (data.length() > 100) throw new IllegalArgumentException("Zbyt wiele profili");
        List<TvGatewayProfile> profiles = new ArrayList<>();
        for (int i=0; i<data.length(); i++) {
            JSONObject p = data.getJSONObject(i);
            profiles.add(new TvGatewayProfile(p.getString("id"), p.getString("name"),
                p.getString("networkId"), p.getString("gatewayIpv4"), p.optString("expectedPublicIpv4", "")));
        }
        TvGatewayCodec.encode(profiles); // validate duplicate IDs before any mutation
        String dns = json.getString("dns");
        TvNetworkSettings.parseDns(dns);
        JSONArray packages = json.getJSONArray("excludedPackages");
        List<String> exclusions = new ArrayList<>();
        for (int i=0; i<packages.length(); i++) {
            String pkg = packages.getString(i);
            if (pkg.equals("uk.blackserv.zerotiertv") || VpnBypassConfig.parse(pkg).size() != 1 || !VpnBypassConfig.parse(pkg).get(0).equals(pkg))
                throw new IllegalArgumentException("Nieprawidłowy pakiet aplikacji");
            exclusions.add(pkg);
        }
        Map<String, String> dnsByTuner = new LinkedHashMap<>();
        JSONObject customDns = json.optJSONObject("dnsByTuner");
        if (customDns != null) {
            java.util.Iterator<String> keys = customDns.keys();
            while (keys.hasNext()) {
                String id = keys.next();
                if (id.trim().isEmpty()) throw new IllegalArgumentException("Empty tuner ID in DNS backup");
                dnsByTuner.put(id, TvDnsPolicy.validate(customDns.getString(id)));
            }
        }
        return new Settings(profiles, dns, VpnBypassConfig.serialize(exclusions),
                json.getBoolean("startOnBoot"), dnsByTuner);
    }
    public static String encode(List<TvGatewayProfile> profiles, String dns, String excluded, boolean boot)
            throws JSONException {
        JSONArray list = new JSONArray();
        for (TvGatewayProfile p : profiles) {
            JSONObject item = new JSONObject().put("id", p.getTunerId()).put("name", p.getDisplayName())
                .put("networkId", p.getNetworkIdHex()).put("gatewayIpv4", p.getGatewayIpv4())
                .put("expectedPublicIpv4", p.getExpectedPublicIpv4() == null ? "" : p.getExpectedPublicIpv4());
            list.put(item);
        }
        return new JSONObject().put("format", "blackserv-zerotier-tv").put("version", 1)
            .put("profiles", list).put("dns", dns).put("startOnBoot", boot)
            .put("excludedPackages", new JSONArray(VpnBypassConfig.parse(excluded))).toString(2);
    }
    public static String exportSettings(Context c) throws JSONException {
        var prefs = PreferenceManager.getDefaultSharedPreferences(c);
        List<TvGatewayProfile> profiles = TvGatewayStore.load(c);
        JSONObject json = new JSONObject(encode(profiles, TvNetworkSettings.dns(c),
            prefs.getString(VpnBypassConfig.PREF_KEY, null),
            prefs.getBoolean(TvNetworkSettings.BOOT, false)));
        JSONObject dns = new JSONObject();
        for (TvGatewayProfile profile : profiles) {
            String address = prefs.getString("blackserv_tv_dns_profile_" + profile.getTunerId(),
                    TvDnsPolicy.defaultDns(profile.getTunerId()));
            if (address != null && !address.trim().isEmpty()) {
                dns.put(profile.getTunerId(), TvDnsPolicy.validate(address));
            }
        }
        json.put("dnsByTuner", dns);
        return json.toString(2);
    }
    public static void restore(Context c, Settings settings) {
        var prefs = PreferenceManager.getDefaultSharedPreferences(c);
        var edit = prefs.edit()
            .putString(TvGatewayStore.PREF_GATEWAYS, TvGatewayCodec.encode(settings.profiles))
            .putString(TvNetworkSettings.DNS, settings.dns)
            .putString(VpnBypassConfig.PREF_KEY, settings.exclusions)
            .putBoolean(TvNetworkSettings.BOOT, settings.boot)
            .remove(TvGatewayStore.PREF_ACTIVE_TUNER);
        // Remove DNS for profiles absent from the incoming backup, not just their cards.
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith("blackserv_tv_dns_profile_")) edit.remove(key);
        }
        for (Map.Entry<String, String> dns : settings.dnsByTuner.entrySet()) {
            edit.putString("blackserv_tv_dns_profile_" + dns.getKey(), dns.getValue());
        }
        boolean saved = edit.commit();
        if (!saved) throw new IllegalStateException("Nie można zapisać ustawień");
    }
}
