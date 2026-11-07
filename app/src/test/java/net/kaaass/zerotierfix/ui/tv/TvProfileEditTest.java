package net.kaaass.zerotierfix.ui.tv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.ArrayList;
import java.util.List;

/** Edit dialog regression: editing an existing tuner must never create a clone. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TvProfileEditTest {
    private ActivityController<TvHomeActivity> controller;
    private Context context;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        controller = Robolectric.buildActivity(TvHomeActivity.class).setup();
    }

    @After public void tearDown() {
        if (controller != null) {
            controller.pause().stop().destroy();
            controller = null;
        }
    }

    private void edit(TvGatewayProfile original, String newId, String newName) throws Exception {
        java.lang.reflect.Method method = TvHomeActivity.class
                .getDeclaredMethod("showProfileDialog", String.class, TvGatewayProfile.class);
        method.setAccessible(true);
        method.invoke(controller.get(), original.getTunerId(), original);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        List<EditText> fields = new ArrayList<>();
        collect(dialog.getWindow().getDecorView(), fields);
        fields.get(0).setText(newId);
        fields.get(1).setText(newName);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    }

    @Test public void changingIdOfExistingProfileDoesNotCreateClone() throws Exception {
        TvGatewayProfile original = TvGatewayStore.load(context).get(0);
        edit(original, "#5-nowy", "Nowa nazwa");
        List<TvGatewayProfile> saved = TvGatewayStore.load(context);
        assertEquals(3, saved.size());
        assertEquals("#5-nowy", saved.get(0).getTunerId());
        assertEquals("Nowa nazwa", saved.get(0).getDisplayName());
        for (TvGatewayProfile profile : saved) {
            assertNotEquals("#5", profile.getTunerId());
        }
    }

    @Test public void changingOnlyNameKeepsExactlyThreeProfiles() throws Exception {
        TvGatewayProfile original = TvGatewayStore.load(context).get(0);
        edit(original, original.getTunerId(), "Inna nazwa");
        List<TvGatewayProfile> saved = TvGatewayStore.load(context);
        assertEquals(3, saved.size());
        assertEquals("Inna nazwa", saved.get(0).getDisplayName());
    }

    @Test public void takingIdOfAnotherProfileIsRejectedAndNothingChanges() throws Exception {
        TvGatewayProfile original = TvGatewayStore.load(context).get(0);
        edit(original, "#9", "Kolizja");
        List<TvGatewayProfile> saved = TvGatewayStore.load(context);
        assertEquals(3, saved.size());
        assertEquals("#5", saved.get(0).getTunerId());
        assertEquals("#9", saved.get(1).getTunerId());
    }

    private static void collect(View view, List<EditText> fields) {
        if (view instanceof EditText) fields.add((EditText) view);
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                collect(((ViewGroup) view).getChildAt(i), fields);
            }
        }
    }
}
