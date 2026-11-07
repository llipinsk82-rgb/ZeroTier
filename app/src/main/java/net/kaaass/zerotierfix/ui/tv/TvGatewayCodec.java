package net.kaaass.zerotierfix.ui.tv;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Versioned, lossless local configuration format for the TV gateway list. */
public final class TvGatewayCodec {
    private static final String HEADER = "BSZTTV1\n";

    private TvGatewayCodec() {
    }

    public static String encode(Collection<TvGatewayProfile> profiles) {
        StringBuilder serialized = new StringBuilder(HEADER);
        Set<String> ids = new HashSet<>();
        for (TvGatewayProfile profile : profiles) {
            if (!ids.add(profile.getTunerId())) {
                throw new IllegalArgumentException("Duplicate tuner ID: " + profile.getTunerId());
            }
            serialized.append(escape(profile.getTunerId())).append('\t')
                    .append(escape(profile.getDisplayName())).append('\t')
                    .append(profile.getNetworkIdHex()).append('\t')
                    .append(profile.getGatewayIpv4()).append('\t')
                    .append(profile.getExpectedPublicIpv4() == null
                            ? "" : profile.getExpectedPublicIpv4())
                    .append('\n');
        }
        return serialized.toString();
    }

    public static List<TvGatewayProfile> decode(String raw) {
        List<TvGatewayProfile> profiles = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return profiles;
        }
        if (!raw.startsWith(HEADER)) {
            throw new IllegalArgumentException("Unknown or invalid TV gateway data version");
        }
        Set<String> ids = new HashSet<>();
        String body = raw.substring(HEADER.length());
        if (body.isEmpty()) {
            return profiles;
        }
        String[] lines = body.split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            if (index == lines.length - 1 && line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            if (parts.length != 5) {
                throw new IllegalArgumentException("Corrupt gateway record at line " + (index + 1));
            }
            TvGatewayProfile profile = new TvGatewayProfile(
                    unescape(parts[0]), unescape(parts[1]),
                    parts[2], parts[3], parts[4]);
            if (!ids.add(profile.getTunerId())) {
                throw new IllegalArgumentException("Duplicate tuner ID: " + profile.getTunerId());
            }
            profiles.add(profile);
        }
        return profiles;
    }

    private static String escape(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String unescape(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
