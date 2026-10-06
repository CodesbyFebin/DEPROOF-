package com.deproof.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ReceiptDaoTest {
    private lateinit var database: DepRoofDatabase
    private lateinit var receiptDao: ReceiptDao

    @Before
    fun setupDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            DepRoofDatabase::class.java
        ).build()
        receiptDao = database.receiptDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertAndRetrieveReceipt() = runBlocking {
        val receipt = ReceiptEntity(
            id = "test-receipt-1",
            transactionHash = "hash123",
            verdict = "VALID",
            messageHash = "msg-hash-123",
            timestamp = 1000L,
            signatureStatus = "VERIFIED",
            chainSubmitted = false,
            jsonData = "{}"
        )

        receiptDao.insert(receipt)
        val retrieved = receiptDao.getById("test-receipt-1")

        assertNotNull(retrieved)
        assertEquals(receipt.transactionHash, retrieved.transactionHash)
        assertEquals(receipt.verdict, retrieved.verdict)
        assertEquals(receipt.messageHash, retrieved.messageHash)
        assertEquals(receipt.signatureStatus, retrieved.signatureStatus)
    }

    @Test
    fun updateReceipt() = runBlocking {
        val receipt = ReceiptEntity(
            id = "test-receipt-2",
            transactionHash = "hash456",
            verdict = "PENDING",
            messageHash = "msg-hash-456",
            timestamp = 2000L
        )

        receiptDao.insert(receipt)

        val updated = receipt.copy(
            verdict = "VALID",
            signatureStatus = "CONFIRMED",
            chainSubmitted = true
        )
        receiptDao.update(updated)

        val retrieved = receiptDao.getById("test-receipt-2")
        assertNotNull(retrieved)
        assertEquals("VALID", retrieved.verdict)
        assertEquals("CONFIRMED", retrieved.signatureStatus)
        assertTrue(retrieved.chainSubmitted)
    }

    @Test
    fun deleteReceipt() = runBlocking {
        val receipt = ReceiptEntity(
            id = "test-receipt-3",
            transactionHash = "hash789",
            verdict = "INVALID",
            messageHash = "msg-hash-789"
        )

        receiptDao.insert(receipt)
        var retrieved = receiptDao.getById("test-receipt-3")
        assertNotNull(retrieved)

        receiptDao.delete(receipt)
        retrieved = receiptDao.getById("test-receipt-3")
        assertNull(retrieved)
    }

    @Test
    fun getAllReceiptsOrderedByTimestampDesc() = runBlocking {
        val receipt1 = ReceiptEntity(
            id = "receipt-1",
            transactionHash = "hash1",
            verdict = "VALID",
            messageHash = "msg1",
            timestamp = 1000L
        )
        val receipt2 = ReceiptEntity(
            id = "receipt-2",
            transactionHash = "hash2",
            verdict = "VALID",
            messageHash = "msg2",
            timestamp = 2000L
        )
        val receipt3 = ReceiptEntity(
            id = "receipt-3",
            transactionHash = "hash3",
            verdict = "VALID",
            messageHash = "msg3",
            timestamp = 3000L
        )

        receiptDao.insert(receipt1)
        receiptDao.insert(receipt2)
        receiptDao.insert(receipt3)

        val allReceipts = receiptDao.getAllReceipts()
        assertEquals(3, allReceipts.size)
        assertEquals("receipt-3", allReceipts[0].id)
        assertEquals("receipt-2", allReceipts[1].id)
        assertEquals("receipt-1", allReceipts[2].id)
    }

    @Test
    fun getRecentReceiptsWithLimit() = runBlocking {
        repeat(5) { i ->
            val receipt = ReceiptEntity(
                id = "receipt-$i",
                transactionHash = "hash$i",
                verdict = "VALID",
                messageHash = "msg$i",
                timestamp = (i + 1) * 1000L
            )
            receiptDao.insert(receipt)
        }

        val recent = receiptDao.getRecentReceipts(2)
        assertEquals(2, recent.size)
        assertEquals("receipt-4", recent[0].id)
        assertEquals("receipt-3", recent[1].id)
    }

    @Test
    fun getReceiptCountEmpty() = runBlocking {
        val count = receiptDao.getReceiptCount()
        assertEquals(0, count)
    }

    @Test
    fun getReceiptCountAfterInserts() = runBlocking {
        repeat(3) { i ->
            val receipt = ReceiptEntity(
                id = "count-receipt-$i",
                transactionHash = "hash$i",
                verdict = "VALID",
                messageHash = "msg$i"
            )
            receiptDao.insert(receipt)
        }

        val count = receiptDao.getReceiptCount()
        assertEquals(3, count)
    }

    @Test
    fun deleteOlderThanTimestamp() = runBlocking {
        val receipt1 = ReceiptEntity(
            id = "old-receipt",
            transactionHash = "hash-old",
            verdict = "VALID",
            messageHash = "msg-old",
            timestamp = 1000L
        )
        val receipt2 = ReceiptEntity(
            id = "new-receipt",
            transactionHash = "hash-new",
            verdict = "VALID",
            messageHash = "msg-new",
            timestamp = 5000L
        )

        receiptDao.insert(receipt1)
        receiptDao.insert(receipt2)

        val countBefore = receiptDao.getReceiptCount()
        assertEquals(2, countBefore)

        receiptDao.deleteOlderThan(3000L)

        val countAfter = receiptDao.getReceiptCount()
        assertEquals(1, countAfter)

        val remaining = receiptDao.getById("new-receipt")
        assertNotNull(remaining)

        val deleted = receiptDao.getById("old-receipt")
        assertNull(deleted)
    }

    @Test
    fun insertWithOnConflictReplace() = runBlocking {
        val receipt1 = ReceiptEntity(
            id = "conflict-receipt",
            transactionHash = "hash-v1",
            verdict = "PENDING",
            messageHash = "msg-v1"
        )
        receiptDao.insert(receipt1)

        val receipt2 = ReceiptEntity(
            id = "conflict-receipt",
            transactionHash = "hash-v2",
            verdict = "VALID",
            messageHash = "msg-v2"
        )
        receiptDao.insert(receipt2)

        val count = receiptDao.getReceiptCount()
        assertEquals(1, count)

        val retrieved = receiptDao.getById("conflict-receipt")
        assertNotNull(retrieved)
        assertEquals("hash-v2", retrieved.transactionHash)
        assertEquals("VALID", retrieved.verdict)
    }

    @Test
    fun receiptFieldDefaults() = runBlocking {
        val receipt = ReceiptEntity(
            id = "test-defaults",
            transactionHash = "hash-test",
            verdict = "PENDING",
            messageHash = "msg-test"
        )

        receiptDao.insert(receipt)
        val retrieved = receiptDao.getById("test-defaults")

        assertNotNull(retrieved)
        assertEquals("PENDING", retrieved.signatureStatus)
        assertEquals(false, retrieved.chainSubmitted)
        assertEquals("", retrieved.jsonData)
    }

    @Test
    fun receiptTimestampPreservation() = runBlocking {
        val customTimestamp = 1234567890L
        val receipt = ReceiptEntity(
            id = "test-timestamp",
            transactionHash = "hash",
            verdict = "VALID",
            messageHash = "msg",
            timestamp = customTimestamp
        )

        receiptDao.insert(receipt)
        val retrieved = receiptDao.getById("test-timestamp")

        assertNotNull(retrieved)
        assertEquals(customTimestamp, retrieved.timestamp)
    }
}
