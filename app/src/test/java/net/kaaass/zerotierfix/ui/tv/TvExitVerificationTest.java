package net.kaaass.zerotierfix.ui.tv;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TvExitVerificationTest {
    @Test public void matchingPublicIpv4Passes() {
        assertEquals(TvExitVerification.Result.VERIFIED,
                TvExitVerification.check(" 83.12.34.56\n", "83.12.34.56"));
    }
    @Test public void mismatchingIpv4IsNotVerified() {
        assertEquals(TvExitVerification.Result.WRONG_EXIT,
                TvExitVerification.check("86.12.34.56", "83.12.34.56"));
    }
    @Test public void missingPinnedExpectedIpCannotBeGreen() {
        assertEquals(TvExitVerification.Result.MISSING_EXPECTED_IP,
                TvExitVerification.check("83.12.34.56", null));
    }
    @Test public void malformedEndpointResponseIsRejected() {
        assertEquals(TvExitVerification.Result.INVALID_RESPONSE,
                TvExitVerification.check("<html>83.12.34.56</html>", "83.12.34.56"));
    }
    @Test public void privateAndLoopbackIpsCannotBeVerified() {
        assertEquals(TvExitVerification.Result.INVALID_RESPONSE,
                TvExitVerification.check("192.168.1.3", "192.168.1.3"));
        assertEquals(TvExitVerification.Result.INVALID_RESPONSE,
                TvExitVerification.check("127.0.0.1", "127.0.0.1"));
    }
    @Test public void malformedExpectedIpNeverBecomesVerified() {
        assertEquals(TvExitVerification.Result.INVALID_RESPONSE,
                TvExitVerification.check("83.12.34.56", "83.12.34.999"));
    }
}
