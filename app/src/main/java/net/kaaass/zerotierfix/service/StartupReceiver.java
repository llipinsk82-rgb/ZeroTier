package net.kaaass.zerotierfix.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import net.kaaass.zerotierfix.ui.tv.TvGatewayStore;
import net.kaaass.zerotierfix.ui.tv.TvNetworkSettings;

/** Boot restores only a previously selected profile and previously granted VPN consent. */
public class StartupReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(TvNetworkSettings.BOOT, false)
                || VpnService.prepare(context) != null) return;
        try {
            String active = TvGatewayStore.getActiveTunerId(context);
            for (var profile : TvGatewayStore.load(context)) {
                if (!profile.getTunerId().equals(active)) continue;
                Intent start = new Intent(context, ZeroTierOneService.class);
                start.putExtra(ZeroTierOneService.ZT1_NETWORK_ID, profile.getNetworkId());
                ContextCompat.startForegroundService(context, start);
                return;
            }
        } catch (RuntimeException restricted) {
            // OEM background restrictions must not crash the receiver or fake a connection.
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putString("blackserv_tv_last_error", "Autostart niedostępny. Połącz z ekranu aplikacji.").apply();
        }
    }
}
