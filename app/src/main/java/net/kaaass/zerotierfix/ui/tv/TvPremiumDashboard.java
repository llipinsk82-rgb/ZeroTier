package net.kaaass.zerotierfix.ui.tv;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import net.kaaass.zerotierfix.R;

/**
 * Native, remote-first 16:9 dashboard. VPN state remains owned by TvHomeActivity;
 * this class only creates and updates the visual surface.
 */
final class TvPremiumDashboard {
    private final Activity activity;
    final LinearLayout cards;
    final TextView summary;
    private final TextView sessionName;
    private final TextView sessionState;
    private final TextView networkValue;
    private final TextView localIpValue;
    private final TextView publicIpValue;
    private final TextView dnsValue;
    private final LinearLayout root;

    TvPremiumDashboard(Activity activity, Runnable settings, Runnable dns,
                       Runnable export, Runnable addProfile) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(color(R.color.tv_canvas));
        root.setPadding(dp(36), dp(24), dp(36), dp(20));

        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("BLACKSERV", 20, R.color.tv_text, true);
        title.setLetterSpacing(0.10f);
        header.addView(title, weight(1));
        TextView product = text("ZEROTIER  /  TV", 15, R.color.tv_focus, true);
        product.setLetterSpacing(0.06f);
        header.addView(product);
        root.addView(header);

        LinearLayout body = horizontal();
        LinearLayout.LayoutParams bodyParams = verticalWeight(1);
        bodyParams.topMargin = dp(22);
        bodyParams.bottomMargin = dp(16);
        root.addView(body, bodyParams);

