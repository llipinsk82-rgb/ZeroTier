package net.kaaass.zerotierfix.ui.tv;

import android.content.Context;
import android.content.pm.ApplicationInfo;
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
}
