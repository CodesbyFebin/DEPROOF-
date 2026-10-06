package com.deproof.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class EncryptedBackupTest {
    private lateinit var context: Context
    private lateinit var backupManager: EncryptedBackupManager
    private lateinit var testBackupDir: File
    private val gson = Gson()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        backupManager = EncryptedBackupManager(context, gson)

        testBackupDir = File(context.cacheDir, "test_backups")
        testBackupDir.mkdirs()
    }

    @After
    fun cleanup() {
        testBackupDir.deleteRecursively()
    }

    @Test
    fun createAndRestoreBackup() = runBlocking {
        val testData = BackupData(
            receiptIds = listOf("receipt-1", "receipt-2"),
            transactionHashes = listOf("hash-1", "hash-2"),
            verdicts = mapOf("hash-1" to "VALID", "hash-2" to "PENDING")
        )

        val backupPath = File(testBackupDir, "test.backup").absolutePath

        val createResult = backupManager.createBackup(testData, backupPath)
        assertTrue(createResult.isSuccess())

        val file = File(backupPath)
        assertTrue(file.exists())

        val restoreResult = backupManager.restoreBackup(backupPath)
        assertTrue(restoreResult.isSuccess())

        val restoredData = (restoreResult as com.deproof.domain.model.Result.Success).data
        assertEquals(testData.receiptIds, restoredData.receiptIds)
        assertEquals(testData.transactionHashes, restoredData.transactionHashes)
        assertEquals(testData.verdicts, restoredData.verdicts)
    }

    @Test
    fun deleteBackup() = runBlocking {
        val testData = BackupData(receiptIds = listOf("receipt-1"))
        val backupPath = File(testBackupDir, "delete_test.backup").absolutePath

        backupManager.createBackup(testData, backupPath)
        assertTrue(File(backupPath).exists())

        val deleteResult = backupManager.deleteBackup(backupPath)
        assertTrue(deleteResult.isSuccess())
        assertFalse(File(backupPath).exists())
    }

    @Test
    fun getBackupMetadata() = runBlocking {
        val testData = BackupData(receiptIds = listOf("receipt-1"))
        val backupPath = File(testBackupDir, "metadata_test.backup").absolutePath

        backupManager.createBackup(testData, backupPath)

        val metadataResult = backupManager.getBackupMetadata(backupPath)
        assertTrue(metadataResult.isSuccess())

        val metadata = (metadataResult as com.deproof.domain.model.Result.Success).data
        assertNotNull(metadata)
        assertTrue(metadata.timestamp > 0)
        assertEquals(1, metadata.version)
        assertTrue(metadata.deviceId.isNotEmpty())
    }

    @Test
    fun listBackups() = runBlocking {
        val testData1 = BackupData(receiptIds = listOf("receipt-1"))
        val testData2 = BackupData(receiptIds = listOf("receipt-2"))

        backupManager.createBackup(testData1, File(testBackupDir, "backup1.backup").absolutePath)
        backupManager.createBackup(testData2, File(testBackupDir, "backup2.backup").absolutePath)

        val listResult = backupManager.listBackups(testBackupDir.absolutePath)
        assertTrue(listResult.isSuccess())

        val backups = (listResult as com.deproof.domain.model.Result.Success).data
        assertEquals(2, backups.size)
        assertTrue(backups.all { it.sizeBytes > 0 })
    }

    @Test
    fun restoreNonexistentBackup() = runBlocking {
        val backupPath = File(testBackupDir, "nonexistent.backup").absolutePath
        val restoreResult = backupManager.restoreBackup(backupPath)
        assertFalse(restoreResult.isSuccess())
    }

    @Test
    fun backupMetadataValidation() = runBlocking {
        val testData = BackupData(
            receiptIds = listOf("receipt-1"),
            transactionHashes = listOf("hash-1")
        )
        val backupPath = File(testBackupDir, "validation_test.backup").absolutePath

        backupManager.createBackup(testData, backupPath)
        val metadataResult = backupManager.getBackupMetadata(backupPath)

        assertTrue(metadataResult.isSuccess())
        val metadata = (metadataResult as com.deproof.domain.model.Result.Success).data
        assertEquals(1, metadata.version)
        assertTrue(metadata.timestamp > 0)
    }

    @Test
    fun largeBackupHandling() = runBlocking {
        val largeData = BackupData(
            receiptIds = (1..1000).map { "receipt-$it" },
            transactionHashes = (1..1000).map { "hash-$it" },
            verdicts = (1..1000).associate { "hash-$it" to "VALID" }
        )
        val backupPath = File(testBackupDir, "large.backup").absolutePath

        val createResult = backupManager.createBackup(largeData, backupPath)
        assertTrue(createResult.isSuccess())

        val restoreResult = backupManager.restoreBackup(backupPath)
        assertTrue(restoreResult.isSuccess())

        val restored = (restoreResult as com.deproof.domain.model.Result.Success).data
        assertEquals(largeData.receiptIds.size, restored.receiptIds.size)
        assertEquals(largeData.verdicts.size, restored.verdicts.size)
    }

    @Test
    fun emptyBackupHandling() = runBlocking {
        val emptyData = BackupData()
        val backupPath = File(testBackupDir, "empty.backup").absolutePath

        val createResult = backupManager.createBackup(emptyData, backupPath)
        assertTrue(createResult.isSuccess())

        val restoreResult = backupManager.restoreBackup(backupPath)
        assertTrue(restoreResult.isSuccess())

        val restored = (restoreResult as com.deproof.domain.model.Result.Success).data
        assertEquals(0, restored.receiptIds.size)
        assertEquals(0, restored.verdicts.size)
    }

    @Test
    fun backupFilePersistence() = runBlocking {
        val testData = BackupData(receiptIds = listOf("receipt-1"))
        val backupPath = File(testBackupDir, "persistence_test.backup").absolutePath

        backupManager.createBackup(testData, backupPath)
        val file = File(backupPath)
        val fileSize1 = file.length()

        val restoreResult = backupManager.restoreBackup(backupPath)
        assertTrue(restoreResult.isSuccess())

        val fileSize2 = file.length()
        assertEquals(fileSize1, fileSize2)
    }

    @Test
    fun metadataDeviceIdNonEmpty() = runBlocking {
        val testData = BackupData(receiptIds = listOf("receipt-1"))
        val backupPath = File(testBackupDir, "device_id_test.backup").absolutePath

        backupManager.createBackup(testData, backupPath)
        val metadataResult = backupManager.getBackupMetadata(backupPath)

        assertTrue(metadataResult.isSuccess())
        val metadata = (metadataResult as com.deproof.domain.model.Result.Success).data
        assertTrue(metadata.deviceId.isNotEmpty())
        assertTrue(metadata.deviceId.length >= 16)
    }
}
