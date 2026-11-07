package net.kaaass.zerotierfix.ui.tv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TvProfileEditorTest {
    private static TvGatewayProfile profile(String id, String name) {
        return new TvGatewayProfile(id, name, "05b1a061a1000001", "192.168.18.1", "91.227.216.230");
    }

    private static List<TvGatewayProfile> three() {
        return new ArrayList<>(Arrays.asList(
                profile("#5", "Piatka"), profile("#9", "Dziewiatka"), profile("#23", "Dwudziestka")));
    }

    private static List<String> ids(List<TvGatewayProfile> list) {
        List<String> ids = new ArrayList<>();
        for (TvGatewayProfile p : list) ids.add(p.getTunerId());
        return ids;
    }

    @Test public void renamingKeepsSingleProfileAtSamePosition() {
        List<TvGatewayProfile> before = three();
        List<TvGatewayProfile> after = TvProfileEditor.apply(before, before.get(0), profile("#5", "Nowa nazwa"));
        assertEquals(Arrays.asList("#5", "#9", "#23"), ids(after));
        assertEquals("Nowa nazwa", after.get(0).getDisplayName());
    }

    @Test public void changingIdReplacesInPlaceInsteadOfCloning() {
        List<TvGatewayProfile> before = three();
        List<TvGatewayProfile> after = TvProfileEditor.apply(before, before.get(0), profile("#5-nowy", "Piatka"));
        assertEquals(Arrays.asList("#5-nowy", "#9", "#23"), ids(after));
    }

    @Test public void changingIdToAnotherProfilesIdIsRejected() {
        List<TvGatewayProfile> before = three();
        try {
            TvProfileEditor.apply(before, before.get(0), profile("#9", "Piatka"));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals(Arrays.asList("#5", "#9", "#23"), ids(before));
        }
    }

    @Test public void editingProfileThatNoLongerExistsFails() {
        List<TvGatewayProfile> before = three();
        try {
            TvProfileEditor.apply(before, profile("#77", "Brak"), profile("#77", "Brak"));
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertEquals(3, before.size());
        }
    }

    @Test public void addAppendsNewProfile() {
        List<TvGatewayProfile> after = TvProfileEditor.apply(three(), null, profile("#31", "Nowy"));
        assertEquals(Arrays.asList("#5", "#9", "#23", "#31"), ids(after));
    }

    @Test public void addingExistingIdIsRejected() {
        try {
            TvProfileEditor.apply(three(), null, profile("#9", "Inny"));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // no duplicate was created
        }
    }

    @Test public void applyNeverMutatesInputList() {
        List<TvGatewayProfile> before = three();
        TvProfileEditor.apply(before, before.get(1), profile("#9x", "Zmiana"));
        TvProfileEditor.apply(before, null, profile("#40", "Dodany"));
        assertEquals(Arrays.asList("#5", "#9", "#23"), ids(before));
    }

    @Test public void duplicateInsertsCopyRightAfterSourceWithSameNetwork() {
        List<TvGatewayProfile> before = three();
        List<TvGatewayProfile> after = TvProfileEditor.duplicate(before, before.get(0));
        assertEquals(Arrays.asList("#5", "#5 (kopia)", "#9", "#23"), ids(after));
        assertEquals(before.get(0).getNetworkIdHex(), after.get(1).getNetworkIdHex());
        assertEquals("Piatka (kopia)", after.get(1).getDisplayName());
        assertEquals(3, before.size());
    }

    @Test public void duplicatingTwiceGivesDistinctIds() {
        List<TvGatewayProfile> once = TvProfileEditor.duplicate(three(), three().get(0));
        List<TvGatewayProfile> twice = TvProfileEditor.duplicate(once, once.get(0));
        assertEquals(Arrays.asList("#5", "#5 (kopia 2)", "#5 (kopia)", "#9", "#23"), ids(twice));
    }
}
