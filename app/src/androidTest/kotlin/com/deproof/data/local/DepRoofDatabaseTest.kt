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

@RunWith(AndroidJUnit4::class)
class DepRoofDatabaseTest {
    private lateinit var database: DepRoofDatabase

    @Before
    fun setupDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            DepRoofDatabase::class.java
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun databaseCanBeCreated() {
        assertNotNull(database)
    }

    @Test
    fun receiptDaoIsAccessible() {
        val dao = database.receiptDao()
        assertNotNull(dao)
    }

    @Test
    fun databaseIsOpen() {
        assertEquals(true, database.isOpen)
    }

    @Test
    fun schemaVersionIsCorrect() {
        // DepRoofDatabase version should be 1 per @Database annotation
        assertEquals(1, database.openHelper.readableDatabase.version)
    }

    @Test
    fun receiptsTableExists() = runBlocking {
        val receipt = ReceiptEntity(
            id = "schema-test",
            transactionHash = "hash",
            verdict = "VALID",
            messageHash = "msg"
        )

        val dao = database.receiptDao()
        dao.insert(receipt)

        val retrieved = dao.getById("schema-test")
        assertNotNull(retrieved)
        assertEquals("schema-test", retrieved.id)
    }

    @Test
    fun receiptEntityColumnsCorrect() = runBlocking {
        val dao = database.receiptDao()
        val receipt = ReceiptEntity(
            id = "column-test",
            transactionHash = "hash-col",
            verdict = "PENDING",
            messageHash = "msg-col",
            timestamp = 1000L,
            signatureStatus = "VERIFIED",
            chainSubmitted = true,
            jsonData = """{"test":"value"}"""
        )

        dao.insert(receipt)
        val retrieved = dao.getById("column-test")

        assertNotNull(retrieved)
        assertEquals("hash-col", retrieved.transactionHash)
        assertEquals("PENDING", retrieved.verdict)
        assertEquals("msg-col", retrieved.messageHash)
        assertEquals(1000L, retrieved.timestamp)
        assertEquals("VERIFIED", retrieved.signatureStatus)
        assertEquals(true, retrieved.chainSubmitted)
        assertEquals("""{"test":"value"}""", retrieved.jsonData)
    }

    @Test
    fun databaseMultipleOperations() = runBlocking {
        val dao = database.receiptDao()

        // Insert multiple receipts
        val receipts = (1..5).map { i ->
            ReceiptEntity(
                id = "multi-$i",
                transactionHash = "hash-$i",
                verdict = if (i % 2 == 0) "VALID" else "PENDING",
                messageHash = "msg-$i",
                timestamp = i * 1000L
            )
        }

        receipts.forEach { dao.insert(it) }

        val count = dao.getReceiptCount()
        assertEquals(5, count)

        val allReceipts = dao.getAllReceipts()
        assertEquals(5, allReceipts.size)

        val recent = dao.getRecentReceipts(3)
        assertEquals(3, recent.size)
    }
}
