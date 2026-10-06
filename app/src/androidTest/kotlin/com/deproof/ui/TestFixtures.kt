package com.deproof.ui

import android.content.Context
import com.deproof.domain.model.Receipt
import com.deproof.domain.model.Verdict
import com.deproof.data.repository.RpcRepository
import com.deproof.data.repository.ReceiptRepository
import com.deproof.domain.exception.DomainException
import com.deproof.domain.util.Result
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Mock data factory for Phase 2 UI tests.
 * Provides consistent test data across all test classes.
 */
object MockDataFactory {

    fun createMockReceipt(
        id: String = "receipt-1",
        instruction: String = "Transfer 1 SOL",
        verdict: Verdict = Verdict.Payable,
        approved: Boolean = true,
        timestamp: Long = System.currentTimeMillis()
    ): Receipt = Receipt(
        id = id,
        transactionHash = "5HpibQW3DsJJWadS2gKbMrMEZA11b2CNz9nwkV4nZYsSomeHashValue123",
        instruction = instruction,
        verdict = verdict,
        timestamp = timestamp,
        approved = approved,
        messageHash = "ca978112ca1bbdc16f7a08a27516e5c2460fcfe27054391b2370ce57415b34f",
        signer = "11111111111111111111111111111111"
    )

    fun createMockReceipts(count: Int = 5): List<Receipt> =
        (1..count).map { i ->
            createMockReceipt(
                id = "receipt-$i",
                instruction = "Transfer ${i} SOL",
                timestamp = System.currentTimeMillis() - (i * 60_000L)
            )
        }

    fun createMockTransactionData(
        address: String = "11111111111111111111111111111111",
        solAmount: BigDecimal = BigDecimal("100.50"),
        skrAmount: BigDecimal = BigDecimal("1000.00")
    ) = TransactionData(
        address = address,
        solBalance = solAmount,
        skrBalance = skrAmount,
        lastUpdate = System.currentTimeMillis()
    )

    fun createInvalidInstruction(): String =
        "INVALID_INSTRUCTION_NOT_BASE64_ENCODED"

    fun createValidTransferInstruction(): String =
        "base64EncodedTransferInstructionHere"

    fun createValidTokenTransferInstruction(): String =
        "base64EncodedTokenTransferInstructionHere"
}

/**
 * Test data holder for transaction information.
 */
data class TransactionData(
    val address: String,
    val solBalance: BigDecimal,
    val skrBalance: BigDecimal,
    val lastUpdate: Long
)

/**
 * Mock RPC repository for UI testing.
 * Returns predefined values without making network calls.
 */
class MockRpcRepository : RpcRepository {
    var balanceSol: BigDecimal = BigDecimal("100.50")
    var balanceSkr: BigDecimal = BigDecimal("1000.00")
    var shouldFailOnNextCall: Boolean = false
    var callCount: Int = 0

    override suspend fun getBalance(address: String): Result<BigDecimal> {
        callCount++
        return if (shouldFailOnNextCall) {
            Result.failure(DomainException.NetworkException("Mock network error"))
        } else {
            Result.success(balanceSol)
        }
    }

    override suspend fun getTokenBalance(
        address: String,
        mint: String
    ): Result<BigDecimal> {
        callCount++
        return if (shouldFailOnNextCall) {
            Result.failure(DomainException.NetworkException("Mock network error"))
        } else {
            Result.success(balanceSkr)
        }
    }

    fun reset() {
        shouldFailOnNextCall = false
        callCount = 0
    }
}

/**
 * Mock Receipt repository for UI testing.
 * Returns mock receipts without database access.
 */
class MockReceiptRepository : ReceiptRepository {
    private val receipts = mutableListOf<Receipt>()
    var shouldFailOnNextCall: Boolean = false

    init {
        receipts.addAll(MockDataFactory.createMockReceipts(3))
    }

    override suspend fun insert(receipt: Receipt): Result<String> {
        return if (shouldFailOnNextCall) {
            Result.failure(DomainException.StorageException("Mock storage error"))
        } else {
            receipts.add(receipt)
            Result.success(receipt.id)
        }
    }

    override suspend fun get(id: String): Result<Receipt?> {
        return Result.success(receipts.find { it.id == id })
    }

    override suspend fun getAll(): Result<List<Receipt>> {
        return if (shouldFailOnNextCall) {
            Result.failure(DomainException.StorageException("Mock storage error"))
        } else {
            Result.success(receipts.toList())
        }
    }

    override suspend fun getRecent(limit: Int): Result<List<Receipt>> {
        return Result.success(receipts.takeLast(limit))
    }

    override suspend fun update(receipt: Receipt): Result<Unit> {
        val index = receipts.indexOfFirst { it.id == receipt.id }
        return if (index >= 0) {
            receipts[index] = receipt
            Result.success(Unit)
        } else {
            Result.failure(DomainException.NotFound("Receipt not found"))
        }
    }

    override suspend fun delete(id: String): Result<Unit> {
        val removed = receipts.removeAll { it.id == id }
        return if (removed) {
            Result.success(Unit)
        } else {
            Result.failure(DomainException.NotFound("Receipt not found"))
        }
    }

    fun reset() {
        receipts.clear()
        receipts.addAll(MockDataFactory.createMockReceipts(3))
        shouldFailOnNextCall = false
    }
}

/**
 * Test context manager for setting up UI test environments.
 */
class TestContextManager(val context: Context) {
    private val rpcRepository = MockRpcRepository()
    private val receiptRepository = MockReceiptRepository()

    fun getRpcRepository(): RpcRepository = rpcRepository

    fun getReceiptRepository(): ReceiptRepository = receiptRepository

    fun resetAll() {
        rpcRepository.reset()
        receiptRepository.reset()
    }

    fun simulateNetworkError() {
        rpcRepository.shouldFailOnNextCall = true
    }

    fun simulateStorageError() {
        receiptRepository.shouldFailOnNextCall = true
    }
}

/**
 * Time formatting utilities for tests.
 */
object TestTimeUtils {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun formatTimestamp(millis: Long): String {
        return LocalDateTime.now().format(formatter)
    }

    fun getRelativeTime(millis: Long): String {
        val seconds = (System.currentTimeMillis() - millis) / 1000
        return when {
            seconds < 60 -> "now"
            seconds < 3600 -> "${seconds / 60} min ago"
            seconds < 86400 -> "${seconds / 3600} hours ago"
            else -> "${seconds / 86400} days ago"
        }
    }
}

/**
 * Test assertions and helpers for common UI patterns.
 */
object TestAssertions {

    fun isValidAddress(address: String): Boolean {
        // Valid Solana address: 32 bytes base58 encoded (approximately 44 characters)
        return address.length in 40..50 && !address.contains(Regex("[^123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]"))
    }

    fun isValidSignature(signature: String): Boolean {
        // Valid Solana signature: 64 bytes base58 encoded (approximately 88 characters)
        return signature.length in 80..95
    }

    fun isValidBalance(amount: String): Boolean {
        return try {
            BigDecimal(amount).let { it >= BigDecimal.ZERO }
        } catch (e: Exception) {
            false
        }
    }

    fun isValidTimestamp(timestamp: Long): Boolean {
        val now = System.currentTimeMillis()
        val hour = 60 * 60 * 1000
        return timestamp in (now - (365 * 24 * hour))..now
    }
}
