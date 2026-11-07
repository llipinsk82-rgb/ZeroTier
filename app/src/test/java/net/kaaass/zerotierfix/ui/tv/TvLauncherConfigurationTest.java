package net.kaaass.zerotierfix.ui.tv;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.preference.PreferenceManager;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TvLauncherConfigurationTest {
    @Test public void defaultsContainCurrentTuner23Profile() {
        TvGatewayProfile tuner23 = null;
        for (TvGatewayProfile profile : TvGatewayStore.defaults()) {
            if ("#23".equals(profile.getTunerId())) tuner23 = profile;
        }

        assertNotNull("Tuner #23 must be visible after a clean install", tuner23);
        assertEquals("2c1ee692a07a5d21", tuner23.getNetworkIdHex());
        assertEquals("192.168.111.1", tuner23.getGatewayIpv4());
        assertEquals("91.227.218.90", tuner23.getExpectedPublicIpv4());
    }

    @Test public void applicationPublishesTvBannerForGoogleTvLauncher() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        ApplicationInfo app = context.getPackageManager()
            .getApplicationInfo(context.getPackageName(), 0);

        assertNotEquals("The application-level TV banner must not be missing", 0, app.banner);
    }

    @Test public void upgradesOnlyTheExactObsoleteTuner23Preset() {
        Context context = RuntimeEnvironment.getApplication();
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        TvGatewayProfile previous = new TvGatewayProfile("#23", "Tuner #23 — Polska",
                "a581878f7d6c0187", "10.32.73.190", "91.227.218.90");
        TvGatewayProfile personal = new TvGatewayProfile("#9", "Mój własny profil",
                "abcdefabcdefabcd", "10.10.10.10", null);
        TvGatewayStore.save(context, Arrays.asList(previous, personal));
        List<TvGatewayProfile> loaded = TvGatewayStore.load(context);
        assertEquals("2c1ee692a07a5d21", loaded.get(0).getNetworkIdHex());
        assertEquals("192.168.111.1", loaded.get(0).getGatewayIpv4());
        assertEquals("abcdefabcdefabcd", loaded.get(1).getNetworkIdHex());
        assertEquals("Mój własny profil", loaded.get(1).getDisplayName());
    }

    @Test public void preservesUserEditedTuner23Routing() {
        Context context = RuntimeEnvironment.getApplication();
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        TvGatewayStore.save(context, Arrays.asList(
                new TvGatewayProfile("#23", "Mój tuner",
                        "a581878f7d6c0187", "10.32.73.191", "91.227.218.90")));
        assertEquals("10.32.73.191", TvGatewayStore.load(context).get(0).getGatewayIpv4());
    }
}
