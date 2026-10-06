package com.deproof.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deproof.domain.model.Receipt
import com.deproof.domain.model.SignatureStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class ReceiptMappingTest {
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
    fun entityToDomainMapping() {
        val entity = ReceiptEntity(
            id = "entity-to-domain",
            transactionHash = "hash123",
            verdict = "VALID",
            messageHash = "msg-hash",
            timestamp = 1000L,
            signatureStatus = "VERIFIED",
            chainSubmitted = true,
            jsonData = """{"test":"data"}"""
        )

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.transactionHash, domain.transactionHash)
        assertEquals(entity.verdict, domain.verdict)
        assertEquals(entity.messageHash, domain.messageHash)
        assertEquals(entity.timestamp, domain.timestamp)
        assertEquals(SignatureStatus.VERIFIED, domain.signatureStatus)
        assertEquals(entity.chainSubmitted, domain.chainSubmitted)
        assertEquals(entity.jsonData, domain.jsonData)
    }

    @Test
    fun domainToEntityMapping() {
        val domain = Receipt(
            id = "domain-to-entity",
            transactionHash = "hash456",
            verdict = "PENDING",
            messageHash = "msg-hash-456",
            timestamp = 2000L,
            signatureStatus = SignatureStatus.PENDING,
            chainSubmitted = false,
            jsonData = """{"pending":"true"}"""
        )

        val entity = domain.toEntity()

        assertEquals(domain.id, entity.id)
        assertEquals(domain.transactionHash, entity.transactionHash)
        assertEquals(domain.verdict, entity.verdict)
        assertEquals(domain.messageHash, entity.messageHash)
        assertEquals(domain.timestamp, entity.timestamp)
        assertEquals(domain.signatureStatus.name, entity.signatureStatus)
        assertEquals(domain.chainSubmitted, entity.chainSubmitted)
        assertEquals(domain.jsonData, entity.jsonData)
    }

    @Test
    fun mappingRoundTrip() {
        val original = ReceiptEntity(
            id = "round-trip",
            transactionHash = "hash-rt",
            verdict = "CONFIRMED",
            messageHash = "msg-rt",
            timestamp = 3000L,
            signatureStatus = "CONFIRMED",
            chainSubmitted = true,
            jsonData = """{"confirmed":"yes"}"""
        )

        val domain = original.toDomain()
        val back = domain.toEntity()

        assertEquals(original.id, back.id)
        assertEquals(original.transactionHash, back.transactionHash)
        assertEquals(original.verdict, back.verdict)
        assertEquals(original.messageHash, back.messageHash)
        assertEquals(original.timestamp, back.timestamp)
        assertEquals(original.signatureStatus, back.signatureStatus)
        assertEquals(original.chainSubmitted, back.chainSubmitted)
        assertEquals(original.jsonData, back.jsonData)
    }

    @Test
    fun persistAndRetrieveMappedEntity() = runBlocking {
        val domain = Receipt(
            id = "persist-mapped",
            transactionHash = "hash-persist",
            verdict = "VALID",
            messageHash = "msg-persist",
            timestamp = 4000L,
            signatureStatus = SignatureStatus.VERIFIED,
            chainSubmitted = true,
            jsonData = """{"persist":"test"}"""
        )

        val entity = domain.toEntity()
        receiptDao.insert(entity)

        val retrieved = receiptDao.getById("persist-mapped")
        assertNotNull(retrieved)

        val retrievedDomain = retrieved.toDomain()
        assertEquals(domain.id, retrievedDomain.id)
        assertEquals(domain.transactionHash, retrievedDomain.transactionHash)
        assertEquals(domain.verdict, retrievedDomain.verdict)
        assertEquals(domain.signatureStatus, retrievedDomain.signatureStatus)
    }

    @Test
    fun allSignatureStatusesMapCorrectly() = runBlocking {
        SignatureStatus.values().forEach { status ->
            val entity = ReceiptEntity(
                id = "status-${status.name}",
                transactionHash = "hash-status",
                verdict = "TEST",
                messageHash = "msg-status",
                signatureStatus = status.name
            )

            val domain = entity.toDomain()
            assertEquals(status, domain.signatureStatus)

            val backToEntity = domain.toEntity()
            assertEquals(status.name, backToEntity.signatureStatus)
        }
    }

    @Test
    fun emptyJsonDataMapping() {
        val entity = ReceiptEntity(
            id = "empty-json",
            transactionHash = "hash",
            verdict = "VALID",
            messageHash = "msg"
        )

        val domain = entity.toDomain()
        assertEquals("", domain.jsonData)

        val backToEntity = domain.toEntity()
        assertEquals("", backToEntity.jsonData)
    }

    @Test
    fun complexJsonDataMapping() {
        val jsonData = """{"nested":{"data":"value"},"array":[1,2,3],"bool":true}"""
        val entity = ReceiptEntity(
            id = "complex-json",
            transactionHash = "hash",
            verdict = "VALID",
            messageHash = "msg",
            jsonData = jsonData
        )

        val domain = entity.toDomain()
        assertEquals(jsonData, domain.jsonData)

        val backToEntity = domain.toEntity()
        assertEquals(jsonData, backToEntity.jsonData)
    }
}
