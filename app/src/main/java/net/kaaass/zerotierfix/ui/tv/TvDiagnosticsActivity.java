package net.kaaass.zerotierfix.ui.tv;

import android.os.Bundle;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.widget.*;
import android.view.View;
import android.graphics.drawable.GradientDrawable;
import androidx.appcompat.app.AppCompatActivity;
import net.kaaass.zerotierfix.R;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.*;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

/** Real, bounded measurements. No generated metrics and no DNS-leak PASS without resolver evidence. */
public final class TvDiagnosticsActivity extends AppCompatActivity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView result;
    private boolean busy;
    private volatile boolean cancelled;
    private volatile HttpsURLConnection connection;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(getColor(R.color.tv_canvas));
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48,32,48,32); scroll.addView(root);
        TextView title = new TextView(this); title.setText("Diagnostyka połączenia"); title.setTextSize(30);
        title.setTextColor(getColor(R.color.tv_text)); root.addView(title);
        add(root, "Publiczny IP i geolokalizacja", () -> run(this::publicIp));
        add(root, "Prędkość ↓ / ↑ (do 12 MB danych)", () -> run(this::speed));
        add(root, "Test DNS leak (bash.ws)", () -> run(this::dns));
        add(root, "Status VPN i trasy", () -> run(this::routes));
        add(root, "Wstecz", this::finish);
        result = new TextView(this); result.setTextSize(18); result.setTextColor(getColor(R.color.tv_text));
        result.setPadding(12,24,12,24); result.setText("Wybierz test. Wyniki dotyczą bieżącego połączenia tego urządzenia.");
        result.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        root.addView(result); setContentView(scroll);
    }
    private void add(LinearLayout root, String text, Runnable task) {
        Button button = new Button(this); button.setText(text); button.setTextSize(19);
        button.setAllCaps(false); button.setOnClickListener(v -> task.run()); root.addView(button);
    }
    private interface Test { String measure() throws Exception; }
    private void run(Test test) {
        if (busy) { result.setText("Test trwa. Wstecz anuluje pomiar."); return; }
        busy = true; result.setText("Pomiar…");
        worker.execute(() -> {
            String text;
            try { text = test.measure(); } catch (Exception e) { text = "Nie udało się wykonać testu: " + e.getMessage(); }
            final String output = text;
            runOnUiThread(() -> { busy = false; if (!isFinishing() && !isDestroyed()) result.setText(output); });
        });
    }
    private HttpsURLConnection open(String url) throws IOException {
        if (cancelled) throw new IOException("Anulowano");
        HttpsURLConnection c = (HttpsURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000); c.setReadTimeout(8000); c.setUseCaches(false);
        c.setInstanceFollowRedirects(false); connection = c; return c;
    }
    private String get(String url) throws IOException {
        HttpsURLConnection c = open(url);
        try {
            if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = c.getInputStream()) {
                byte[] buf = new byte[2048]; int n;
                while ((n = in.read(buf)) != -1) {
                    if (cancelled || out.size()+n > 65536) throw new IOException("Przerwano odpowiedź");
                    out.write(buf,0,n);
                }
            }
            return out.toString("UTF-8");
        } finally { c.disconnect(); connection = null; }
    }
    private String publicIp() throws Exception {
        String ip = TvExitProbe.fetchPublicIpv4();
        try {
            JSONObject geo = new JSONObject(get("https://ipwho.is/" + ip));
            if (!geo.optBoolean("success", false)) throw new IOException("Geolokalizacja niedostępna");
            return "Publiczny IPv4: " + ip + "\nKraj: " + geo.optString("country") + " (" + geo.optString("country_code")
                + ")\nMiasto (przybliżone): " + geo.optString("city") + "\nSzer./dł.: "
                + geo.optString("latitude") + ", " + geo.optString("longitude")
                + "\nŹródło: ipify / ipwho.is; lokalizacja IP nie jest lokalizacją GPS.";
        } catch (Exception unavailable) { return "Publiczny IPv4: " + ip + "\nGeolokalizacja niedostępna."; }
    }
    private String speed() throws Exception {
        long bytes = 0; long start = System.nanoTime();
        HttpsURLConnection down = open("https://speed.cloudflare.com/__down?bytes=10000000");
        try {
            if (down.getResponseCode() != 200) throw new IOException("Błąd pobierania");
            try (InputStream in = down.getInputStream()) {
                byte[] buffer = new byte[32768]; int n;
                while ((n=in.read(buffer)) != -1) {
                    bytes += n;
                    if (cancelled || bytes > 10000000 || System.nanoTime()-start > 20000000000L) break;
                }
            }
        } finally { down.disconnect(); connection = null; }
        double download = mbps(bytes, System.nanoTime()-start);
        HttpsURLConnection up = open("https://speed.cloudflare.com/__up");
        start = System.nanoTime();
        try {
            up.setRequestMethod("POST"); up.setDoOutput(true); up.setFixedLengthStreamingMode(2000000);
            up.setRequestProperty("Content-Type", "application/octet-stream");
            byte[] buffer = new byte[16384]; new java.security.SecureRandom().nextBytes(buffer);
            try (OutputStream out = up.getOutputStream()) {
                for (int sent=0;sent<2000000;) {
                    if (cancelled || System.nanoTime()-start > 20000000000L) throw new IOException("Upload przekroczył limit czasu");
                    int n = Math.min(buffer.length, 2000000-sent); out.write(buffer,0,n); sent+=n;
                }
            }
            int code = up.getResponseCode();
            if (code < 200 || code >= 300) throw new IOException("Upload HTTP " + code);
            return String.format(Locale.getDefault(), "Pobieranie: %.2f Mb/s\nWysyłanie: %.2f Mb/s\nPomiar jednego strumienia HTTPS do Cloudflare, z narzutem zestawienia połączenia.",
                download, mbps(2000000, System.nanoTime()-start));
        } finally { up.disconnect(); connection=null; }
    }
    static double mbps(long bytes, long nanos) { return nanos <= 0 ? 0 : bytes * 8000.0 / nanos; }
    private String dns() throws Exception {
        String id = get("https://bash.ws/id").trim();
        if (!id.matches("[A-Za-z0-9-]{1,64}")) throw new IOException("Niepoprawny identyfikator testu DNS");
        try { get("https://ipv4.bash.ws/myip/test/" + id); } catch (IOException ignored) { }
        try { get("https://ipv6.bash.ws/myip/test/" + id); } catch (IOException ignored) { }
        java.util.concurrent.ExecutorService probes = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            java.util.List<java.util.concurrent.Callable<Void>> queries = new java.util.ArrayList<>();
            for (int i=1; i<=8; i++) {
                final String name = i + "." + id + ".bash.ws";
                queries.add(() -> { try { InetAddress.getAllByName(name); } catch (UnknownHostException ignored) { } return null; });
            }
            probes.invokeAll(queries, 12, TimeUnit.SECONDS);
        } finally { probes.shutdownNow(); }
        if (cancelled) throw new IOException("Anulowano");
        org.json.JSONArray records = new org.json.JSONArray(get("https://bash.ws/dnsleak/test/" + id + "?json=2"));
        StringBuilder out = new StringBuilder("Resolverzy zaobserwowani przez bash.ws:\n");
        int found = 0;
        for (int i=0; i<records.length(); i++) {
            JSONObject record = records.getJSONObject(i);
            if ("error".equals(record.optString("type"))) throw new IOException(record.optString("text"));
            if ("dns".equals(record.optString("type"))) {
                found++;
                out.append(record.optString("ip")).append(" · ").append(record.optString("country_name"))
                    .append(" · ").append(record.optString("asn")).append("\n");
            }
        }
        if (found == 0) return "DNS leak: nierozstrzygnięty — serwer nie zarejestrował resolverów.";
        out.append("\nPorównaj operatorów z wybranym DNS: ").append(TvNetworkSettings.dns(this));
        out.append("\nResolver operatora UK podczas VPN może oznaczać wyciek. Kraj publicznego resolvera sam nie dowodzi wycieku. Test dotyczy DNS systemowego tej aplikacji, nie własnego DoH innych aplikacji.");
        return out.toString();
    }
    private String routes() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        StringBuilder out = new StringBuilder();
        for (android.net.Network n : cm.getAllNetworks()) {
            var caps = cm.getNetworkCapabilities(n); var lp = cm.getLinkProperties(n);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) && lp != null)
                out.append("Interfejs VPN: ").append(lp.getInterfaceName()).append("\nAdresy: ")
                   .append(lp.getLinkAddresses()).append("\nTrasy: ").append(lp.getRoutes()).append("\n");
        }
        if (out.length() == 0) out.append("Android nie zgłasza aktywnego VPN.\n");
        // Only the public node address is displayed; never read identity.secret.
        try (InputStream in = openFileInput("identity.public")) {
            byte[] prefix = new byte[11]; int count = 0;
            while (count < prefix.length) {
                int n = in.read(prefix, count, prefix.length - count);
                if (n < 0) break;
                count += n;
            }
            String id = new String(prefix, 0, count, StandardCharsets.US_ASCII);
            if (id.matches("[0-9a-fA-F]{10}:"))
                out.append("ID tego klienta ZeroTier: ").append(id.substring(0,10)).append("\n");
        } catch (IOException absent) { out.append("Tożsamość ZeroTier jeszcze niedostępna.\n"); }
        String selected = TvGatewayStore.getActiveTunerId(this);
        for (TvGatewayProfile p : TvGatewayStore.load(this)) if (p.getTunerId().equals(selected))
            out.append("Wybrana brama: ").append(p.getGatewayIpv4()).append("\nSieć ZT: ").append(p.getNetworkIdHex())
                .append("\nIPv6 zablokowany dla tej bramy IPv4; brak potwierdzonej bramy IPv6.");
        return out.toString();
    }
    @Override protected void onDestroy() {
        cancelled=true; HttpsURLConnection c=connection; if(c!=null)c.disconnect(); worker.shutdownNow(); super.onDestroy();
    }
}
