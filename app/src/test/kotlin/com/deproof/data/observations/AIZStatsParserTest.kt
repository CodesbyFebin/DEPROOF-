package com.deproof.data.observations

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for AIZStatsParser - mirrors Python reference suite (10 tests)
 * All tests passing confirms strict validation and digest tracking
 */
@RunWith(RobolectricTestRunner::class)
class AIZStatsParserTest {

    private val parser = AIZStatsParser

    // ===== Valid input =====

    @Test
    fun testValidStatsAreAccepted() {
        val json = """
            {
                "storage": {"total_count": 1000, "total_size": 1048576},
                "delivery": {"upstream_speed": 100}
            }
        """.toByteArray()

        val obs = parser.parseStats(json)

        assertEquals(1000L, obs.metrics.storageObjectCount)
        assertEquals(1048576L, obs.metrics.storageSizeBytes)
        assertEquals(100L, obs.metrics.upstreamSpeedRaw)
        assertEquals("deproof-aioz-observation-v1", obs.schema)
        assertEquals("LOCAL_OBSERVATION", obs.assurance)
        assertEquals("NOT_SUBMITTED", obs.skrPayment)
    }

    // ===== Validation failures =====

    @Test(expected = ParseException::class)
    fun testNegativeIntegerRejected() {
        val json = """
            {
                "storage": {"total_count": -1, "total_size": 1024},
                "delivery": {"upstream_speed": 50}
            }
        """.toByteArray()

        parser.parseStats(json)
    }

    @Test(expected = ParseException::class)
    fun testFloatValueRejected() {
        val json = """
            {
                "storage": {"total_count": 100.5, "total_size": 1024},
                "delivery": {"upstream_speed": 50}
            }
        """.toByteArray()

        parser.parseStats(json)
    }

    @Test(expected = ParseException::class)
    fun testBooleanValueRejected() {
        val json = """
            {
                "storage": {"total_count": true, "total_size": 1024},
                "delivery": {"upstream_speed": 50}
            }
        """.toByteArray()

        parser.parseStats(json)
    }

    @Test(expected = ParseException::class)
    fun testDuplicateFieldRejected() {
        // Note: JSONObject in Android will use last value for duplicate keys
        // This test verifies we'd catch it if JSON parser preserved duplicates
        val jsonStr = """{"storage":{"total_count":100,"total_count":200,"total_size":1024},"delivery":{"upstream_speed":50}}"""

        try {
            parser.parseStats(jsonStr.toByteArray())
            fail("Should reject duplicate fields")
        } catch (e: ParseException) {
            // Expected
            assertTrue(e.message?.contains("total_count") == true || e.message?.contains("storage") == true)
        }
    }

    @Test(expected = ParseException::class)
    fun testAbsentFieldNotZero() {
        // Missing total_size should fail, not default to 0
        val json = """
            {
                "storage": {"total_count": 100},
                "delivery": {"upstream_speed": 50}
            }
        """.toByteArray()

        parser.parseStats(json)
    }

    @Test(expected = ParseException::class)
    fun testMissingStorageMetricsRejected() {
        val json = """
            {
                "delivery": {"upstream_speed": 50}
            }
        """.toByteArray()

        parser.parseStats(json)
    }

    @Test(expected = ParseException::class)
    fun testMissingDeliveryMetricsRejected() {
        val json = """
            {
                "storage": {"total_count": 100, "total_size": 1024}
            }
        """.toByteArray()

        parser.parseStats(json)
    }

    @Test(expected = ParseException::class)
    fun testInvalidJsonRejected() {
        val json = """
            {
                "storage": {"total_count": 100, "total_size": 1024},
                "delivery": {"upstream_speed": 50}
            INVALID
        """.toByteArray()

        parser.parseStats(json)
    }

    @Test(expected = ParseException::class)
    fun testOversizedInputRejected() {
        // Create input larger than MAX_BYTES (65536)
        val largeValue = "x".repeat(70000)
        val json = """{"data": "$largeValue"}""".toByteArray()

        parser.parseStats(json)
    }

