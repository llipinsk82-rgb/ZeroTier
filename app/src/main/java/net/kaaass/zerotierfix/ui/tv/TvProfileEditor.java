package net.kaaass.zerotierfix.ui.tv;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure list operations for tuner profiles.
 * Editing replaces the edited profile in place; it never leaves the old one behind.
 * Duplicating is a separate, explicit operation.
 */
public final class TvProfileEditor {
    private TvProfileEditor() {
    }

    /**
     * @param original the profile being edited, or null when adding a new one
     * @param edited   the profile as the user wants it to be saved
     * @return a new list; {@code current} is never modified
     */
    public static List<TvGatewayProfile> apply(List<TvGatewayProfile> current,
                                               TvGatewayProfile original,
                                               TvGatewayProfile edited) {
        List<TvGatewayProfile> result = new ArrayList<>(current);
        if (original == null) {
            if (indexOf(result, edited.getTunerId()) >= 0) {
                throw new IllegalArgumentException(
                        "Tuner o tym ID już istnieje: " + edited.getTunerId());
            }
            result.add(edited);
            return result;
        }
        int index = indexOf(result, original.getTunerId());
        if (index < 0) {
            throw new IllegalStateException("Edytowany profil już nie istnieje");
        }
        int clash = indexOf(result, edited.getTunerId());
        if (clash >= 0 && clash != index) {
            throw new IllegalArgumentException(
                    "Tuner o tym ID już istnieje: " + edited.getTunerId());
        }
        result.set(index, edited);
        return result;
    }

    /** Adds a copy right after {@code source}, with a free ID and the same settings. */
    public static List<TvGatewayProfile> duplicate(List<TvGatewayProfile> current,
                                                   TvGatewayProfile source) {
        int index = indexOf(current, source.getTunerId());
        if (index < 0) {
            throw new IllegalStateException("Duplikowany profil już nie istnieje");
        }
        TvGatewayProfile copy = new TvGatewayProfile(
                freeId(current, source.getTunerId()),
                source.getDisplayName() + " (kopia)",
                source.getNetworkIdHex(),
                source.getGatewayIpv4(),
                source.getExpectedPublicIpv4());
        List<TvGatewayProfile> result = new ArrayList<>(current);
        result.add(index + 1, copy);
        return result;
    }

    static String freeId(List<TvGatewayProfile> current, String base) {
        String candidate = base + " (kopia)";
        int number = 2;
        while (indexOf(current, candidate) >= 0) {
            candidate = base + " (kopia " + number + ")";
            number++;
        }
        return candidate;
    }

    private static int indexOf(List<TvGatewayProfile> profiles, String tunerId) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getTunerId().equals(tunerId)) {
                return i;
            }
        }
        return -1;
    }
}
