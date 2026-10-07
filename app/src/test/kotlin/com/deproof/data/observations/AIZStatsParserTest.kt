package com.deproof.data.observations

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

class AIZStatsParserTest {

    private val parser = AIZStatsParser

    // ===== Valid Input Tests =====

    @Test
    fun testParseValidStats() = runBlocking {
        val input = """
            storage_object_count=42
            storage_size_bytes=1048576
            upstream_speed_kbps=1024
        """.trimIndent().toByteArray()

        val observation = parser.parseStats(input)

        assertNotNull(observation)
        observation?.let {
            assertEquals("AIOZ", it.provider)
            assertEquals("deproof-aioz-observation-v1", it.schema)
            assertEquals("operator-supplied-cli-stats", it.source)
            assertEquals("LOCAL_OBSERVATION", it.assurance)
            assertTrue(it.sourceSha256.length == 64)  // SHA256 hex is 64 chars
        }
    }

    @Test
    fun testParsedMetricsCorrect() = runBlocking {
        val input = """
            storage_object_count=100
            storage_size_bytes=5242880
            upstream_speed_kbps=2048
        """.trimIndent().toByteArray()

        val observation = parser.parseStats(input) as? AIZObservation

        assertNotNull(observation)
        observation?.let {
            assertEquals(100, it.metrics.storageObjectCount)
            assertEquals(5242880, it.metrics.storageSizeBytes)
            assertEquals(2048, it.metrics.upstreamSpeedRaw)
        }
    }

    @Test
    fun testTimestampWithinReasonableRange() = runBlocking {
        val before = System.currentTimeMillis()
        val input = "storage_object_count=1\nstorage_size_bytes=1024\nupstream_speed_kbps=100".toByteArray()
        val observation = parser.parseStats(input)
        val after = System.currentTimeMillis()

        assertNotNull(observation)
        observation?.let {
            assertTrue("Timestamp should be >= before", it.timestamp >= before)
            assertTrue("Timestamp should be <= after", it.timestamp <= after)
        }
    }

    @Test
    fun testAssuranceLevelIsLocalObservation() = runBlocking {
        val input = "storage_object_count=1\nstorage_size_bytes=1024\nupstream_speed_kbps=100".toByteArray()
        val observation = parser.parseStats(input)

        observation?.let {
            assertEquals("LOCAL_OBSERVATION", it.assurance)
            assertEquals("NOT_RUN", it.independentVerification)
        }
    }

    @Test
    fun testRewardAssetIsAIOZ() = runBlocking {
        val input = "storage_object_count=1\nstorage_size_bytes=1024\nupstream_speed_kbps=100".toByteArray()
        val observation = parser.parseStats(input)

        observation?.let {
            assertEquals("AIOZ", it.rewardAsset)
            assertEquals("NOT_SUBMITTED", it.skrPaymentStatus)
        }
    }

    @Test
    fun testSourceDigestIsConsistent() = runBlocking {
        val input = "storage_object_count=42\nstorage_size_bytes=1048576\nupstream_speed_kbps=1024".toByteArray()
        val obs1 = parser.parseStats(input)
        val obs2 = parser.parseStats(input)

        assertNotNull(obs1)
        assertNotNull(obs2)
        obs1?.let { o1 ->
            obs2?.let { o2 ->
                assertEquals(o1.sourceSha256, o2.sourceSha256)
            }
        }
    }

    // ===== Float Handling Tests =====

    @Test
    fun testUpstreamSpeedAcceptsFloatInput() = runBlocking {
        val input = "storage_object_count=1\nstorage_size_bytes=1024\nupstream_speed_kbps=1024.5".toByteArray()
        val observation = parser.parseStats(input) as? AIZObservation

        assertNotNull(observation)
        observation?.let {
            // Float input is accepted and truncated to Long
            assertEquals(1024, it.metrics.upstreamSpeedRaw)
        }
    }

    @Test
    fun testIntegersForStorageFields() = runBlocking {
        val input = "storage_object_count=1\nstorage_size_bytes=1024\nupstream_speed_kbps=100".toByteArray()
        val observation = parser.parseStats(input) as? AIZObservation

        assertNotNull(observation)
        observation?.let {
            assertTrue(it.metrics.storageObjectCount is Long)
            assertTrue(it.metrics.storageSizeBytes is Long)
            assertTrue(it.metrics.upstreamSpeedRaw is Long)
        }
    }

    // ===== Negative Value Rejection =====

    @Test
    fun testRejectNegativeStorageObjectCount() = runBlocking {
        val input = "storage_object_count=-1\nstorage_size_bytes=1024\nupstream_speed_kbps=100".toByteArray()
        val observation = parser.parseStats(input)

        assertNull(observation)
    }

    @Test
    fun testRejectNegativeStorageSize() = runBlocking {
        val input = "storage_object_count=1\nstorage_size_bytes=-1024\nupstream_speed_kbps=100".toByteArray()
        val observation = parser.parseStats(input)

        assertNull(observation)
    }

