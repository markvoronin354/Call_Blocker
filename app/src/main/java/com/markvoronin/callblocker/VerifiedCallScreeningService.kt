package com.markvoronin.callblocker

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection

/**
 * Telecom only hands this service calls from numbers that aren't in the user's contacts (the app
 * holds no READ_CONTACTS), so everything arriving here is an unsaved number. Those are blocked
 * unless the carrier verified the caller number via STIR/SHAKEN, the same signal behind the green
 * check in Samsung's Recents.
 */
class VerifiedCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val settings = ScreenerSettings(this)
        val status = callDetails.callerNumberVerificationStatus
        val allowed = isAllowed(settings.allowVerified, status)

        DecisionLog(this).add(
            Decision(
                time = System.currentTimeMillis(),
                number = callDetails.handle?.schemeSpecificPart ?: "Unknown",
                verificationStatus = status,
                allowed = allowed,
            )
        )

        val response = CallResponse.Builder()
        if (!allowed) {
            when (settings.blockAction) {
                BlockAction.REJECT -> response
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipNotification(true)
                BlockAction.SILENCE -> response.setSilenceCall(true)
            }
        }
        respondToCall(callDetails, response.build())
    }
}

/** Only a carrier-verified number can pass, and only if the user allows verified callers. */
internal fun isAllowed(allowVerified: Boolean, verificationStatus: Int): Boolean =
    allowVerified && verificationStatus == Connection.VERIFICATION_STATUS_PASSED