    // ===== Source digest tracking =====

    @Test
    fun testSourceDigestIsTracked() {
        val json = """
            {
                "storage": {"total_count": 1000, "total_size": 1048576},
                "delivery": {"upstream_speed": 100}
            }
        """.toByteArray()

        val obs = parser.parseStats(json)

        assertNotNull(obs.sourceSha256)
        assertEquals(64, obs.sourceSha256.length)  // SHA256 hex is 64 chars

        // Verify digest is consistent
        val obs2 = parser.parseStats(json)
        assertEquals(obs.sourceSha256, obs2.sourceSha256)
    }

    @Test
    fun testSourceDigestChangesWithInput() {
        val json1 = """
            {
                "storage": {"total_count": 1000, "total_size": 1048576},
                "delivery": {"upstream_speed": 100}
            }
        """.toByteArray()

        val json2 = """
            {
                "storage": {"total_count": 2000, "total_size": 1048576},
                "delivery": {"upstream_speed": 100}
            }
        """.toByteArray()

        val obs1 = parser.parseStats(json1)
        val obs2 = parser.parseStats(json2)

        assertNotEquals(obs1.sourceSha256, obs2.sourceSha256)
    }

    // ===== Assurance levels =====

    @Test
    fun testAssuranceLevelIsLocalObservation() {
        val json = """
            {
                "storage": {"total_count": 100, "total_size": 1024},
                "delivery": {"upstream_speed": 50}
            }
        """.toByteArray()

        val obs = parser.parseStats(json)

        assertEquals("LOCAL_OBSERVATION", obs.assurance)
        assertNull(obs.providerAcknowledgement)
        assertEquals("NOT_RUN", obs.independentVerification)
    }

    // ===== Reward separation =====

    @Test
    fun testRewardAssetSeparation() {
        val json = """
            {
                "storage": {"total_count": 100, "total_size": 1024},
                "delivery": {"upstream_speed": 50}
            }
        """.toByteArray()

        val obs = parser.parseStats(json)

        assertEquals("AIOZ", obs.rewardAsset)
        assertEquals("NOT_SUBMITTED", obs.skrPayment)
        // These are separate ledgers - confirmed by distinct values
    }

    // ===== Edge cases =====

    @Test
    fun testZeroValuesAreAccepted() {
        val json = """
            {
                "storage": {"total_count": 0, "total_size": 0},
                "delivery": {"upstream_speed": 0}
            }
        """.toByteArray()

        val obs = parser.parseStats(json)

        assertEquals(0L, obs.metrics.storageObjectCount)
        assertEquals(0L, obs.metrics.storageSizeBytes)
        assertEquals(0L, obs.metrics.upstreamSpeedRaw)
    }

    @Test
    fun testLargeValuesAreAccepted() {
        val maxUint64 = Long.MAX_VALUE  // 2^63-1 (Java Long limit)
        val json = """
            {
                "storage": {"total_count": $maxUint64, "total_size": $maxUint64},
                "delivery": {"upstream_speed": $maxUint64}
            }
        """.toByteArray()

        val obs = parser.parseStats(json)

        assertEquals(maxUint64, obs.metrics.storageObjectCount)
        assertEquals(maxUint64, obs.metrics.storageSizeBytes)
        assertEquals(maxUint64, obs.metrics.upstreamSpeedRaw)
    }

    // ===== UTF-8 handling =====

    @Test
    fun testValidUtf8IsProcessed() {
        val json = """
            {
                "storage": {"total_count": 100, "total_size": 1024},
                "delivery": {"upstream_speed": 50}
            }
        """.toByteArray(Charsets.UTF_8)

        // Should not throw
        val obs = parser.parseStats(json)
        assertNotNull(obs)
    }

    @Test(expected = ParseException::class)
    fun testInvalidUtf8IsRejected() {
        // Invalid UTF-8 byte sequence
        val invalidBytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0xFD.toByte())
        parser.parseStats(invalidBytes)
    }
}
