package net.kaaass.zerotierfix.ui.tv;

/**
 * Immutable status calculation for one Android TV host tile.
 *
 * A joined ZeroTier network alone never proves that the selected exit
 * gateway is routing traffic. The tile is green only after verification.
 */
public final class TvConnectionState {
    public enum Status {
        OFF,
        CONNECTING,
        VERIFYING_EXIT,
        ON,
        ERROR
    }

    private TvConnectionState() {
    }

    public static Status forTile(long tileId, long selectedTileId,
                                 boolean zeroTierReady, boolean exitVerified,
                                 boolean connectionError) {
        if (selectedTileId == 0 || selectedTileId != tileId) {
            return Status.OFF;
        }
        if (connectionError) {
            return Status.ERROR;
        }
        if (!zeroTierReady) {
            return Status.CONNECTING;
        }
        if (!exitVerified) {
            return Status.VERIFYING_EXIT;
        }
        return Status.ON;
    }
}
