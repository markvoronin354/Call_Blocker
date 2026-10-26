package com.markvoronin.callblocker

import android.content.Context
import androidx.core.content.edit

enum class BlockAction { REJECT, SILENCE }

/** User-facing options for the screening service, persisted in SharedPreferences. */
class ScreenerSettings(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("screener_settings", Context.MODE_PRIVATE)

    /** Let unsaved numbers through when the carrier has verified them (STIR/SHAKEN). */
    var allowVerified: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_VERIFIED, true)
        set(value) = prefs.edit { putBoolean(KEY_ALLOW_VERIFIED, value) }

    var blockAction: BlockAction
        get() = BlockAction.entries.firstOrNull { it.name == prefs.getString(KEY_ACTION, null) }
            ?: BlockAction.REJECT
        set(value) = prefs.edit { putString(KEY_ACTION, value.name) }

    private companion object {
        const val KEY_ALLOW_VERIFIED = "allow_verified"
        const val KEY_ACTION = "block_action"
    }
}
