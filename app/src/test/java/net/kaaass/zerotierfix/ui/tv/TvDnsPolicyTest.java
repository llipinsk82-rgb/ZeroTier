package net.kaaass.zerotierfix.ui.tv;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import android.preference.PreferenceManager;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TvDnsPolicyTest {
    @Test public void verifiedTunersHaveIndependentLocalDns() {
        assertEquals("192.168.18.5", TvDnsPolicy.defaultDns("#5"));
        assertEquals("192.168.100.153", TvDnsPolicy.defaultDns("#9"));
        assertEquals("194.204.152.34", TvDnsPolicy.defaultDns("#23"));
    }

    @Test public void unknownTunersNeverFallBackToPublicResolver() {
        assertNull(TvDnsPolicy.defaultDns("unknown"));
    }

    @Test public void runtimeDnsUsesSelectedTunerWithoutHiddenGlobalFallback() {
        var context = RuntimeEnvironment.getApplication();
        var prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
        assertEquals("192.168.18.5", TvNetworkStore.dnsForTuner(context, "#5"));
        assertEquals("194.204.152.34", TvNetworkStore.dnsForTuner(context, "#23"));
        assertNull(TvNetworkStore.dnsForTuner(context, "unknown"));
        prefs.edit().putString("blackserv_tv_dns_profile_#5", "192.168.18.6").commit();
        assertEquals("192.168.18.6", TvNetworkStore.dnsForTuner(context, "#5"));
    }

    @Test public void dnsSettingsMustBeLiteralIp() {
        assertEquals("192.168.18.5", TvDnsPolicy.validate(" 192.168.18.5 "));
        try {
            TvDnsPolicy.validate("dns.google");
            fail("DNS must not be resolved through UK ISP");
        } catch (IllegalArgumentException expected) { }
    }
}
