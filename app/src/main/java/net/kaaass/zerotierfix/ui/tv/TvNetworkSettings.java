package net.kaaass.zerotierfix.ui.tv;

import android.content.Context;
import android.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.List;

public final class TvNetworkSettings {
    public static final String DNS = "blackserv_tv_dns";
    public static final String BOOT = "blackserv_tv_boot";
    private TvNetworkSettings() { }
    public static String dns(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getString(DNS, "1.1.1.1,1.0.0.1");
    }
    public static List<String> parseDns(String raw) {
        List<String> values = new ArrayList<>();
        for (String value : raw.split(",", -1)) {
            String ip = value.trim();
            if (!org.apache.commons.validator.routines.InetAddressValidator.getInstance().isValid(ip))
                throw new IllegalArgumentException("DNS musi być adresem IPv4 lub IPv6");
            if (!values.contains(ip)) values.add(ip);
        }
        if (values.isEmpty() || values.size() > 4) throw new IllegalArgumentException("Podaj 1–4 adresy DNS");
        return values;
    }
}
