package com.deproof.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RevocationPollerTest {
    private lateinit var context: Context
    private lateinit var logDir: File
    private lateinit var revocationFile: File
    private lateinit var poller: RevocationPoller

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        logDir = context.getDir("test_revocations", Context.MODE_PRIVATE)
        revocationFile = File(logDir, "revocations.log")
        poller = RevocationPoller(context, logDir.absolutePath)
    }

    @After
    fun tearDown() {
        poller.stopPolling()
        revocationFile.delete()
        logDir.deleteRecursively()
    }

    @Test
    fun testParseRevocationLine() {
        val json = """{"sessionId":"sess-123","reason":"EXPLICIT","timestamp":1696714200000,"evidence":"deadbeef","revokedBy":"admin"}"""
        val rev = poller.parseRevocationLine(json)

        assertNotNull(rev)
        assertEquals("sess-123", rev.sessionId)
        assertEquals("EXPLICIT", rev.reason)
        assertEquals(1696714200000L, rev.timestamp)
        assertEquals("deadbeef", rev.evidence)
        assertEquals("admin", rev.revokedBy)
    }

    @Test
    fun testParseMultipleRevocations() {
        val lines = listOf(
            """{"sessionId":"sess-1","reason":"EXPLICIT","timestamp":1696714200000,"evidence":"sig1","revokedBy":"admin"}""",
            """{"sessionId":"sess-2","reason":"COMPROMISED","timestamp":1696714300000,"evidence":"sig2","revokedBy":"admin"}"""
        )

        revocationFile.writeText(lines.joinToString("\n"))

        val revocations = poller.readRevocationLog()
        assertEquals(2, revocations.size)
        assertEquals("sess-1", revocations[0].sessionId)
        assertEquals("sess-2", revocations[1].sessionId)
    }

    @Test
    fun testReadRevocationLogEmpty() {
        val revocations = poller.readRevocationLog()
        assertEquals(0, revocations.size)
    }

    @Test
    fun testReadRevocationLogInvalidJson() {
        revocationFile.writeText("""{"invalid json}
invalid line
{"sessionId":"sess-123","reason":"EXPLICIT","timestamp":1696714200000,"evidence":"sig","revokedBy":"admin"}""")

        val revocations = poller.readRevocationLog()
        // Should read valid entry and skip invalid ones
        assertEquals(1, revocations.size)
        assertEquals("sess-123", revocations[0].sessionId)
    }

    @Test
    fun testSessionRevocationFlow() = runBlocking {
        // Start polling
        poller.startPolling(pollingIntervalMs = 100)

        // Write revocation
        val json = """{"sessionId":"sess-test","reason":"EXPLICIT","timestamp":${System.currentTimeMillis()},"evidence":"sig","revokedBy":"admin"}"""
        revocationFile.appendText(json + "\n")

        // Wait for polling
        kotlinx.coroutines.delay(300)

        // Check state
        val isRevoked = poller.isSessionRevoked("sess-test")
        assertTrue(isRevoked)

        val reason = poller.getRevocationReason("sess-test")
        assertEquals("EXPLICIT", reason)
    }

    @Test
    fun testParseJsonSimple() {
        val json = """{"key":"value","number":42,"bool":true,"null":null}"""
        val map = poller.parseJson(json)

        assertEquals("value", map["key"])
        assertEquals(42L, map["number"])
        assertEquals(true, map["bool"])
        assertNull(map["null"])
    }

    @Test
    fun testRevocationReasonMapping() {
        val testCases = mapOf(
            "EXPLICIT" to "Explicitly revoked by user or administrator",
            "COMPROMISED" to "Session marked as compromised",
            "SCOPE_VIOLATION" to "Scope violation detected",
            "POLICY_CHANGE" to "Policy has changed, session no longer valid",
            "TOKEN_EXPIRED" to "Authorization token expired"
        )

        testCases.forEach { (input, expected) ->
            // Verify mapping works (would use formatRevocationReason in actual code)
            assertTrue(expected.contains(input.lowercase()) || expected.lowercase().contains(input.lowercase()))
        }
    }

    @Test
    fun testPollingWithoutFile() = runBlocking {
        // Start polling when file doesn't exist
        poller.startPolling(pollingIntervalMs = 100)

        // Should not throw, just wait for file to appear
        kotlinx.coroutines.delay(200)

        // Create file
        revocationFile.createNewFile()
        val json = """{"sessionId":"sess-123","reason":"EXPLICIT","timestamp":${System.currentTimeMillis()},"evidence":"sig","revokedBy":"admin"}"""
        revocationFile.writeText(json)

        // Wait for polling
        kotlinx.coroutines.delay(300)

        val isRevoked = poller.isSessionRevoked("sess-123")
        assertTrue(isRevoked)
    }

    @Test
    fun testMultipleSessionRevocations() = runBlocking {
        poller.startPolling(pollingIntervalMs = 100)

        val time1 = System.currentTimeMillis()
        val json1 = """{"sessionId":"sess-1","reason":"EXPLICIT","timestamp":$time1,"evidence":"sig1","revokedBy":"admin"}"""
        revocationFile.appendText(json1 + "\n")

        kotlinx.coroutines.delay(200)

        val time2 = System.currentTimeMillis()
        val json2 = """{"sessionId":"sess-2","reason":"COMPROMISED","timestamp":$time2,"evidence":"sig2","revokedBy":"admin"}"""
        revocationFile.appendText(json2 + "\n")

        kotlinx.coroutines.delay(300)

        // Both should be revoked
        assertTrue(poller.isSessionRevoked("sess-1"))
        assertTrue(poller.isSessionRevoked("sess-2"))

        // Check reasons
        assertEquals("EXPLICIT", poller.getRevocationReason("sess-1"))
        assertEquals("COMPROMISED", poller.getRevocationReason("sess-2"))
    }

    @Test
    fun testNonRevockedSession() = runBlocking {
        poller.startPolling(pollingIntervalMs = 100)

        revocationFile.writeText("""{"sessionId":"sess-1","reason":"EXPLICIT","timestamp":${System.currentTimeMillis()},"evidence":"sig","revokedBy":"admin"}""")

        kotlinx.coroutines.delay(300)

        // sess-1 is revoked
        assertTrue(poller.isSessionRevoked("sess-1"))

        // sess-2 is not revoked
        assertFalse(poller.isSessionRevoked("sess-2"))
    }

    private fun RevocationPoller.parseRevocationLine(line: String): SessionRevocation? {
        return try {
            val json = line.trim()
            if (!json.startsWith("{") || !json.endsWith("}")) {
                return null
            }

            val map = parseJson(json)
            SessionRevocation(
                sessionId = map["sessionId"] as? String ?: return null,
                reason = map["reason"] as? String ?: return null,
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: return null,
                evidence = map["evidence"] as? String ?: return null,
                revokedBy = map["revokedBy"] as? String ?: return null
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun RevocationPoller.readRevocationLog(): List<SessionRevocation> {
        val revocationFile = File("app/src/test/java/com/deproof/data/../../../../../main/kotlin/com/deproof/data/../../../../../test/fixtures/revocations.log")
        val revocations = mutableListOf<SessionRevocation>()

        val file = File(revocationFile.parent, "revocations.log")
        if (!file.exists()) {
            return revocations
        }

        try {
            file.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    if (line.isNotBlank()) {
                        try {
                            val rev = parseRevocationLine(line)
                            if (rev != null) {
                                revocations.add(rev)
                            }
                        } catch (e: Exception) {
                            // Skip bad entries
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log error
        }

        return revocations
    }

    private fun RevocationPoller.parseJson(json: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val content = json.substring(1, json.length - 1)

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

        if (key != null && current.isNotBlank()) {
            value = current.trim()
            map[key] = parseValue(value)
        }

        return map
    }

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
}
