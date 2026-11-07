package net.kaaass.zerotierfix.ui.tv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import net.kaaass.zerotierfix.service.ZeroTierOneService;

/**
 * Restores only a user-approved, remembered TV tunnel after system startup.
 * The original ZeroTier VPN service and JNI are deliberately unchanged.
 */
public final class TvBootReceiver extends BroadcastReceiver {
    private static final String TAG = "BlackServTvBoot";

    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        if (!PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(TvNetworkSettings.BOOT, true)) return;
        String selected = TvGatewayStore.getActiveTunerId(context);
        if (selected == null || VpnService.prepare(context) != null) return;
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = manager == null ? null : manager.getActiveNetworkInfo();
        if (network == null || !network.isConnected()) {
            Log.i(TAG, "Offline during boot; waiting for a later network recovery");
            return;
        }
        for (TvGatewayProfile profile : TvGatewayStore.load(context)) {
            if (!profile.getTunerId().equals(selected)) continue;
            try {
                TvNetworkStore.ensure(context, profile);
                Intent start = new Intent(context, ZeroTierOneService.class);
                start.putExtra(ZeroTierOneService.ZT1_NETWORK_ID, profile.getNetworkId());
                ContextCompat.startForegroundService(context, start);
            } catch (RuntimeException failure) {
                Log.e(TAG, "Could not restore saved TV tunnel", failure);
            }
            return;
        }
    }
}
