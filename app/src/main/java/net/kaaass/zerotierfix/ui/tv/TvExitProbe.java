package net.kaaass.zerotierfix.ui.tv;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;

/**
 * Fetches the observed public IPv4 through the app's ordinary VPN-routed socket.
 * Do not protect/bind this request to the physical network: only the ZeroTier
 * transport socket is protected against VPN loops.
 */
public final class TvExitProbe {
    private static final String IPV4_ENDPOINT = "https://api.ipify.org";

    private TvExitProbe() {
    }

    public static String fetchPublicIpv4() throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection)
                new URL(IPV4_ENDPOINT).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "text/plain");
        try {
            if (connection.getResponseCode() != 200) {
                throw new IOException("Exit IP endpoint rejected request");
            }
            try (Reader reader = new InputStreamReader(connection.getInputStream(),
                    StandardCharsets.US_ASCII)) {
                return readResponse(reader);
            }
        } finally {
            connection.disconnect();
        }
    }

    static String readResponse(Reader reader) throws IOException {
        StringBuilder text = new StringBuilder();
        char[] chunk = new char[32];
        int count;
        while ((count = reader.read(chunk)) != -1) {
            text.append(chunk, 0, count);
            if (text.length() > 64) {
                throw new IOException("Exit IP response too long");
            }
        }
        return text.toString().trim();
    }
}