        LinearLayout session = vertical();
        session.setPadding(dp(25), dp(22), dp(25), dp(21));
        session.setBackground(panelBackground());
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, -1, 54);
        leftParams.rightMargin = dp(16);
        body.addView(session, leftParams);

        TextView eyebrow = text("AKTYWNA SESJA", 13, R.color.tv_focus, true);
        eyebrow.setLetterSpacing(0.12f);
        session.addView(eyebrow);
        sessionName = text("Nie wybrano tunera", 30, R.color.tv_text, true);
        sessionName.setSingleLine(true);
        sessionName.setEllipsize(TextUtils.TruncateAt.END);
        session.addView(sessionName, marginTop(7));

        sessionState = text("●  WYŁĄCZONY", 15, R.color.tv_disconnected, true);
        session.addView(sessionState, marginTop(12));
        summary = text("Brak aktywnego połączenia", 15, R.color.tv_muted, false);
        summary.setMaxLines(3);
        summary.setEllipsize(TextUtils.TruncateAt.END);
        session.addView(summary, marginTop(10));

        View divider = new View(activity);
        divider.setBackgroundColor(0xFF30434A);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
        dividerParams.topMargin = dp(22);
        dividerParams.bottomMargin = dp(18);
        session.addView(divider, dividerParams);

        networkValue = infoRow(session, "SIEĆ", "—");
        localIpValue = infoRow(session, "IP ZEROTIER", "—");
        publicIpValue = infoRow(session, "PUBLICZNY IP", "—");
        dnsValue = infoRow(session, "DNS", "—");

        LinearLayout right = vertical();
        body.addView(right, new LinearLayout.LayoutParams(0, -1, 46));
        LinearLayout listHeader = horizontal();
        listHeader.setGravity(Gravity.CENTER_VERTICAL);
        listHeader.addView(text("TUNELE", 17, R.color.tv_text, true), weight(1));
        TextView hint = text("OK · połącz / rozłącz", 13, R.color.tv_muted, false);
        listHeader.addView(hint);
        right.addView(listHeader);

        ScrollView tunnelScroll = new ScrollView(activity);
        tunnelScroll.setFillViewport(true);
        tunnelScroll.setVerticalScrollBarEnabled(false);
        tunnelScroll.setClipToPadding(false);
        cards = vertical();
        cards.setPadding(dp(3), dp(8), dp(3), dp(5));
        tunnelScroll.addView(cards);
        right.addView(tunnelScroll, verticalWeight(1));

        LinearLayout controls = horizontal();
        controls.addView(button("DNS", dns), weight(1));
        LinearLayout.LayoutParams exportParams = weight(1);
        exportParams.leftMargin = dp(8);
        controls.addView(button("Eksport", export), exportParams);
        right.addView(controls, marginTop(6));

        LinearLayout moreControls = horizontal();
        moreControls.addView(button("+ Tuner", addProfile), weight(1));
        LinearLayout.LayoutParams settingsParams = weight(1);
        settingsParams.leftMargin = dp(8);
        moreControls.addView(button("Ustawienia", settings), settingsParams);
        right.addView(moreControls, marginTop(8));

        TextView footer = text("STRZAŁKI  NAWIGACJA     ·     OK  WYBIERZ     ·     PRZYTRZYMAJ OK  USUŃ", 12,
                R.color.tv_disconnected, false);
        footer.setLetterSpacing(0.04f);
        root.addView(footer);
    }

    View view() { return root; }

    void showSession(TvGatewayProfile profile, TvConnectionState.Status status,
                     String vpnIp, String publicIp, String dns) {
        sessionName.setText(profile == null ? "Nie wybrano tunera" : profile.getDisplayName());
        sessionState.setText("●  " + statusLabel(status));
        sessionState.setTextColor(color(statusColor(status)));
        networkValue.setText(profile == null ? "—" : profile.getNetworkIdHex());
        localIpValue.setText(vpnIp == null || vpnIp.isEmpty() ? "—" : vpnIp);
        publicIpValue.setText(publicIp == null || publicIp.isEmpty() ? "—" : publicIp);
        dnsValue.setText(profile == null ? "—" : dns);
    }

    LinearLayout card(TvGatewayProfile profile, TvConnectionState.Status status,
                      boolean selected, String detail, Runnable onClick, Runnable onLongClick) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(17), dp(10), dp(17), dp(10));
        row.setMinimumHeight(dp(76));
        row.setFocusable(true);
        row.setClickable(true);
        row.setTag(profile.getTunerId());
        row.setContentDescription(profile.getTunerId() + ", " + profile.getDisplayName()
                + ", " + statusLabel(status));

        LinearLayout description = vertical();
        TextView name = text(profile.getTunerId() + "  ·  " + profile.getDisplayName(), 18, R.color.tv_text, true);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        description.addView(name);
        TextView details = text(detail, 13, R.color.tv_muted, false);
        details.setSingleLine(true);
        details.setEllipsize(TextUtils.TruncateAt.END);
        description.addView(details, marginTop(6));
        row.addView(description, weight(1));
        TextView state = text(statusLabel(status), 12, statusColor(status), true);
        state.setPadding(dp(8), 0, 0, 0);
        row.addView(state);
        row.setBackground(cardBackground(status, selected, false));
        row.setOnFocusChangeListener((v, focused) ->
                row.setBackground(cardBackground(status, selected, focused)));
        row.setOnClickListener(v -> onClick.run());
        row.setOnLongClickListener(v -> { onLongClick.run(); return true; });
        return row;
    }

    private TextView infoRow(LinearLayout parent, String label, String initial) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(label, 12, R.color.tv_muted, true), weight(1));
        TextView value = text(initial, 15, R.color.tv_text, true);
        value.setSingleLine(true);
        value.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(value);
        parent.addView(row, marginTop(13));
        return value;
    }

    private TextView button(String label, Runnable click) {
        TextView button = text(label, 15, R.color.tv_text, true);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(41));
        button.setFocusable(true);
        button.setClickable(true);
        button.setBackground(cardBackground(TvConnectionState.Status.OFF, false, false));
        button.setOnFocusChangeListener((v, focus) -> button.setBackground(
                cardBackground(TvConnectionState.Status.OFF, false, focus)));
        button.setOnClickListener(v -> click.run());
        return button;
    }

    private GradientDrawable panelBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color(R.color.tv_surface));
        background.setCornerRadius(dp(19));
        background.setStroke(dp(1), 0xFF2E4148);
        return background;
    }

    private GradientDrawable cardBackground(TvConnectionState.Status status,
                                            boolean selected, boolean focused) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(focused ? 0xFF24414D : selected ? 0xFF1B3540 : 0xFF14232C);
        background.setCornerRadius(dp(15));
        background.setStroke(dp(focused ? 3 : 1),
                focused ? color(R.color.tv_focus) : selected ? color(statusColor(status)) : 0xFF30444D);
        return background;
    }

    private static String statusLabel(TvConnectionState.Status status) {
        switch (status) {
            case ON: return "AKTYWNY";
            case CONNECTING: return "ŁĄCZY";
            case VERIFYING_EXIT: return "SPRAWDZA";
            case ERROR: return "BŁĄD";
            default: return "OFF";
        }
    }

    private static int statusColor(TvConnectionState.Status status) {
        switch (status) {
            case ON: return R.color.tv_connected;
            case CONNECTING: case VERIFYING_EXIT: return R.color.tv_pending;
            case ERROR: return R.color.tv_error;
            default: return R.color.tv_disconnected;
        }
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(activity);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(activity);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView v = new TextView(activity);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color(color));
        v.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private LinearLayout.LayoutParams weight(float value) {
        return new LinearLayout.LayoutParams(0, -2, value);
    }

    private LinearLayout.LayoutParams verticalWeight(float value) {
        return new LinearLayout.LayoutParams(-1, 0, value);
    }

    private LinearLayout.LayoutParams marginTop(int margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(margin);
        return p;
    }

    private int dp(int value) {
        return Math.round(activity.getResources().getDisplayMetrics().density * value);
    }

    private int color(int res) { return ContextCompat.getColor(activity, res); }
}
