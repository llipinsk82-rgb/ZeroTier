package net.kaaass.zerotierfix.ui.tv;

/** Rejects a late exit-IP result after switching, disconnecting or pausing TV UI. */
public final class TvExitSession {
    private long generation;
    private String currentTuner;

    public long select(String tunerId) {
        currentTuner = tunerId;
        return ++generation;
    }

    public void invalidate() {
        generation++;
    }

    public void clear() {
        currentTuner = null;
        invalidate();
    }

    public long token() {
        return generation;
    }

    public boolean isCurrent(long token, String tunerId) {
        return currentTuner != null
                && currentTuner.equals(tunerId)
                && generation == token;
    }
}
