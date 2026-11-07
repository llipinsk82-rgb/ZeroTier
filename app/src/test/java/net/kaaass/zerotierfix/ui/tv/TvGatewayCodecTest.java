package net.kaaass.zerotierfix.ui.tv;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class TvGatewayCodecTest {
    @Test public void emptyListRoundTrips() {
        assertTrue(TvGatewayCodec.decode(TvGatewayCodec.encode(
                Collections.<TvGatewayProfile>emptyList())).isEmpty());
    }

    @Test public void roundTripsPolishNamesAndMultipleGatewaysOnOneNetwork() {
        List<TvGatewayProfile> input = Arrays.asList(
                new TvGatewayProfile("u39.romek", "Romek | Łódź", "0123456789abcdef",
                        "10.147.17.5", "83.10.20.30"),
                new TvGatewayProfile("u23.mama", "Mama", "0123456789abcdef",
                        "10.147.17.8", null));
        List<TvGatewayProfile> output = TvGatewayCodec.decode(TvGatewayCodec.encode(input));
        assertEquals(2, output.size());
        assertEquals("Romek | Łódź", output.get(0).getDisplayName());
        assertEquals("10.147.17.8", output.get(1).getGatewayIpv4());
        assertNull(output.get(1).getExpectedPublicIpv4());
    }

    @Test(expected = IllegalArgumentException.class)
    public void corruptRecordDoesNotSilentlyDisappear() {
        TvGatewayCodec.decode("BSZTTV1\\ncorrupt");
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateTunerIdentityIsRejected() {
        TvGatewayProfile same = new TvGatewayProfile("u39.romek", "Romek",
                "0123456789abcdef", "10.147.17.5", null);
        TvGatewayCodec.encode(Arrays.asList(same, same));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownSchemaVersionIsRejected() {
        TvGatewayCodec.decode("BSZTTV99\\n");
    }
}
