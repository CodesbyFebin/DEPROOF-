package com.example.domain

import org.junit.Test
import org.junit.Assert.*
import java.io.File

// FN059 partial: the dispatcher reads the real connectors/support.json and must
// never report success, freshness or an observation.
class ConnectorTest {
    private val raw = File("../connectors/support.json").readText(Charsets.UTF_8)
    private val registry = parseConnectorSupport(raw)
    private val op = ConnectorRequest("status")
    private fun fails(code: String, block: () -> Unit) { try { block(); fail("Expected $code") } catch (e: Failure) { assertEquals(code, e.code) } }

    @Test fun everyAdapterInTheRealSupportFileIsBlockedWithoutObservation() {
        assertEquals(6, registry.size)
        for (entry in registry) {
            val r = runConnector(registry, entry.id, op)
            assertEquals(entry.id, r.connectorId)
            assertEquals("BLOCKED", r.outcome)
            assertNull(r.observedAt)
            assertEquals("NOT_OBSERVED", r.freshness)
            assertTrue(r.reason.isNotBlank())
        }
    }

    @Test fun reasonCarriesTheRecordedBlockerForBlockedAndUnverifiedAdapters() {
        val staking = runConnector(registry, "skr-staking", op)
        assertTrue(staking.reason.startsWith("BLOCKED: "))
        assertTrue(staking.reason.contains("Versioned verified IDL/layout/state semantics not available"))
        val rpc = runConnector(registry, "solana-rpc", op)
        assertTrue(rpc.reason.startsWith("UNQUALIFIED: "))
        assertTrue(rpc.reason.contains("Live genesis/mint/account/status observations absent"))
        assertEquals("app/src/main/java/com/example/data/Rpc.kt", rpc.source)
    }

    @Test fun qualifiedStatusStillDoesNotProduceASuccessfulResult() {
        val synthetic = listOf(ConnectorEntry("test-adapter", "QUALIFIED", null, null))
        val r = runConnector(synthetic, "test-adapter", op)
        assertEquals("BLOCKED", r.outcome)
        assertTrue(r.reason.startsWith("NO_DISPATCH_IMPLEMENTED"))
        assertNull(r.observedAt)
    }

    @Test fun unknownAdapterAndBadRequestsAreRefused() {
        assertEquals("BLOCKED", runConnector(registry, "no-such-adapter", op).outcome)
        assertTrue(runConnector(registry, "no-such-adapter", op).reason.startsWith("UNKNOWN_CONNECTOR"))
        fails("BAD_OPERATION") { runConnector(registry, "solana-rpc", ConnectorRequest("  ")) }
        fails("BAD_OPERATION") { runConnector(registry, "solana-rpc", ConnectorRequest("x".repeat(65))) }
    }

    @Test fun wrongSchemaIsRejected() {
        fails("CONNECTOR_SCHEMA_MISMATCH") { parseConnectorSupport("{\"schema\":\"other\",\"adapters\":[]}") }
    }
}
