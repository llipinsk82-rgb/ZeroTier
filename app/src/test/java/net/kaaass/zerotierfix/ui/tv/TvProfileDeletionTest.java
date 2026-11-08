package net.kaaass.zerotierfix.ui.tv;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import net.kaaass.zerotierfix.events.NodeDestroyedEvent;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Real activity rendering, dialogs and SharedPreferences; never starts a VPN. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TvProfileDeletionTest {
    private ActivityController<TvHomeActivity> controller;
    private Context context;
    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        reopen();
    }
    @After public void tearDown() { close(); }
    private void close() { if (controller != null) { controller.pause().stop().destroy(); controller = null; } }
    private void reopen() { close(); controller = Robolectric.buildActivity(TvHomeActivity.class).setup(); }
    private View card(String id) { return controller.get().getWindow().getDecorView().findViewWithTag(id); }
    private void delete(String id) {
        assertNotNull(card(id));
        card(id).performLongClick();
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    }
    @Test public void deletedCardIsAbsentImmediatelyAndAfterActivityRestart() {
        delete("#5");
        assertNull("Deleted profile must not leave a placeholder card", card("#5"));
        assertNotNull(card("#9"));
        reopen();
        assertNull(card("#5"));
        assertEquals(2, TvGatewayStore.load(context).size());
    }
    @Test public void deletingAllPersistsEmptyInsteadOfReseedingDefaults() {
        delete("#5"); delete("#9"); delete("#23");
        assertTrue(TvGatewayStore.load(context).isEmpty());
        // Force queued apply() writes and verify the explicit empty-list marker is retained.
        assertTrue(PreferenceManager.getDefaultSharedPreferences(context).edit().commit());
        String persisted = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(TvGatewayStore.PREF_GATEWAYS, null);
        assertNotNull("Empty list must be stored on disk rather than removing the preference", persisted);
        assertTrue(TvGatewayCodec.decode(persisted).isEmpty());
        reopen();
        assertNull(card("#5")); assertNull(card("#9")); assertNull(card("#23"));
        assertTrue(TvGatewayStore.load(context).isEmpty());
    }
    @Test public void deletingProfileDoesNotRemoveLocalNetworkMembership() {
        TvGatewayProfile p = TvGatewayStore.load(context).get(0);
        TvNetworkStore.ensure(context, p);
        delete("#5");
        net.kaaass.zerotierfix.ZerotierFixApplication app =
            (net.kaaass.zerotierfix.ZerotierFixApplication) context;
        assertNotNull(app.getDaoSession().getNetworkDao().load(p.getNetworkId()));
    }
    @Test public void deletingProfileRemovesItsDnsOverride() {
        var prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("blackserv_tv_dns_profile_#5", "8.8.8.8").commit();
        delete("#5");
        assertFalse("Re-added tuner must not inherit a removed profile's resolver",
                prefs.contains("blackserv_tv_dns_profile_#5"));
    }
    @Test public void readdingSameTunerViaDialogProducesExactlyOneCard() throws Exception {
        TvGatewayProfile p = TvGatewayStore.load(context).get(0);
        delete("#5");
        java.lang.reflect.Method add = TvHomeActivity.class.getDeclaredMethod("showProfileDialog", String.class, TvGatewayProfile.class);
        add.setAccessible(true);
        for (int attempt=0; attempt<2; attempt++) {
            add.invoke(controller.get(), "#5", null);
            AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
            List<EditText> fields = new ArrayList<>(); collect(dialog.getWindow().getDecorView(), fields);
            String[] values = {p.getTunerId(), p.getDisplayName(), p.getNetworkIdHex(), p.getGatewayIpv4(), p.getExpectedPublicIpv4()};
            assertEquals(values.length, fields.size());
            for (int i=0;i<values.length;i++) fields.get(i).setText(values[i]);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        }
        reopen();
        assertEquals(3, TvGatewayStore.load(context).size());
        assertEquals(1, count(controller.get().getWindow().getDecorView(), "#5"));
    }
    @Test public void switchingWaitsForOldNodeDestructionBeforeStartingNewProfile() throws Exception {
        TvGatewayStore.setActiveTunerId(context, "#9");
        java.lang.reflect.Field running = TvHomeActivity.class.getDeclaredField("serviceRunning");
        running.setAccessible(true);
        running.setBoolean(controller.get(), true);
        reopen();
        running.setBoolean(controller.get(), true);

        card("#5").performClick();
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
            .idleFor(600, TimeUnit.MILLISECONDS);

        assertNull("A fixed delay must not start the replacement while the old node may still exist",
            TvGatewayStore.getActiveTunerId(context));

        controller.get().onNodeDestroyed(new NodeDestroyedEvent());
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        String started = TvGatewayStore.getActiveTunerId(context);
        if (started == null) {
            java.lang.reflect.Field consent = TvHomeActivity.class.getDeclaredField("pendingConsent");
            consent.setAccessible(true);
            TvGatewayProfile requested = (TvGatewayProfile) consent.get(controller.get());
            assertNotNull("Replacement must request VPN consent when Robolectric has not granted it", requested);
            started = requested.getTunerId();
        }
        assertEquals("#5", started);
    }
    @Test public void staleSelectedProfileConnectsWithOneClickWhenServiceIsStopped() throws Exception {
        TvGatewayStore.setActiveTunerId(context, "#5");
        reopen();

        card("#5").performClick();
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        String started = TvGatewayStore.getActiveTunerId(context);
        if (started == null) {
            java.lang.reflect.Field consent = TvHomeActivity.class.getDeclaredField("pendingConsent");
            consent.setAccessible(true);
            TvGatewayProfile requested = (TvGatewayProfile) consent.get(controller.get());
            assertNotNull("One click must initiate the stale profile instead of only clearing it", requested);
            started = requested.getTunerId();
        }
        assertEquals("#5", started);
    }
    private static int count(View view, String tag) {
        int result = tag.equals(view.getTag()) ? 1 : 0;
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) result+=count(((ViewGroup)view).getChildAt(i),tag);
        return result;
    }
    private static void collect(View view, List<EditText> fields) {
        if(view instanceof EditText) fields.add((EditText)view);
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) collect(((ViewGroup)view).getChildAt(i),fields);
    }
}