    @Test
    fun testRejectNegativeUpstreamSpeed() = runBlocking {
        val input = "storage_object_count=1\nstorage_size_bytes=1024\nupstream_speed_kbps=-100".toByteArray()
        val observation = parser.parseStats(input)

        assertNull(observation)
    }

    // ===== Missing Field Rejection =====

    @Test
    fun testRejectMissingStorageObjectCount() = runBlocking {
        val input = "storage_size_bytes=1024\nupstream_speed_kbps=100".toByteArray()
        val observation = parser.parseStats(input)

        assertNull(observation)
    }

    @Test
    fun testRejectMissingStorageSize() = runBlocking {
        val input = "storage_object_count=1\nupstream_speed_kbps=100".toByteArray()
        val observation = parser.parseStats(input)

        assertNull(observation)
    }

    @Test
    fun testRejectMissingUpstreamSpeed() = runBlocking {
        val input = "storage_object_count=1\nstorage_size_bytes=1024".toByteArray()
        val observation = parser.parseStats(input)

        assertNull(observation)
    }

    // ===== Invalid Format Rejection =====

    @Test
    fun testRejectInvalidStorageObjectCount() = runBlocking {
        val input = "storage_object_count=abc\nstorage_size_bytes=1024\nupstream_speed_kbps=100".toByteArray()
        val observation = parser.parseStats(input)

        assertNull(observation)
    }

    @Test
    fun testRejectInvalidStorageSize() = runBlocking {
        val input = "storage_object_count=1\nstorage_size_bytes=xyz\nupstream_speed_kbps=100".toByteArray()
        val observation = parser.parseStats(input)

        assertNull(observation)
    }

    @Test
    fun testRejectInvalidUpstreamSpeed() = runBlocking {
        val input = "storage_object_count=1\nstorage_size_bytes=1024\nupstream_speed_kbps=notanumber".toByteArray()
        val observation = parser.parseStats(input)

        assertNull(observation)
    }

    // ===== Size Limit Tests =====

    @Test
    fun testRejectOversizedInput() = runBlocking {
        val oversized = ByteArray(66 * 1024) { 'A'.code.toByte() }
        val observation = parser.parseStats(oversized)

        assertNull(observation)
    }

    @Test
    fun testAcceptMaxValidSize() = runBlocking {
        val maxSize = ByteArray(65 * 1024 - 100)
        val validInput = "storage_object_count=1\nstorage_size_bytes=1024\nupstream_speed_kbps=100".toByteArray()
        val input = maxSize.copyOf(maxSize.size - 100) + validInput

        val observation = parser.parseStats(input)
        assertNotNull(observation)  // Should parse without size error
    }

    // ===== Edge Cases =====

    @Test
    fun testHandleEmptyInput() = runBlocking {
        val observation = parser.parseStats(ByteArray(0))

        assertNull(observation)
    }

    @Test
    fun testHandleWhitespaceOnlyInput() = runBlocking {
        val input = "   \n  \n   ".toByteArray()
        val observation = parser.parseStats(input)

        assertNull(observation)
    }

    @Test
    fun testHandleExtraFields() = runBlocking {
        val input = """
            storage_object_count=1
            storage_size_bytes=1024
            upstream_speed_kbps=100
            extra_field=ignored
            another_field=also_ignored
        """.trimIndent().toByteArray()

        val observation = parser.parseStats(input)

        assertNotNull(observation)  // Extra fields are ignored
    }

    @Test
    fun testZeroValuesAreValid() = runBlocking {
        val input = "storage_object_count=0\nstorage_size_bytes=0\nupstream_speed_kbps=0".toByteArray()
        val observation = parser.parseStats(input) as? AIZObservation

        assertNotNull(observation)
        observation?.let {
            assertEquals(0, it.metrics.storageObjectCount)
            assertEquals(0, it.metrics.storageSizeBytes)
            assertEquals(0, it.metrics.upstreamSpeedRaw)
        }
    }

    @Test
    fun testLargeValidValues() = runBlocking {
        val input = "storage_object_count=999999\nstorage_size_bytes=1099511627776\nupstream_speed_kbps=100000".toByteArray()
        val observation = parser.parseStats(input) as? AIZObservation

        assertNotNull(observation)
        observation?.let {
            assertEquals(999999, it.metrics.storageObjectCount)
            assertEquals(1099511627776, it.metrics.storageSizeBytes)
            assertEquals(100000, it.metrics.upstreamSpeedRaw)
        }
    }

    // ===== Interface Contract Tests =====

    @Test
    fun testImplementsObservationInterface() = runBlocking {
        val input = "storage_object_count=1\nstorage_size_bytes=1024\nupstream_speed_kbps=100".toByteArray()
        val observation = parser.parseStats(input)

        assertNotNull(observation)
        assertTrue(observation is Observation)
    }

    @Test
    fun testObservationImmutable() = runBlocking {
        val input = "storage_object_count=1\nstorage_size_bytes=1024\nupstream_speed_kbps=100".toByteArray()
        val observation = parser.parseStats(input) as? AIZObservation

        assertNotNull(observation)
        observation?.let {
            val copy = it.copy()
            assertEquals(it, copy)
            assertFalse(it === copy)  // Different instances
        }
    }
}
