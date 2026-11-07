package net.kaaass.zerotierfix.ui.tv;

/**
 * Pure, fail-closed decision for the TV tile. A ZeroTier join is not enough.
 * The expected public address is supplied by the user after checking the tuner.
 */
public final class TvExitVerification {
    public enum Result {
        VERIFIED,
        WRONG_EXIT,
        MISSING_EXPECTED_IP,
        INVALID_RESPONSE
    }

    private TvExitVerification() {
    }

    public static Result check(String response, String expectedPublicIpv4) {
        if (!isPublicIpv4(response)) {
            return Result.INVALID_RESPONSE;
        }
        if (expectedPublicIpv4 == null || expectedPublicIpv4.trim().isEmpty()) {
            return Result.MISSING_EXPECTED_IP;
        }
        if (!isPublicIpv4(expectedPublicIpv4)) {
            return Result.INVALID_RESPONSE;
        }
        return response.trim().equals(expectedPublicIpv4.trim())
                ? Result.VERIFIED : Result.WRONG_EXIT;
    }

    private static boolean isPublicIpv4(String value) {
        if (value == null) {
            return false;
        }
        String address = value.trim();
        if (!address.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) {
            return false;
        }
        String[] parts = address.split("\\.");
        int[] bytes = new int[4];
        for (int i = 0; i < bytes.length; i++) {
            if (parts[i].length() > 1 && parts[i].startsWith("0")) {
                return false;
            }
            bytes[i] = Integer.parseInt(parts[i]);
            if (bytes[i] > 255) {
                return false;
            }
        }
        int a = bytes[0], b = bytes[1], c = bytes[2];
        return a != 0 && a != 10 && a != 127 && a < 224
                && !(a == 100 && b >= 64 && b <= 127)
                && !(a == 169 && b == 254)
                && !(a == 172 && b >= 16 && b <= 31)
                && !(a == 192 && b == 168)
                && !(a == 192 && b == 0 && (c == 0 || c == 2))
                && !(a == 198 && (b == 18 || b == 19 || (b == 51 && c == 100)))
                && !(a == 203 && b == 0 && c == 113);
    }
}
