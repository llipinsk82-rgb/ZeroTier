package net.kaaass.zerotierfix.ui.tv;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.zerotier.sdk.VirtualNetworkConfig;
import com.zerotier.sdk.VirtualNetworkStatus;

import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.ZerotierFixApplication;
import net.kaaass.zerotierfix.events.IsServiceRunningReplyEvent;
import net.kaaass.zerotierfix.events.IsServiceRunningRequestEvent;
import net.kaaass.zerotierfix.events.NetworkListReplyEvent;
import net.kaaass.zerotierfix.events.NetworkListRequestEvent;
import net.kaaass.zerotierfix.events.NodeDestroyedEvent;
import net.kaaass.zerotierfix.events.StopEvent;
import net.kaaass.zerotierfix.events.VPNErrorEvent;
import net.kaaass.zerotierfix.model.Network;
import net.kaaass.zerotierfix.service.ZeroTierOneService;
import net.kaaass.zerotierfix.ui.JoinNetworkActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Remote-first Android TV entry. Tuner cards are configured explicitly;
 * a ZeroTier network name is never presented as a verified tuner identity.
 */
public final class TvHomeActivity extends AppCompatActivity {
    private static final int REQUEST_VPN = 804, EXPORT = 805, IMPORT = 806;
    private TvGatewayProfile pendingSwitch;
    private final EventBus events = EventBus.getDefault();
    private final List<TvGatewayProfile> gateways = new ArrayList<>();
    private LinearLayout cards;
    private TextView summary;
    private TvGatewayProfile pendingConsent;
    private boolean serviceRunning;
    private boolean networkReady;
    private boolean connectionError;
    private long joinedNetworkId;
    private final TvExitSession exitSession = new TvExitSession();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService exitExecutor = Executors.newSingleThreadExecutor();
    private final Runnable pollConnection = new Runnable() {
        @Override public void run() {
            if (!tvResumed) return;
            events.post(new IsServiceRunningRequestEvent());
            events.post(new NetworkListRequestEvent());
            maybeProbeExit();
            uiHandler.postDelayed(this, 7000);
        }
    };
    private boolean tvResumed;
    private boolean exitVerified;
    private boolean probeInProgress;
    private int probeFailures;
    private long verifiedAtMs;
    private String verifiedPublicIp;
    private String verificationProblem;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(color(R.color.tv_canvas));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int inset = getResources().getDimensionPixelSize(R.dimen.tv_screen_inset);
        root.setPadding(inset, inset, inset, inset);
        scroll.addView(root);

        TextView brand = text("BLACKSERV  /  ZEROTIER TV", 19, R.color.tv_focus, true);
        root.addView(brand);

        
        
        

        summary = text("Brak aktywnego połączenia", 18, R.color.tv_muted, false);
        root.addView(summary, verticalSpace(4));

        cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.VERTICAL);
        root.addView(cards, verticalSpace(26));

        root.addView(action("Ustawienia", this::showSettings), verticalSpace(18));

        TextView caption = text(
                "Zielony status pojawi się dopiero po weryfikacji polskiego adresu wyjściowego.",
                16, R.color.tv_muted, false);
        root.addView(caption, verticalSpace(22));

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvResumed = true;
        exitSession.select(TvGatewayStore.getActiveTunerId(this));
        resetExitVerification();
        if (!events.isRegistered(this)) {
            events.register(this);
        }
        serviceRunning = false;
        networkReady = false;
        verificationProblem = android.preference.PreferenceManager.getDefaultSharedPreferences(this)
                .getString("blackserv_tv_last_error", null);
        connectionError = verificationProblem != null;
        refreshCards();
        events.post(new IsServiceRunningRequestEvent());
        events.post(new NetworkListRequestEvent());
        uiHandler.removeCallbacks(pollConnection);
        uiHandler.postDelayed(pollConnection, 2500);
    }

    @Override
    protected void onPause() {
        tvResumed = false;
        uiHandler.removeCallbacks(pollConnection);
        resetExitVerification();
        if (events.isRegistered(this)) {
            events.unregister(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        exitExecutor.shutdownNow();
        super.onDestroy();
    }

    private void resetExitVerification() {
        exitSession.invalidate();
        exitVerified = false;
        probeInProgress = false;
        probeFailures = 0;
        verifiedAtMs = 0;
        verifiedPublicIp = null;
        verificationProblem = null;
    }

    private void refreshCards() {
        gateways.clear();
        try {
            gateways.addAll(TvGatewayStore.load(this));
        } catch (IllegalArgumentException damagedConfig) {
            connectionError = true;
            Toast.makeText(this, "Nie można odczytać zapisanych tunerów", Toast.LENGTH_LONG).show();
        }
        render();
    }

    private void render() {
        View previouslyFocused = getCurrentFocus();
        Object focusedTag = previouslyFocused == null ? null : previouslyFocused.getTag();
        String focusedTunerId = focusedTag instanceof String ? (String) focusedTag : null;
        cards.removeAllViews();
        String selectedId = TvGatewayStore.getActiveTunerId(this);
        int selectedIndex = -1;
        for (int i = 0; i < gateways.size(); i++) {
            if (gateways.get(i).getTunerId().equals(selectedId)) {
                selectedIndex = i;
                break;
            }
        }
        for (int index = 0; index < gateways.size(); index++) {
            TvGatewayProfile profile = gateways.get(index);
            boolean joined = serviceRunning && networkReady
                    && joinedNetworkId == profile.getNetworkId();
            TvConnectionState.Status status = TvConnectionState.forTile(
                    index + 1, selectedIndex + 1, joined, exitVerified && joined,
                    connectionError && index == selectedIndex);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(24), dp(18), dp(24), dp(18));
            card.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.tv_card_min_height));
            card.setFocusable(true);
            card.setClickable(true);
            card.setTag(profile.getTunerId());
            card.setContentDescription(profile.getTunerId() + ", " + profile.getDisplayName()
                    + ", " + label(status));
            card.addView(text(profile.getTunerId(), 26, R.color.tv_text, true));
            card.addView(text(profile.getDisplayName(), 19, R.color.tv_muted, false));
            card.addView(text(profile.getTunerId().equals(selectedId) && verifiedPublicIp != null
                    ? "Publiczny IP: " + verifiedPublicIp : "OK: połącz / rozłącz", 16, R.color.tv_muted, false));
            int stateColor = statusColor(status);
            card.addView(text(label(status), 17, stateColor, true));

            card.setBackground(tileBackground(status, false));
            card.setOnFocusChangeListener((v, focus) ->
                    card.setBackground(tileBackground(status, focus)));
            card.setOnClickListener(v -> toggle(profile));
            card.setOnLongClickListener(v -> {
                confirmDelete(profile);
                return true;
            });
            cards.addView(card, verticalSpace(index == 0 ? 0 : 14));
            if (profile.getTunerId().equals(focusedTunerId)) {
                card.requestFocus();
            }
        }
        if (selectedIndex < 0) {
            summary.setText("Brak aktywnego połączenia");
        } else if (connectionError) {
            summary.setText(verificationProblem != null
                    ? verificationProblem : "Błąd połączenia — sprawdź tuner i trasę");
        } else if (serviceRunning && networkReady && exitVerified) {
            summary.setText("Zweryfikowany publiczny IP: " + verifiedPublicIp);
        } else if (serviceRunning && networkReady
                && gateways.get(selectedIndex).getExpectedPublicIpv4() == null) {
            summary.setText("Dodaj oczekiwany polski publiczny IP, aby potwierdzić wyjście.");
        } else if (verificationProblem != null) {
            summary.setText(verificationProblem);
        } else if (serviceRunning && networkReady) {
            summary.setText("Sieć ZeroTier aktywna. Sprawdzanie publicznego IP wyjścia...");
        } else {
            summary.setText("Łączenie z wybranym tunerem...");
        }
    }

    private void toggle(TvGatewayProfile profile) {
        String current = TvGatewayStore.getActiveTunerId(this);
        if (profile.getTunerId().equals(current)) {
            if (serviceRunning) {
                pendingSwitch = null;
                disconnect();
                return;
            }
            // A process stop can leave only the selected-profile preference.
            // Treat it as stale and reconnect with this single click.
            TvGatewayStore.setActiveTunerId(this, null);
            current = null;
        }
        if (current != null || serviceRunning) {
            pendingSwitch = profile;
            disconnect();
            return;
        }
        TvNetworkStore.ensure(this, profile);
        Intent approval = VpnService.prepare(this);
        if (approval != null) {
            pendingConsent = profile;
            startActivityForResult(approval, REQUEST_VPN);
        } else {
            connect(profile);
        }
    }

    private void connect(TvGatewayProfile profile) {
        android.preference.PreferenceManager.getDefaultSharedPreferences(this).edit().remove("blackserv_tv_last_error").apply();
        exitSession.select(profile.getTunerId());
        resetExitVerification();
        connectionError = false;
        networkReady = false;
        joinedNetworkId = 0;
        TvGatewayStore.setActiveTunerId(this, profile.getTunerId());
        Intent start = new Intent(this, ZeroTierOneService.class);
        start.putExtra(ZeroTierOneService.ZT1_NETWORK_ID, profile.getNetworkId());
        ContextCompat.startForegroundService(this, start);
        render();
    }

    private void disconnect() {
        exitSession.clear();
        resetExitVerification();
        stopService(new Intent(this, ZeroTierOneService.class));
        TvGatewayStore.setActiveTunerId(this, null);
        pendingConsent = null;
        joinedNetworkId = 0;
        networkReady = false;
        serviceRunning = false;
        connectionError = false;
        render();
    }

    @Override
    protected void onActivityResult(int request, int result, @Nullable Intent data) {
        super.onActivityResult(request, result, data);
        if ((request == EXPORT || request == IMPORT) && result == RESULT_OK && data != null && data.getData() != null) {
            try {
                if (request == EXPORT) {
                    try (java.io.OutputStream out = getContentResolver().openOutputStream(data.getData(), "wt")) {
                        if (out == null) throw new IOException("Nie można otworzyć pliku");
                        out.write(TvBackup.exportSettings(this).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                    Toast.makeText(this, "Backup zapisany", Toast.LENGTH_LONG).show();
                } else {
                    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                    try (java.io.InputStream in = getContentResolver().openInputStream(data.getData())) {
                        if (in == null) throw new IOException("Nie można otworzyć pliku");
                        byte[] buffer = new byte[4096]; int n;
                        while ((n = in.read(buffer)) != -1) {
                            if (bytes.size() + n > TvBackup.MAX_BYTES) throw new IOException("Plik jest zbyt duży");
                            bytes.write(buffer, 0, n);
                        }
                    }
                    TvBackup.Settings imported = TvBackup.decode(bytes.toString("UTF-8"));
                    new AlertDialog.Builder(this).setTitle("Przywrócić ustawienia?")
                        .setMessage("Zastąpi zapisane profile, DNS i wyjątki. Tożsamość ZeroTier pozostanie bez zmian.")
                        .setNegativeButton("Anuluj", null).setPositiveButton("Przywróć", (d,w) -> {
                            disconnect();
                            try { TvBackup.restore(this, imported); refreshCards(); }
                            catch (RuntimeException failure) { showProblem(failure.getMessage()); }
                        }).show();
                }
            } catch (Exception failure) { showProblem("Backup: " + failure.getMessage()); }
        }
        if (request == REQUEST_VPN) {
            TvGatewayProfile approved = pendingConsent;
            pendingConsent = null;
            if (result == RESULT_OK && approved != null) {
                connect(approved);
            } else {
                Toast.makeText(this, "Nie udzielono zgody na VPN", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServiceStatus(IsServiceRunningReplyEvent reply) {
        boolean running = reply.isRunning();
        if (serviceRunning != running) {
            serviceRunning = running;
            if (!running) resetExitVerification();
            render();
        }
        maybeProbeExit();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNetworkList(NetworkListReplyEvent reply) {
        String selected = TvGatewayStore.getActiveTunerId(this);
        TvGatewayProfile active = null;
        for (TvGatewayProfile profile : gateways) {
            if (profile.getTunerId().equals(selected)) {
                active = profile;
                break;
            }
        }
        long newNetworkId = 0;
        boolean ready = false;
        if (active != null && reply.getNetworkList() != null) {
            for (VirtualNetworkConfig config : reply.getNetworkList()) {
                if (config == null || config.getNwid() != active.getNetworkId()) continue;
                if (config.getStatus() != VirtualNetworkStatus.NETWORK_STATUS_OK) {
                    verificationProblem = "ZeroTier: " + config.getStatus().toString();
                    connectionError = config.getStatus().toString().contains("ACCESS_DENIED")
                            || config.getStatus().toString().contains("NOT_FOUND");
                    render();
                }
                if (config.getStatus() == VirtualNetworkStatus.NETWORK_STATUS_OK
                        && config.getAssignedAddresses().length > 0) {
                    if (verificationProblem != null && verificationProblem.startsWith("ZeroTier:")) {
                        connectionError = false; verificationProblem = null;
                    }
                    newNetworkId = config.getNwid();
                    ready = true;
                    break;
                }
            }
        }
        if (networkReady != ready || joinedNetworkId != newNetworkId) {
            networkReady = ready;
            joinedNetworkId = newNetworkId;
            if (!ready) resetExitVerification();
            render();
        }
        maybeProbeExit();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNodeDestroyed(NodeDestroyedEvent event) {
        resetExitVerification();
        serviceRunning = false;
        networkReady = false;
        joinedNetworkId = 0;
        if (TvGatewayStore.getActiveTunerId(this) != null) {
            connectionError = true;
            verificationProblem = "Tunel VPN przerwany. Rozłącz i połącz ponownie.";
        }
        render();
        if (pendingSwitch != null) {
            TvGatewayProfile next = pendingSwitch;
            pendingSwitch = null;
            // NodeDestroyed is posted synchronously just before node.close().
            // Queue the replacement so it starts only after teardown returns.
            uiHandler.post(() -> {
                if (tvResumed) {
                    toggle(next);
                } else {
                    pendingSwitch = next;
                }
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onError(VPNErrorEvent error) {
        resetExitVerification();
        connectionError = true;
        verificationProblem = error.getMessage();
        render();
    }

    /**
     * Verify the real internet egress from the TV process, without protecting
     * this HTTPS socket. A verified ZT join alone must never turn a tile green.
     */
    private void maybeProbeExit() {
        if (!tvResumed || !serviceRunning || !networkReady
                || connectionError || probeInProgress) {
            return;
        }
        String selected = TvGatewayStore.getActiveTunerId(this);
        TvGatewayProfile active = null;
        for (TvGatewayProfile profile : gateways) {
            if (profile.getTunerId().equals(selected)
                    && profile.getNetworkId() == joinedNetworkId) {
                active = profile;
                break;
            }
        }
        if (active == null || active.getExpectedPublicIpv4() == null) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (exitVerified && now - verifiedAtMs < 60000) {
            return;
        }
        final String tunerId = active.getTunerId();
        final String expectedIp = active.getExpectedPublicIpv4();
        final long token = exitSession.token();
        probeInProgress = true;
        exitExecutor.execute(() -> {
            String observed = null;
            IOException requestError = null;
            try {
                observed = TvExitProbe.fetchPublicIpv4();
            } catch (IOException error) {
                requestError = error;
            }
            final String actualIp = observed;
            final IOException failure = requestError;
            final TvExitVerification.Result result = failure == null
                    ? TvExitVerification.check(actualIp, expectedIp) : null;
            uiHandler.post(() -> {
                if (!tvResumed || !exitSession.isCurrent(token, tunerId)
                        || !serviceRunning || !networkReady) {
                    return;
                }
                probeInProgress = false;
                if (failure != null) {
                    exitVerified = false;
                    probeFailures++;
                    verificationProblem = "Nie udało się sprawdzić publicznego IP ("
                            + probeFailures + "/3).";
                    if (probeFailures >= 3) {
                        connectionError = true;
                        verificationProblem = "Brak weryfikacji wyjścia. Rozłącz i spróbuj ponownie.";
                    }
                } else if (result == TvExitVerification.Result.VERIFIED) {
                    exitVerified = true;
                    verifiedAtMs = SystemClock.elapsedRealtime();
                    verifiedPublicIp = actualIp;
                    probeFailures = 0;
                    verificationProblem = null;
                } else {
                    exitVerified = false;
                    connectionError = true;
                    verificationProblem = result == TvExitVerification.Result.WRONG_EXIT
                            ? "Zły adres wyjściowy: " + actualIp + " (oczekiwano " + expectedIp + ")."
                            : "Odpowiedź serwera IP niepoprawna. Sprawdź trasę VPN.";
                }
                render();
            });
        });
    }

    private void showProfileActions(TvGatewayProfile chosen) {
        new AlertDialog.Builder(this).setTitle(chosen.getTunerId() + " \u00b7 " + chosen.getDisplayName())
            .setItems(new String[]{"Edytuj", "Duplikuj", "Usu\u0144"}, (dialog, action) -> {
                if (action == 0) showProfileDialog(chosen.getTunerId(), chosen);
                else if (action == 1) duplicateProfile(chosen);
                else confirmDelete(chosen);
            }).show();
    }

    private void duplicateProfile(TvGatewayProfile source) {
        try {
            TvGatewayStore.save(this, TvProfileEditor.duplicate(gateways, source));
            refreshCards();
            Toast.makeText(this, "Zduplikowano profil", Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException | IllegalStateException failure) {
            showProblem(failure.getMessage());
        }
    }

    private void showAddDialog() { showProfileDialog("", null); }

    private void showProfileDialog(String slot, TvGatewayProfile existing) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(10), dp(24), dp(10));
        EditText id = input("ID tunera");
        EditText title = input("Nazwa, np. Romek");
        EditText nwid = input("ZeroTier Network ID (16 znaków hex)");
        EditText gateway = input("Adres ZeroTier tunera (IPv4)");
        EditText expected = input("Polski publiczny IP (opcjonalnie)");
        id.setText(existing == null ? slot : existing.getTunerId());
        title.setText(existing == null ? "Tuner " + slot + " — Polska" : existing.getDisplayName());
        if (existing != null) {
            nwid.setText(existing.getNetworkIdHex()); gateway.setText(existing.getGatewayIpv4());
            expected.setText(existing.getExpectedPublicIpv4());
        }
        for (EditText field : new EditText[]{id, title, nwid, gateway, expected}) {
            form.addView(field);
        }
        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Dodaj tuner" : "Edytuj tuner")
                .setView(form)
                .setNegativeButton("Anuluj", null)
                .setPositiveButton("Zapisz", (dialog, which) -> {
                    try {
                        TvGatewayProfile profile = new TvGatewayProfile(
                                id.getText().toString(), title.getText().toString(),
                                nwid.getText().toString(), gateway.getText().toString(),
                                expected.getText().toString());
                        List<TvGatewayProfile> updated = TvProfileEditor.apply(gateways, existing, profile);
                        boolean editedWasActive = existing != null
                                && existing.getTunerId().equals(TvGatewayStore.getActiveTunerId(this));
                        if (editedWasActive) disconnect();
                        TvNetworkStore.ensure(this, profile);
                        TvGatewayStore.save(this, updated);
                        refreshCards();
                    } catch (IllegalArgumentException | IllegalStateException invalid) {
                        Toast.makeText(this, invalid.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }).show();
    }


    private void showProblem(String message) {
        new AlertDialog.Builder(this).setTitle("Nie wykonano operacji").setMessage(message)
            .setPositiveButton("Zamknij", null).show();
    }
    private void showSettings() {
        String[] options = {"Profile tunerów", "Aplikacje bez VPN", "Diagnostyka", "DNS",
            "Uruchamianie po restarcie", "Eksport ustawień JSON", "Import ustawień JSON"};
        new AlertDialog.Builder(this).setTitle("Ustawienia").setItems(options, (d, which) -> {
            switch (which) {
                case 0:
                    List<String> names = new ArrayList<>();
                    for (TvGatewayProfile p : gateways) names.add(p.getTunerId() + " · " + p.getDisplayName());
                    names.add("Dodaj profil");
                    new AlertDialog.Builder(this).setTitle("Profile tunerów")
                        .setItems(names.toArray(new String[0]), (a, index) -> {
                            if (index == gateways.size()) showAddDialog();
                            else showProfileActions(gateways.get(index));
                        }).show(); break;
                case 1: startActivity(new Intent(this, TvBypassActivity.class)); break;
                case 2: startActivity(new Intent(this, TvDiagnosticsActivity.class)); break;
                case 3:
                    EditText dns = input("DNS oddzielone przecinkami"); dns.setText(TvNetworkSettings.dns(this));
                    new AlertDialog.Builder(this).setTitle("DNS przez VPN").setView(dns)
                        .setNegativeButton("Anuluj", null).setPositiveButton("Zapisz", (a,b) -> {
                            try {
                                String value = String.join(",", TvNetworkSettings.parseDns(dns.getText().toString()));
                                android.preference.PreferenceManager.getDefaultSharedPreferences(this).edit()
                                    .putString(TvNetworkSettings.DNS, value).apply();
                                Toast.makeText(this, "Zapisano. Połącz VPN ponownie.", Toast.LENGTH_LONG).show();
                            } catch (IllegalArgumentException error) { showProblem(error.getMessage()); }
                        }).show(); break;
                case 4:
                    var prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
                    boolean boot = prefs.getBoolean(TvNetworkSettings.BOOT, false);
                    new AlertDialog.Builder(this).setTitle("Po restarcie")
                        .setSingleChoiceItems(new String[]{"Nie łącz automatycznie", "Przywróć ostatnio aktywny profil"}, boot ? 1 : 0,
                            (a,index) -> { prefs.edit().putBoolean(TvNetworkSettings.BOOT, index == 1).apply(); a.dismiss(); }).show(); break;
                case 5: document(EXPORT); break;
                case 6: document(IMPORT); break;
            }
        }).setNegativeButton("Wstecz", null).show();
    }
    private void document(int request) {
        Intent pick = new Intent(request == EXPORT ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE); pick.setType("application/json");
        if (request == EXPORT) pick.putExtra(Intent.EXTRA_TITLE, "blackserv-zerotier-tv.json");
        try { startActivityForResult(pick, request); }
        catch (android.content.ActivityNotFoundException missing) {
            showProblem("Brak systemowego wyboru plików. Zainstaluj aplikację obsługującą dokumenty na Android TV.");
        }
    }

    private void confirmDelete(TvGatewayProfile profile) {
        new AlertDialog.Builder(this)
                .setTitle("Usunąć zapisany tuner?")
                .setMessage(profile.getTunerId())
                .setNegativeButton("Anuluj", null)
                .setPositiveButton("Usuń", (dialog, which) -> {
                    if (profile.getTunerId().equals(TvGatewayStore.getActiveTunerId(this))) {
                        disconnect();
                    }
                    List<TvGatewayProfile> copy = new ArrayList<>(gateways);
                    copy.remove(profile);
                    TvGatewayStore.save(this, copy);
                    refreshCards();
                }).show();
    }

    private EditText input(String hint) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setTextSize(18);
        field.setHint(hint);
        return field;
    }

    private TextView action(String label, Runnable onClick) {
        TextView view = text(label, 19, R.color.tv_text, true);
        view.setPadding(dp(18), dp(12), dp(18), dp(12));
        view.setFocusable(true);
        view.setClickable(true);
        view.setOnFocusChangeListener((v, focused) ->
                view.setBackground(tileBackground(TvConnectionState.Status.OFF, focused)));
        view.setBackground(tileBackground(TvConnectionState.Status.OFF, false));
        view.setOnClickListener(v -> onClick.run());
        return view;
    }

    private TextView text(String value, int sp, int colorResource, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color(colorResource));
        view.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private GradientDrawable tileBackground(TvConnectionState.Status status, boolean focused) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color(R.color.tv_surface));
        drawable.setCornerRadius(dp(18));
        drawable.setStroke(dp(focused ? 4 : 1),
                color(focused ? R.color.tv_focus : statusColor(status)));
        return drawable;
    }

    private int statusColor(TvConnectionState.Status status) {
        switch (status) {
            case ON: return R.color.tv_connected;
            case CONNECTING:
            case VERIFYING_EXIT: return R.color.tv_pending;
            case ERROR: return R.color.tv_error;
            default: return R.color.tv_disconnected;
        }
    }

    private String label(TvConnectionState.Status status) {
        switch (status) {
            case ON: return "POŁĄCZONO";
            case CONNECTING: return "ŁĄCZENIE";
            case VERIFYING_EXIT: return "WERYFIKACJA WYJŚCIA";
            case ERROR: return "BŁĄD";
            default: return "WYŁĄCZONY";
        }
    }

    private int color(int colorResource) {
        return ContextCompat.getColor(this, colorResource);
    }

    private int dp(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }

    private LinearLayout.LayoutParams verticalSpace(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topMargin);
        return params;
    }
}
