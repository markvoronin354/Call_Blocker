package com.markvoronin.callblocker

import android.content.Context
import android.telecom.Connection
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class Decision(
    val time: Long,
    val number: String,
    val verificationStatus: Int,
    val allowed: Boolean,
) {
    val verificationLabel: String
        get() = when (verificationStatus) {
            Connection.VERIFICATION_STATUS_PASSED -> "Verified"
            Connection.VERIFICATION_STATUS_FAILED -> "Verification failed"
            else -> "Not verified"
        }
}

/** The most recent screening decisions, kept on-device so the app can show what it did. */
class DecisionLog(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("decision_log", Context.MODE_PRIVATE)

    fun add(decision: Decision) {
        val entries = JSONArray().put(decision.toJson())
        val existing = load()
        for (i in 0 until minOf(existing.size, MAX_ENTRIES - 1)) entries.put(existing[i].toJson())
        prefs.edit { putString(KEY_ENTRIES, entries.toString()) }
    }

    /** Newest first. */
    fun load(): List<Decision> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).map { array.getJSONObject(it).toDecision() }
    }

    fun clear() = prefs.edit { remove(KEY_ENTRIES) }

    private fun Decision.toJson() = JSONObject()
        .put("time", time)
        .put("number", number)
        .put("status", verificationStatus)
        .put("allowed", allowed)

    private fun JSONObject.toDecision() =
        Decision(getLong("time"), getString("number"), getInt("status"), getBoolean("allowed"))

    private companion object {
        const val KEY_ENTRIES = "entries"
        const val MAX_ENTRIES = 50
    }
}
