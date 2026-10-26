package com.markvoronin.callblocker

import android.telecom.Connection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreeningDecisionTest {
    @Test
    fun verifiedCallerIsAllowedWhenEnabled() {
        assertTrue(isAllowed(true, Connection.VERIFICATION_STATUS_PASSED))
    }

    @Test
    fun verifiedCallerIsBlockedWhenDisabled() {
        assertFalse(isAllowed(false, Connection.VERIFICATION_STATUS_PASSED))
    }

    @Test
    fun unverifiedCallerIsBlocked() {
        assertFalse(isAllowed(true, Connection.VERIFICATION_STATUS_NOT_VERIFIED))
    }

    @Test
    fun spoofedCallerIsBlocked() {
        assertFalse(isAllowed(true, Connection.VERIFICATION_STATUS_FAILED))
    }
}
