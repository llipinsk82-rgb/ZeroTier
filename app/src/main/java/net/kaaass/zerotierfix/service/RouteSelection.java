package net.kaaass.zerotierfix.service;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/** Picks the longest matching CIDR prefix, regardless of map iteration order. */
public final class RouteSelection {
    private RouteSelection() {
    }

    public static <T> T longestMatch(Iterable<T> routes,
                                     Predicate<? super T> matches,
                                     ToIntFunction<? super T> prefix) {
        T best = null;
        int bestPrefix = -1;
        for (T route : routes) {
            int currentPrefix = prefix.applyAsInt(route);
            if (currentPrefix > bestPrefix && matches.test(route)) {
                best = route;
                bestPrefix = currentPrefix;
            }
        }
        return best;
    }
}
