package com.deproof.data.security

import com.google.gson.Gson
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BackupDataSerializationTest {
    private lateinit var gson: Gson

    @Before
    fun setup() {
        gson = Gson()
    }

    @Test
    fun serializeBackupData() {
        val data = BackupData(
            receiptIds = listOf("receipt-1", "receipt-2"),
            transactionHashes = listOf("hash-1", "hash-2"),
            verdicts = mapOf("hash-1" to "VALID", "hash-2" to "PENDING")
        )

        val json = gson.toJson(data)
        assertNotNull(json)
        assertEquals(true, json.contains("receipt-1"))
        assertEquals(true, json.contains("VALID"))
    }

    @Test
    fun deserializeBackupData() {
        val originalData = BackupData(
            receiptIds = listOf("receipt-1", "receipt-2"),
            transactionHashes = listOf("hash-1", "hash-2"),
            verdicts = mapOf("hash-1" to "VALID", "hash-2" to "PENDING"),
            metadata = mapOf("key" to "value")
        )

        val json = gson.toJson(originalData)
        val deserialized = gson.fromJson(json, BackupData::class.java)

        assertEquals(originalData.receiptIds, deserialized.receiptIds)
        assertEquals(originalData.transactionHashes, deserialized.transactionHashes)
        assertEquals(originalData.verdicts, deserialized.verdicts)
        assertEquals(originalData.metadata, deserialized.metadata)
    }

    @Test
    fun serializeBackupMetadata() {
        val metadata = BackupMetadata(
            timestamp = 1234567890L,
            version = 1,
            deviceId = "test-device-12345"
        )

        val json = gson.toJson(metadata)
        assertNotNull(json)
        assertEquals(true, json.contains("1234567890"))
        assertEquals(true, json.contains("test-device-12345"))
    }

    @Test
    fun deserializeBackupMetadata() {
        val originalMetadata = BackupMetadata(
            timestamp = 1234567890L,
            version = 1,
            deviceId = "test-device-12345"
        )

        val json = gson.toJson(originalMetadata)
        val deserialized = gson.fromJson(json, BackupMetadata::class.java)

        assertEquals(originalMetadata.timestamp, deserialized.timestamp)
        assertEquals(originalMetadata.version, deserialized.version)
        assertEquals(originalMetadata.deviceId, deserialized.deviceId)
    }

    @Test
    fun serializeBackupContent() {
        val metadata = BackupMetadata(
            timestamp = 1234567890L,
            version = 1,
            deviceId = "test-device"
        )
        val data = BackupData(
            receiptIds = listOf("receipt-1"),
            transactionHashes = listOf("hash-1")
        )
        val content = BackupContent(metadata = metadata, data = data)

        val json = gson.toJson(content)
        assertNotNull(json)
        assertEquals(true, json.contains("metadata"))
        assertEquals(true, json.contains("data"))
    }

    @Test
    fun deserializeBackupContent() {
        val metadata = BackupMetadata(
            timestamp = 1234567890L,
            version = 1,
            deviceId = "test-device"
        )
        val data = BackupData(
            receiptIds = listOf("receipt-1", "receipt-2"),
            transactionHashes = listOf("hash-1", "hash-2")
        )
        val originalContent = BackupContent(metadata = metadata, data = data)

        val json = gson.toJson(originalContent)
        val deserialized = gson.fromJson(json, BackupContent::class.java)

        assertEquals(originalContent.metadata.timestamp, deserialized.metadata.timestamp)
        assertEquals(originalContent.data.receiptIds, deserialized.data.receiptIds)
        assertEquals(originalContent.data.transactionHashes, deserialized.data.transactionHashes)
    }

    @Test
    fun backupDataRoundTrip() {
        val data = BackupData(
            receiptIds = (1..100).map { "receipt-$it" },
            transactionHashes = (1..100).map { "hash-$it" },
            verdicts = (1..100).associate { "hash-$it" to if (it % 2 == 0) "VALID" else "PENDING" },
            metadata = mapOf(
                "source" to "android",
                "app_version" to "1.0.0",
                "backup_type" to "encrypted"
            )
        )

        val json = gson.toJson(data)
        val restored = gson.fromJson(json, BackupData::class.java)

        assertEquals(data.receiptIds.size, restored.receiptIds.size)
        assertEquals(data.transactionHashes.size, restored.transactionHashes.size)
        assertEquals(data.verdicts.size, restored.verdicts.size)
        assertEquals(data.metadata.size, restored.metadata.size)
    }

    @Test
    fun emptyBackupDataSerialization() {
        val data = BackupData()
        val json = gson.toJson(data)
        val restored = gson.fromJson(json, BackupData::class.java)

        assertEquals(0, restored.receiptIds.size)
        assertEquals(0, restored.transactionHashes.size)
        assertEquals(0, restored.verdicts.size)
        assertEquals(0, restored.metadata.size)
    }

    @Test
    fun backupInfoSerialization() {
        val info = BackupInfo(
            path = "/data/backup.backup",
            timestamp = 1234567890L,
            version = 1,
            sizeBytes = 1024L
        )

        val json = gson.toJson(info)
        val deserialized = gson.fromJson(json, BackupInfo::class.java)

        assertEquals(info.path, deserialized.path)
        assertEquals(info.timestamp, deserialized.timestamp)
        assertEquals(info.version, deserialized.version)
        assertEquals(info.sizeBytes, deserialized.sizeBytes)
    }
}
