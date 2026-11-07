package net.kaaass.zerotierfix.ui.tv;

import org.junit.Test;
import java.io.IOException;
import java.io.StringReader;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TvExitProbeTest {
    @Test public void parsesShortPlainTextResponse() throws Exception {
        assertEquals("83.12.34.56", TvExitProbe.readResponse(new StringReader(" 83.12.34.56\n")));
    }
    @Test public void rejectsOversizedHtmlResponses() throws Exception {
        try {
            TvExitProbe.readResponse(new StringReader(new String(new char[256]).replace('\0', 'x')));
            fail("Should reject oversized response");
        } catch (IOException expected) {
            assertEquals("Exit IP response too long", expected.getMessage());
        }
    }
}
