package com.example.domain

import org.junit.Test
import org.junit.Assert.*

// EF019 matcher logic. Device and prover-network checks are outside this test.
class ProofJobTest {
    private val caps = ProverCapabilities(
        backends = setOf("gnark-groth16"),
        circuitVersions = mapOf("cubic" to setOf("1", "2")),
        memoryMiB = 4096, cpuCores = 4)
    private val job = ProofJobRequirement("gnark-groth16", "cubic", "2", 2048, 2)

    @Test fun compatibleJobMatchesWithNoMismatch() {
        val m = matchProofJob(job, caps)
        assertTrue(m.compatible)
        assertTrue(m.mismatches.isEmpty())
    }

    @Test fun eachMismatchIsReportedExplicitly() {
        assertEquals(listOf("BACKEND_NOT_SUPPORTED"), matchProofJob(job.copy(backend = "other"), caps).mismatches)
        assertEquals(listOf("CIRCUIT_NOT_SUPPORTED"), matchProofJob(job.copy(circuit = "unknown"), caps).mismatches)
        assertEquals(listOf("CIRCUIT_VERSION_NOT_SUPPORTED"), matchProofJob(job.copy(circuitVersion = "9"), caps).mismatches)
        assertEquals(listOf("INSUFFICIENT_MEMORY"), matchProofJob(job.copy(minMemoryMiB = 8192), caps).mismatches)
        assertEquals(listOf("INSUFFICIENT_CPU"), matchProofJob(job.copy(minCpuCores = 8), caps).mismatches)
    }

    @Test fun incompatibleJobIsNeverReportedAsCompatible() {
        val m = matchProofJob(job.copy(backend = "other", minCpuCores = 8), caps)
        assertFalse(m.compatible)
        assertEquals(listOf("BACKEND_NOT_SUPPORTED", "INSUFFICIENT_CPU"), m.mismatches)
    }

    @Test fun negativeRequirementsAreRefused() {
        try { matchProofJob(job.copy(minMemoryMiB = -1), caps); fail("negative memory accepted") }
        catch (e: Failure) { assertEquals("BAD_REQUIREMENT", e.code) }
    }
}
