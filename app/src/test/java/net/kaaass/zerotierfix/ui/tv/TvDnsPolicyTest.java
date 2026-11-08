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

    @Test public void controllerDnsIsDefaultAndManualOverrideIsProfileScoped() {
        var context = RuntimeEnvironment.getApplication();
        var prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
        assertTrue(TvNetworkStore.usesControllerDns(context, "#5"));
        assertTrue(TvNetworkStore.usesControllerDns(context, "#23"));
        assertNull(TvNetworkStore.dnsForTuner(context, "#5"));
        assertNull(TvNetworkStore.dnsForTuner(context, "#23"));
        assertNull(TvNetworkStore.dnsForTuner(context, "unknown"));
        prefs.edit().putString("blackserv_tv_dns_profile_#5", "192.168.18.6").commit();
        assertFalse(TvNetworkStore.usesControllerDns(context, "#5"));
        assertEquals("192.168.18.6", TvNetworkStore.dnsForTuner(context, "#5"));
        assertTrue(TvNetworkStore.usesControllerDns(context, "#23"));
    }

    @Test public void presetProvidersAreExplicitManualChoices() {
        assertEquals("1.1.1.1", TvDnsPolicy.providerDns("cloudflare"));
        assertEquals("8.8.8.8", TvDnsPolicy.providerDns("google"));
        assertEquals("194.204.152.34", TvDnsPolicy.providerDns("orange"));
        assertNull(TvDnsPolicy.providerDns("auto"));
    }

    @Test public void dnsSettingsMustBeLiteralIp() {
        assertEquals("192.168.18.5", TvDnsPolicy.validate(" 192.168.18.5 "));
        try {
            TvDnsPolicy.validate("dns.google");
            fail("DNS must not be resolved through UK ISP");
        } catch (IllegalArgumentException expected) { }
    }
}
