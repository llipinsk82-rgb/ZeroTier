package net.kaaass.zerotierfix.ui.tv;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.service.VpnBypassConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.TreeMap;

/** D-pad friendly list of applications excluded from ZeroTier VPN. */
public final class TvBypassActivity extends AppCompatActivity {
    private final Set<String> excludedPackages = new LinkedHashSet<>();
    private final TreeMap<String, String> visiblePackages = new TreeMap<>();
    private LinearLayout list;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(color(R.color.tv_canvas));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(48), dp(40), dp(48), dp(48));
        scroller.addView(root);

        TextView heading = label("Aplikacje bez VPN", 30);
        root.addView(heading);
        TextView hint = label("Zaznaczone aplikacje korzystają z normalnego internetu. "
                + "Zmiany wymagają ponownego połączenia VPN.", 18);
        hint.setTextColor(color(R.color.tv_muted));
        root.addView(hint);

        TextView add = label("+ Dodaj nazwę pakietu", 20);
        add.setFocusable(true);
        add.setPadding(dp(16), dp(18), dp(16), dp(18));
        add.setOnClickListener(v -> showAddPackageDialog());
        add.setOnFocusChangeListener((v, focused) ->
                add.setBackground(background(focused)));
        root.addView(add);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
        setContentView(scroller);
        reload();
    }

    private void reload() {
        String saved = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(VpnBypassConfig.PREF_KEY, null);
        excludedPackages.clear();
        excludedPackages.addAll(VpnBypassConfig.parse(saved));

        visiblePackages.clear();
        PackageManager pm = getPackageManager();
        Intent tvApps = new Intent(Intent.ACTION_MAIN);
        tvApps.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo info : pm.queryIntentActivities(tvApps, 0)) {
            if (info.activityInfo != null && info.activityInfo.packageName != null) {
                visiblePackages.put(info.activityInfo.packageName,
                        info.loadLabel(pm).toString());
            }
        }
        Intent launcherApps = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo info : pm.queryIntentActivities(launcherApps, 0)) {
            if (info.activityInfo != null) visiblePackages.put(info.activityInfo.packageName, info.loadLabel(pm).toString());
        }
        visiblePackages.remove(getPackageName()); // VPN health probes must follow this app's tunnel.
        for (String pkg : excludedPackages) {
            if (!visiblePackages.containsKey(pkg)) {
                visiblePackages.put(pkg, pkg);
            }
        }
        render();
    }

    private void render() {
        Object focusedTag = getCurrentFocus() == null ? null : getCurrentFocus().getTag();
        list.removeAllViews();
        for (Map.Entry<String, String> app : visiblePackages.entrySet()) {
            String pkg = app.getKey();
            boolean isExcluded = excludedPackages.contains(pkg);
            TextView row = label((isExcluded ? "✓  " : "○  ")
                    + app.getValue() + "\n" + pkg, 20);
            row.setPadding(dp(18), dp(16), dp(18), dp(16));
            row.setTextColor(color(isExcluded ? R.color.tv_focus : R.color.tv_text));
            row.setTag(pkg);
            row.setFocusable(true);
            row.setClickable(true);
            row.setBackground(background(false));
            row.setOnFocusChangeListener((v, focused) ->
                    row.setBackground(background(focused)));
            row.setOnClickListener(v -> {
                if (!excludedPackages.add(pkg)) {
                    excludedPackages.remove(pkg);
                }
                save();
                render();
            });
            LinearLayout.LayoutParams margin = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            margin.topMargin = dp(10);
            list.addView(row, margin);
            if (pkg.equals(focusedTag)) row.requestFocus();
        }
    }

    private void showAddPackageDialog() {
        EditText value = new EditText(this);
        value.setHint("com.przyklad.aplikacja");
        value.setSingleLine(true);
        new AlertDialog.Builder(this)
                .setTitle("Dodaj aplikację poza VPN")
                .setView(value)
                .setNegativeButton("Anuluj", null)
                .setPositiveButton("Dodaj", (dialog, which) -> {
                    String pkg = value.getText().toString().trim();
                    if (!pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")) {
                        Toast.makeText(this, "Niepoprawna nazwa pakietu", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (pkg.equals(getPackageName())) {
                        Toast.makeText(this, "Klient VPN musi pozostać w tunelu dla diagnostyki", Toast.LENGTH_LONG).show();
                        return;
                    }
                    excludedPackages.add(pkg);
                    visiblePackages.put(pkg, pkg);
                    save();
                    render();
                }).show();
    }

    private void save() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putString(VpnBypassConfig.PREF_KEY,
                        VpnBypassConfig.serialize(excludedPackages))
                .apply();
        Toast.makeText(this, "Zapisano. Połącz VPN ponownie, aby zastosować.",
                Toast.LENGTH_SHORT).show();
    }

    private GradientDrawable background(boolean focused) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color(R.color.tv_surface));
        shape.setCornerRadius(dp(14));
        shape.setStroke(dp(focused ? 4 : 1),
                color(focused ? R.color.tv_focus : R.color.tv_disconnected));
        return shape;
    }

    private TextView label(String text, int sp) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color(R.color.tv_text));
        return view;
    }

    private int color(int resource) {
        return ContextCompat.getColor(this, resource);
    }

    private int dp(int raw) {
        return Math.round(getResources().getDisplayMetrics().density * raw);
    }
}
