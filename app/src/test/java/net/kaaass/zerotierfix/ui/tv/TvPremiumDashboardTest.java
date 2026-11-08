package net.kaaass.zerotierfix.ui.tv;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TvPremiumDashboardTest {
    @Test public void premiumTvSurfaceUsesTwoColumnsAndRemoteFocusableTunerRows() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        TvPremiumDashboard dashboard = new TvPremiumDashboard(activity,
                () -> {}, () -> {}, () -> {}, () -> {});
        LinearLayout root = (LinearLayout) dashboard.view();
        assertEquals(LinearLayout.VERTICAL, root.getOrientation());
        assertEquals(3, root.getChildCount()); // header, dashboard body, remote hint
        LinearLayout body = (LinearLayout) root.getChildAt(1);
        assertEquals(LinearLayout.HORIZONTAL, body.getOrientation());
        assertEquals(2, body.getChildCount());

        TvGatewayProfile tuner = new TvGatewayProfile("#5", "Tuner #5", "0123456789abcdef",
                "192.168.18.5", "1.2.3.4");
        View row = dashboard.card(tuner, TvConnectionState.Status.OFF, false,
                "Polska", () -> {}, () -> {});
        assertTrue(row.isFocusable());
        assertEquals("#5", row.getTag());
        assertTrue(row.getContentDescription().toString().contains("Tuner #5"));
    }

    @Test public void networkJoinAloneNeverMakesSessionAppearVerified() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        TvPremiumDashboard dashboard = new TvPremiumDashboard(activity,
                () -> {}, () -> {}, () -> {}, () -> {});
        TvGatewayProfile tuner = new TvGatewayProfile("#23", "Darek", "0123456789abcdef",
                "192.168.18.23", "1.2.3.4");
        dashboard.showSession(tuner, TvConnectionState.Status.VERIFYING_EXIT,
                "192.168.18.247", null, "Automatyczny · ZeroTier");
        assertFalse(flattenText(dashboard.view()).contains("AKTYWNY"));
        assertTrue(flattenText(dashboard.view()).contains("SPRAWDZA"));
    }

    private String flattenText(View view) {
        if (view instanceof android.widget.TextView) {
            return ((android.widget.TextView) view).getText().toString();
        }
        if (view instanceof android.view.ViewGroup) {
            StringBuilder text = new StringBuilder();
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                text.append(flattenText(group.getChildAt(i))).append(' ');
            }
            return text.toString();
        }
        return "";
    }
}
