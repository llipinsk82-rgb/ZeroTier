package net.kaaass.zerotierfix.service;

import net.kaaass.zerotierfix.BuildConfig;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CandidateBuildTypeTest {
    @Test
    public void tvCandidateMustNotBeDebuggable() {
        assertEquals("candidate", BuildConfig.BUILD_TYPE);
        assertFalse("packet diagnostics must not run in the installed TV candidate",
                BuildConfig.DEBUG);
    }
}
