package net.kaaass.zerotierfix.service;

import android.app.Notification;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ZeroTierForegroundServiceTest {
    @Test public void promotesServiceBeforeReturningForMissingIntent() {
        ZeroTierOneService service = Robolectric.buildService(ZeroTierOneService.class).create().get();
        service.onStartCommand(null, 0, 1);
        Notification notification = Shadows.shadowOf(service).getLastForegroundNotification();
        assertNotNull("startForegroundService requires immediate foreground notification", notification);
    }
}
