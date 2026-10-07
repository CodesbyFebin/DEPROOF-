package com.deproof.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private val Context.revocationDataStore: DataStore<Preferences> by preferencesDataStore(name = "revocations")

/**
 * Represents a session revocation event.
 */
data class SessionRevocation(
    val sessionId: String,
    val reason: String, // EXPLICIT, COMPROMISED, SCOPE_VIOLATION, POLICY_CHANGE, TOKEN_EXPIRED
    val timestamp: Long, // Unix milliseconds
    val evidence: String, // Hex-encoded signature
    val revokedBy: String // Node authority
)

/**
 * Manages revocation polling and synchronization with the node agent.
 */
class RevocationPoller(private val context: Context, private val nodeAgentLogDir: String) {
    private val tag = "RevocationPoller"
    private val isRunning = AtomicBoolean(false)
    private val revocationFile = File(nodeAgentLogDir, "revocations.log")

    private val lastSyncTimeKey = stringPreferencesKey("last_sync_time")
    private val knownRevocationsKey = stringPreferencesKey("known_revocations")

    /**
     * Starts polling for revocations from the node agent's log file.
     * Runs in background and notifies via stateFlow when changes are detected.
     */
    fun startPolling(pollingIntervalMs: Long = 5000) {
        if (isRunning.getAndSet(true)) {
            Log.d(tag, "Polling already running")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            Log.d(tag, "Starting revocation polling")
            try {
                while (isRunning.get()) {
                    checkForNewRevocations()
                    delay(pollingIntervalMs)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error during polling", e)
                isRunning.set(false)
            }
        }
    }

    /**
     * Stops the polling loop.
     */
    fun stopPolling() {
        isRunning.set(false)
        Log.d(tag, "Revocation polling stopped")
    }

    /**
     * Checks the revocation log file for new entries.
     */
    private suspend fun checkForNewRevocations() {
        if (!revocationFile.exists()) {
            return
        }

        try {
            val lastSyncTime = getLastSyncTime()
            val revocations = readRevocationLog()

            // Filter to new revocations since last sync
            val newRevocations = revocations.filter { it.timestamp > lastSyncTime }

            if (newRevocations.isNotEmpty()) {
                Log.d(tag, "Found ${newRevocations.size} new revocations")
                newRevocations.forEach { rev ->
                    handleRevocation(rev)
                }
                updateLastSyncTime(System.currentTimeMillis())
            }
        } catch (e: Exception) {
            Log.e(tag, "Error checking revocations", e)
        }
    }

    /**
     * Reads and parses the revocation log file.
     */
    private fun readRevocationLog(): List<SessionRevocation> {
        val revocations = mutableListOf<SessionRevocation>()

        try {
            revocationFile.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    if (line.isNotBlank()) {
                        try {
                            val rev = parseRevocationLine(line)
                            if (rev != null) {
                                revocations.add(rev)
                            }
                        } catch (e: Exception) {
                            Log.w(tag, "Failed to parse revocation line: $line", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error reading revocation log", e)
        }

        return revocations
    }

    /**
     * Parses a single JSON line from the revocation log.
     */
    private fun parseRevocationLine(line: String): SessionRevocation? {
        return try {
            // Parse JSON manually to avoid dependency on kotlinx.serialization
            val json = line.trim()
            if (!json.startsWith("{") || !json.endsWith("}")) {
                return null
            }

            // Simple JSON parsing for revocation entries
            val map = parseJson(json)
            SessionRevocation(
                sessionId = map["sessionId"] as? String ?: return null,
                reason = map["reason"] as? String ?: return null,
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: return null,
                evidence = map["evidence"] as? String ?: return null,
                revokedBy = map["revokedBy"] as? String ?: return null
            )
        } catch (e: Exception) {
            Log.w(tag, "Failed to parse revocation", e)
            null
        }
    }

    /**
     * Simple JSON parser for revocation entries (avoids external dependencies).
     */
    private fun parseJson(json: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val content = json.substring(1, json.length - 1) // Remove { }

        var current = ""
        var inQuote = false
        var key: String? = null
        var value = ""
        var inValue = false

        for (i in content.indices) {
            val char = content[i]

            when {
                char == '"' -> inQuote = !inQuote
                char == ':' && !inQuote && !inValue -> {
                    key = current.trim().trim('"')
                    current = ""
                    inValue = true
                }
                char == ',' && !inQuote && inValue -> {
                    value = current.trim()
                    if (key != null) {
                        map[key] = parseValue(value)
                    }
                    current = ""
                    inValue = false
                    key = null
                }
                else -> current += char
            }
        }

        // Handle last entry
        if (key != null && current.isNotBlank()) {
            value = current.trim()
            map[key] = parseValue(value)
        }

        return map
    }

    /**
     * Parse a JSON value string.
     */
    private fun parseValue(value: String): Any? {
        return when {
            value == "null" -> null
            value == "true" -> true
            value == "false" -> false
            value.startsWith("\"") && value.endsWith("\"") -> value.substring(1, value.length - 1)
            value.toLongOrNull() != null -> value.toLong()
            value.toDoubleOrNull() != null -> value.toDouble()
            else -> value
        }
    }

    /**
     * Handles a detected revocation.
     */
    private suspend fun handleRevocation(revocation: SessionRevocation) {
        Log.i(tag, "Processing revocation for session ${revocation.sessionId}: ${revocation.reason}")

        // Update local state
        updateKnownRevocation(revocation)

        // Clear local wallet session if it matches
        clearWalletSessionIfMatching(revocation.sessionId)

        // Notify listeners (via DataStore)
        context.revocationDataStore.edit { prefs ->
            prefs[stringPreferencesKey("latest_revocation_${revocation.sessionId}")] =
                "${revocation.reason}@${revocation.timestamp}"
        }
    }

    /**
     * Clears the wallet session if it matches the revoked session.
     */
    private suspend fun clearWalletSessionIfMatching(revokedSessionId: String) {
        try {
            context.revocationDataStore.edit { prefs ->
                val currentSession = prefs[stringPreferencesKey("current_session_id")]
                if (currentSession == revokedSessionId) {
                    Log.i(tag, "Clearing wallet session due to revocation")
                    prefs.remove(stringPreferencesKey("current_session_id"))
                    prefs.remove(stringPreferencesKey("wallet_connection"))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error clearing wallet session", e)
        }
    }

    /**
     * Updates the known revocations list.
     */
    private suspend fun updateKnownRevocation(revocation: SessionRevocation) {
        context.revocationDataStore.edit { prefs ->
            val current = prefs[knownRevocationsKey] ?: ""
            val updated = if (current.isBlank()) {
                revocation.sessionId
            } else {
                "$current,${revocation.sessionId}"
            }
            prefs[knownRevocationsKey] = updated
        }
    }

    /**
     * Gets the last sync time from DataStore.
     */
    private suspend fun getLastSyncTime(): Long {
        return context.revocationDataStore.data.map { prefs ->
            prefs[lastSyncTimeKey]?.toLongOrNull() ?: 0L
        }.first()
    }

    /**
     * Updates the last sync time in DataStore.
     */
    private suspend fun updateLastSyncTime(time: Long) {
        context.revocationDataStore.edit { prefs ->
            prefs[lastSyncTimeKey] = time.toString()
        }
    }

    /**
     * Gets a flow of revocation events for a specific session.
     */
    fun getRevocationFlow(sessionId: String): Flow<String?> {
        return context.revocationDataStore.data.map { prefs ->
            prefs[stringPreferencesKey("latest_revocation_$sessionId")]
        }
    }

    /**
     * Checks if a session is revoked.
     */
    suspend fun isSessionRevoked(sessionId: String): Boolean {
        return context.revocationDataStore.data.map { prefs ->
            prefs[stringPreferencesKey("latest_revocation_$sessionId")] != null
        }.first()
    }

    /**
     * Gets the revocation reason for a session.
     */
    suspend fun getRevocationReason(sessionId: String): String? {
        return context.revocationDataStore.data.map { prefs ->
            prefs[stringPreferencesKey("latest_revocation_$sessionId")]?.split("@")?.getOrNull(0)
        }.first()
    }
}
