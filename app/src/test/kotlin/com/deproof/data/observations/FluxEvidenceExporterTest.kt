import kotlinx.coroutines.runBlocking
package com.deproof.data.observations

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for Flux evidence export
 */
class FluxEvidenceExporterTest {

    private val exporter = FluxEvidenceExporter

    private val sampleObservation = FluxObservation(
        schema = "deproof-flux-observation-v1",
        provider = "Flux",
        source = "operator-flux-node",
        timestamp = System.currentTimeMillis(),
        assurance = "LOCAL_OBSERVATION",
        sourceSha256 = "e908a7da76643f008927ce7e28170d05bd25e6f94f33cad8fed853d560a6d6ea",
        endpoint = "192.168.1.100:16110",
        rewardAsset = "FLUX",
        rewardStatus = "PENDING",
        nodeMetrics = NodeMetrics(
            nodeId = "flux-node-001",
            tier = "Cumulus",
            benchmarkScore = 85000,
            uptime = 1234567,
            cpuUsage = 45.2,
            memoryUsage = 62.8,
            storageUsage = 78.5,
            networkBandwidth = 100,
            collateralStatus = "LOCKED"
        ),
        nodeSha256 = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"
    )

    // ===== JSON Export =====

    @Test
    fun testExportAsJsonIsValid() = runBlocking {
        val json = exporter.exportAsJson(sampleObservation)

        assertNotNull(json)
        assertTrue(json.isNotEmpty())

        // Parse to verify it's valid JSON
        val obj = JSONObject(json)
        assertNotNull(obj)
    }

    @Test
    fun testJsonExportIncludesType() = runBlocking {
        val json = exporter.exportAsJson(sampleObservation)
        val obj = JSONObject(json)

        assertEquals("flux-node-observation", obj.getString("type"))
    }

    @Test
    fun testJsonExportIncludesProvider() = runBlocking {
        val json = exporter.exportAsJson(sampleObservation)
        val obj = JSONObject(json)

        assertEquals("Flux", obj.getString("provider"))
    }

    @Test
    fun testJsonExportIncludesNodeId() = runBlocking {
        val json = exporter.exportAsJson(sampleObservation)
        val obj = JSONObject(json)

        assertEquals("flux-node-001", obj.getString("nodeId"))
    }

    @Test
    fun testJsonExportIncludesEndpoint() = runBlocking {
        val json = exporter.exportAsJson(sampleObservation)
        val obj = JSONObject(json)

        assertEquals("192.168.1.100:16110", obj.getString("endpoint"))
    }

    @Test
    fun testJsonExportIncludesTimestamp() = runBlocking {
        val json = exporter.exportAsJson(sampleObservation)
        val obj = JSONObject(json)

        assertTrue(obj.has("timestamp"))
        assertTrue(obj.getLong("timestamp") > 0)
    }

    @Test
    fun testJsonExportIncludesISOTimestamp() = runBlocking {
        val json = exporter.exportAsJson(sampleObservation)
        val obj = JSONObject(json)

        assertTrue(obj.has("timestampISO"))
        val iso = obj.getString("timestampISO")
        assertTrue(iso.contains("T"))  // ISO format includes T
        assertTrue(iso.contains("Z"))  // UTC timezone
    }

    @Test
    fun testJsonExportIncludesAuditTrail() = runBlocking {
        val json = exporter.exportAsJson(sampleObservation)
        val obj = JSONObject(json)

        assertTrue(obj.has("audit"))
        val audit = obj.getJSONObject("audit")
        assertEquals("e908a7da76643f008927ce7e28170d05bd25e6f94f33cad8fed853d560a6d6ea", audit.getString("sourceDigest"))
        assertEquals("SHA-256", audit.getString("digestAlgorithm"))
        assertEquals("a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6", audit.getString("responseDigest"))
    }

    @Test
    fun testJsonExportIncludesMetrics() = runBlocking {
        val json = exporter.exportAsJson(sampleObservation)
        val obj = JSONObject(json)

        assertTrue(obj.has("nodeMetrics"))
        val metrics = obj.getJSONObject("nodeMetrics")
        assertEquals("Cumulus", metrics.getString("tier"))
        assertEquals(85000, metrics.getLong("benchmarkScore"))
        assertEquals(1234567, metrics.getLong("uptime_seconds"))
    }

