package net.kaaass.zerotierfix.service;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class RouteSelectionTest {
    private static final class FakeRoute {
        final String name;
        final int prefix;
        final boolean matches;
        FakeRoute(String name, int prefix, boolean matches) {
            this.name = name; this.prefix = prefix; this.matches = matches;
        }
    }

    @Test public void longestMatchingPrefixAlwaysWinsOverDefault() {
        List<FakeRoute> routes = Arrays.asList(
                new FakeRoute("default", 0, true),
                new FakeRoute("overlay", 24, true),
                new FakeRoute("specific", 32, true));
        assertEquals("specific", RouteSelection.longestMatch(routes,
                r -> r.matches, r -> r.prefix).name);
    }

    @Test public void nonMatchingSpecificRouteCannotWin() {
        List<FakeRoute> routes = Arrays.asList(
                new FakeRoute("specific", 32, false),
                new FakeRoute("overlay", 24, true),
                new FakeRoute("default", 0, true));
        assertEquals("overlay", RouteSelection.longestMatch(routes,
                r -> r.matches, r -> r.prefix).name);
    }

    @Test public void noMatchingRoutesReturnsNull() {
        assertNull(RouteSelection.longestMatch(
                Collections.singletonList(new FakeRoute("no", 24, false)),
                r -> r.matches, r -> r.prefix));
    }

    @Test public void emptyRoutesReturnsNull() {
        assertNull(RouteSelection.longestMatch(Collections.<FakeRoute>emptyList(),
                r -> r.matches, r -> r.prefix));
    }
}
