package com.deproof.domain.proof

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import com.deproof.data.observations.AIZObservation
import com.deproof.data.observations.FluxObservation
import com.deproof.data.observations.Metrics
import com.deproof.data.observations.NodeMetrics
import com.deproof.data.observations.Observation

class ProofGeneratorTest {

    private val generator = ProofGeneratorWithObservations

    // ===== Valid Proof Generation =====

    @Test
    fun testGenerateProofWithSingleAIZObservation() = runBlocking {
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 42,
                storageSizeBytes = 1048576,
                upstreamSpeedRaw = 1024
            )
        )

        val result = generator.generateProof("device-001", listOf(observation))

        assertTrue(result.isSuccess())
        result.getOrNull()?.let { proof ->
            assertNotNull(proof.proofId)
            assertEquals("deproof-proof-v1", proof.version)
            assertEquals("device-001", proof.deviceId)
            assertEquals(1, proof.observationCount)
            assertEquals(listOf("AIOZ"), proof.providers)
            assertEquals(ProofStatus.GENERATED, proof.status)
            assertTrue(proof.messageHash.length == 64)  // SHA256 hex
        }
    }

    @Test
    fun testGenerateProofWithMultipleObservations() = runBlocking {
        val aizObs = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 100,
                storageSizeBytes = 5242880,
                upstreamSpeedRaw = 2048
            )
        )

        val fluxObs = FluxObservation(
            source = "operator-flux-node",
            sourceSha256 = "xyz789abc123xyz789abc123xyz789abc123xyz789abc123xyz789abc123xyz78",
            endpoint = "192.168.1.100:16127",
            nodeMetrics = NodeMetrics(
                nodeId = "flux-node-001",
                tier = "T1",
                benchmarkScore = 95,
                uptime = 604800,
                cpuUsage = 30.0,
                memoryUsage = 50.0,
                storageUsage = 70.0,
                networkBandwidth = 1000,
                collateralStatus = "LOCKED"
            )
        )

        val result = generator.generateProof("device-multi", listOf(aizObs, fluxObs))

        assertTrue(result.isSuccess())
        result.getOrNull()?.let { proof ->
            assertEquals(2, proof.observationCount)
            assertEquals(listOf("AIOZ", "Flux").sorted(), proof.providers.sorted())
            assertEquals(2, proof.observationDigests.size)
            assertTrue(proof.observationDigests.contains(aizObs.sourceSha256))
            assertTrue(proof.observationDigests.contains(fluxObs.sourceSha256))
        }
    }

    @Test
    fun testProofMessageHashIsConsistent() = runBlocking {
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 42,
                storageSizeBytes = 1048576,
                upstreamSpeedRaw = 1024
            )
        )

        val deviceId = "device-001"
        val result1 = generator.generateProof(deviceId, listOf(observation))
        val result2 = generator.generateProof(deviceId, listOf(observation))

        assertTrue(result1.isSuccess())
        assertTrue(result2.isSuccess())

        val proof1 = result1.getOrNull()
        val proof2 = result2.getOrNull()

        assertNotNull(proof1)
        assertNotNull(proof2)

        proof1?.let { p1 ->
            proof2?.let { p2 ->
                assertEquals(
                    "Same inputs should produce consistent message binding",
                    p1.completeMessageBinding.joinToString(""),
                    p2.completeMessageBinding.joinToString("")
                )
            }
        }
    }

    @Test
    fun testProofImmutabilityAfterCreation() = runBlocking {
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
            )
        )

        val result = generator.generateProof("device-001", listOf(observation))
        assertTrue(result.isSuccess())

        result.getOrNull()?.let { proof ->
            val originalHash = proof.messageHash
            val copy = proof.copy()

            assertEquals(proof, copy)
            assertEquals(originalHash, copy.messageHash)
            assertFalse(proof === copy)  // Different instances
        }
    }

    @Test
    fun testProofVersionCorrect() = runBlocking {
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
            )
        )

        val result = generator.generateProof("device-001", listOf(observation))

        result.getOrNull()?.let { proof ->
            assertEquals("deproof-proof-v1", proof.version)
        }
    }

    @Test
    fun testProofHasUniqueId() = runBlocking {
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
            )
        )

        val result1 = generator.generateProof("device-001", listOf(observation))
        val result2 = generator.generateProof("device-001", listOf(observation))

        assertTrue(result1.isSuccess())
        assertTrue(result2.isSuccess())

        val proof1 = result1.getOrNull()?.proofId
        val proof2 = result2.getOrNull()?.proofId

        assertNotEquals("Each proof should have unique ID", proof1, proof2)
    }

    @Test
    fun testProofTimestampWithinRange() = runBlocking {
        val before = System.currentTimeMillis()
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
            )
        )

        val result = generator.generateProof("device-001", listOf(observation))
        val after = System.currentTimeMillis()

        result.getOrNull()?.let { proof ->
            assertTrue("Proof timestamp should be >= before", proof.timestamp >= before)
            assertTrue("Proof timestamp should be <= after", proof.timestamp <= after)
        }
    }

    // ===== Provider Tracking =====

    @Test
    fun testProvidersListSorted() = runBlocking {
        val fluxObs = FluxObservation(
            source = "operator-flux-node",
            sourceSha256 = "xyz789abc123xyz789abc123xyz789abc123xyz789abc123xyz789abc123xyz78",
            endpoint = "192.168.1.100:16127",
            nodeMetrics = NodeMetrics(
                nodeId = "flux-node-001",
                tier = "T1",
                benchmarkScore = 95,
                uptime = 604800,
                cpuUsage = 30.0,
                memoryUsage = 50.0,
                storageUsage = 70.0,
                networkBandwidth = 1000,
                collateralStatus = "LOCKED"
            )
        )

        val aizObs = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 100,
                storageSizeBytes = 5242880,
                upstreamSpeedRaw = 2048
            )
        )

        val result = generator.generateProof("device-001", listOf(fluxObs, aizObs))

        result.getOrNull()?.let { proof ->
            val expected = listOf("AIOZ", "Flux")
            assertEquals(expected, proof.providers)
        }
    }

    @Test
    fun testProvidersListDeduped() = runBlocking {
        val aizObs1 = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 100,
                storageSizeBytes = 5242880,
                upstreamSpeedRaw = 2048
            )
        )

        val aizObs2 = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "def456abc123def456abc123def456abc123def456abc123def456abc123def45",
            metrics = Metrics(
                storageObjectCount = 50,
                storageSizeBytes = 2621440,
                upstreamSpeedRaw = 1024
            )
        )

        val result = generator.generateProof("device-001", listOf(aizObs1, aizObs2))

        result.getOrNull()?.let { proof ->
            assertEquals(2, proof.observationCount)
            assertEquals(listOf("AIOZ"), proof.providers)
        }
    }

    // ===== Error Handling =====

    @Test
    fun testRejectBlankDeviceId() = runBlocking {
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
            )
        )

        val result = generator.generateProof("", listOf(observation))

        assertTrue(result.isError())
    }

    @Test
    fun testRejectWhitespaceDeviceId() = runBlocking {
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
            )
        )

        val result = generator.generateProof("   ", listOf(observation))

        assertTrue(result.isError())
    }

    @Test
    fun testRejectEmptyObservationsList() = runBlocking {
        val result = generator.generateProof("device-001", emptyList())

        assertTrue(result.isError())
    }

    @Test
    fun testErrorExceptionAccessible() = runBlocking {
        val result = generator.generateProof("", emptyList())

        assertTrue(result.isError())
        assertNotNull((result as? Result.Error)?.exception)
    }

    // ===== Message Binding Tests =====

    @Test
    fun testCompleteMessageBindingIsNotEmpty() = runBlocking {
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 42,
                storageSizeBytes = 1048576,
                upstreamSpeedRaw = 1024
            )
        )

        val result = generator.generateProof("device-001", listOf(observation))

        result.getOrNull()?.let { proof ->
            assertTrue(proof.completeMessageBinding.isNotEmpty())
            assertTrue(proof.completeMessageBinding.size > 0)
        }
    }

    @Test
    fun testMessageHashDerivedFromBinding() = runBlocking {
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
            )
        )

        val result = generator.generateProof("device-001", listOf(observation))

        result.getOrNull()?.let { proof ->
            assertNotNull(proof.messageHash)
            assertTrue(proof.messageHash.length == 64)  // SHA256 hex
            assertFalse(proof.messageHash.isEmpty())
        }
    }

    // ===== Status Tracking =====

    @Test
    fun testNewProofStatusIsGenerated() = runBlocking {
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
            )
        )

        val result = generator.generateProof("device-001", listOf(observation))

        result.getOrNull()?.let { proof ->
            assertEquals(ProofStatus.GENERATED, proof.status)
        }
    }

    @Test
    fun testCreatedAtTimestampSet() = runBlocking {
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
            )
        )

        val before = System.currentTimeMillis()
        val result = generator.generateProof("device-001", listOf(observation))
        val after = System.currentTimeMillis()

        result.getOrNull()?.let { proof ->
            assertTrue(proof.createdAt >= before)
            assertTrue(proof.createdAt <= after)
        }
    }

    // ===== Observation Digests =====

    @Test
    fun testObservationDigestsTracked() = runBlocking {
        val obs1Digest = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1"
        val obs2Digest = "xyz789abc123xyz789abc123xyz789abc123xyz789abc123xyz789abc123xyz78"

        val aizObs = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = obs1Digest,
            metrics = Metrics(
                storageObjectCount = 100,
                storageSizeBytes = 5242880,
                upstreamSpeedRaw = 2048
            )
        )

        val fluxObs = FluxObservation(
            source = "operator-flux-node",
            sourceSha256 = obs2Digest,
            endpoint = "192.168.1.100:16127",
            nodeMetrics = NodeMetrics(
                nodeId = "flux-node-001",
                tier = "T1",
                benchmarkScore = 95,
                uptime = 604800,
                cpuUsage = 30.0,
                memoryUsage = 50.0,
                storageUsage = 70.0,
                networkBandwidth = 1000,
                collateralStatus = "LOCKED"
            )
        )

        val result = generator.generateProof("device-001", listOf(aizObs, fluxObs))

        result.getOrNull()?.let { proof ->
            assertEquals(2, proof.observationDigests.size)
            assertTrue(proof.observationDigests.contains(obs1Digest))
            assertTrue(proof.observationDigests.contains(obs2Digest))
        }
    }

    @Test
    fun testSignatureNullByDefault() = runBlocking {
        val observation = AIZObservation(
            source = "operator-supplied-cli-stats",
            sourceSha256 = "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            metrics = Metrics(
                storageObjectCount = 1,
                storageSizeBytes = 1024,
                upstreamSpeedRaw = 100
            )
        )

        val result = generator.generateProof("device-001", listOf(observation))

        result.getOrNull()?.let { proof ->
            assertNull(proof.signature)
        }
    }
}