    @Test
    fun testJsonExportIncludesRewardTracking() = runBlocking {
        val json = exporter.exportAsJson(sampleObservation)
        val obj = JSONObject(json)

        assertTrue(obj.has("rewardTracking"))
        val rewards = obj.getJSONObject("rewardTracking")

        val flux = rewards.getJSONObject("fluxReward")
        assertEquals("FLUX", flux.getString("asset"))
        assertEquals("Flux", flux.getString("blockchain"))

        val skr = rewards.getJSONObject("skrPayment")
        assertEquals("SKR", skr.getString("asset"))
        assertEquals("Solana", skr.getString("blockchain"))
        assertEquals("NOT_SUBMITTED", skr.getString("status"))
    }

    @Test
    fun testJsonExportIncludesDisclaimer() = runBlocking {
        val json = exporter.exportAsJson(sampleObservation)
        val obj = JSONObject(json)

        assertTrue(obj.has("disclaimer"))
        val disclaimer = obj.getString("disclaimer")
        assertTrue(disclaimer.contains("node health observation"))
        assertTrue(disclaimer.contains("NOT constitute independent proof"))
    }

    // ===== CSV Export =====

    @Test
    fun testExportAsCSVIsValid() = runBlocking {
        val csv = exporter.exportAsCSV(listOf(sampleObservation))

        assertNotNull(csv)
        assertTrue(csv.isNotEmpty())
        assertTrue(csv.contains("timestamp"))
    }

    @Test
    fun testCSVExportHasHeader() = runBlocking {
        val csv = exporter.exportAsCSV(listOf(sampleObservation))
        val lines = csv.split("\n")

        assertTrue(lines[0].contains("timestamp"))
        assertTrue(lines[0].contains("nodeId"))
        assertTrue(lines[0].contains("cpuUsage"))
    }

    @Test
    fun testCSVExportIncludesData() = runBlocking {
        val csv = exporter.exportAsCSV(listOf(sampleObservation))
        val lines = csv.split("\n")

        assertTrue(lines.size >= 2)  // Header + at least 1 data row
        assertTrue(lines[1].contains("flux-node-001"))
        assertTrue(lines[1].contains("Cumulus"))
    }

    @Test
    fun testCSVExportMultipleObservations() = runBlocking {
        val obs1 = sampleObservation
        val obs2 = sampleObservation.copy(
            nodeMetrics = sampleObservation.nodeMetrics.copy(
                cpuUsage = 50.0
            )
        )

        val csv = exporter.exportAsCSV(listOf(obs1, obs2))
        val lines = csv.split("\n")

        assertEquals(3, lines.size)  // Header + 2 data rows
    }

    // ===== Disclaimer Export =====

    @Test
    fun testExportWithDisclaimerIncludesJson() = runBlocking {
        val export = exporter.exportWithDisclaimer(sampleObservation)

        assertTrue(export.contains("DISCLAIMER"))
        assertTrue(export.contains("flux-node-observation"))
        assertTrue(export.contains("OBSERVATION DATA"))
    }

    @Test
    fun testExportWithDisclaimerIncludesAuditTrail() = runBlocking {
        val export = exporter.exportWithDisclaimer(sampleObservation)

        assertTrue(export.contains("AUDIT TRAIL"))
        assertTrue(export.contains("Endpoint"))
        assertTrue(export.contains("Response Digest"))
        assertTrue(export.contains("Timestamp"))
    }

    @Test
    fun testExportWithDisclaimerIncludesAssetSeparation() = runBlocking {
        val export = exporter.exportWithDisclaimer(sampleObservation)

        assertTrue(export.contains("ASSET SEPARATION"))
        assertTrue(export.contains("FLUX rewards"))
        assertTrue(export.contains("SKR payments"))
        assertTrue(export.contains("tracked independently"))
    }

    // ===== Error Handling =====

    @Test
    fun testCSVExportWithEmptyList() = runBlocking {
        val csv = exporter.exportAsCSV(emptyList())

        assertNotNull(csv)
        assertTrue(csv.contains("timestamp"))  // Header should be present
    }
}
