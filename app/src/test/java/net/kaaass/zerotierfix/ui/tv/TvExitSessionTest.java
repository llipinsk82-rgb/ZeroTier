package net.kaaass.zerotierfix.ui.tv;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TvExitSessionTest {
    @Test public void onlyCurrentTunerMayAcceptProbe() {
        TvExitSession session = new TvExitSession();
        long token = session.select("u39.romek");
        assertTrue(session.isCurrent(token, "u39.romek"));
        assertFalse(session.isCurrent(token, "u27.bocian"));
    }
    @Test public void lateResultFromPreviousTunerIsIgnored() {
        TvExitSession session = new TvExitSession();
        long first = session.select("u39.romek");
        long second = session.select("u27.bocian");
        assertFalse(session.isCurrent(first, "u39.romek"));
        assertTrue(session.isCurrent(second, "u27.bocian"));
    }
    @Test public void sameTunerAfterReconnectMustUseNewToken() {
        TvExitSession session = new TvExitSession();
        long before = session.select("u39.romek");
        session.invalidate();
        long after = session.select("u39.romek");
        assertFalse(session.isCurrent(before, "u39.romek"));
        assertTrue(session.isCurrent(after, "u39.romek"));
    }
    @Test public void disconnectRejectsLateVerification() {
        TvExitSession session = new TvExitSession();
        long before = session.select("u39.romek");
        session.clear();
        assertFalse(session.isCurrent(before, "u39.romek"));
    }
}
