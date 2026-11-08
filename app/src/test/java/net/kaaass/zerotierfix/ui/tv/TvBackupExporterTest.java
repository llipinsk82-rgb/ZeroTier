package net.kaaass.zerotierfix.ui.tv;

import org.junit.Test;
import java.util.Date;
import static org.junit.Assert.*;

public class TvBackupExporterTest {
    @Test public void createsUniqueSafeNamesWithoutAnExternalFilePicker() {
        String first = TvBackupExporter.filename(new Date(1_700_000_000_000L));
        String second = TvBackupExporter.filename(new Date(1_700_000_010_000L));
        assertTrue(first.matches("blackserv-zerotier-tv-[0-9]{8}-[0-9]{6}\\.json"));
        assertNotEquals(first, second);
    }
}
