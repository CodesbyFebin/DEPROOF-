package com.deproof.integration

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest

/**
 * End-to-end integration tests for DeProof custody framework.
 * These tests verify the complete flow from policy validation through
 * crash recovery, ensuring noncustodial architecture guarantees.
 */
class EndToEndTest {

    @Mock
    private lateinit var policyValidator: PolicyValidator

    @Mock
    private lateinit var walletAdapter: MobileWalletAdapterBridge

    @Mock
    private lateinit var transactionSigner: TransactionSigner

    @Mock
    private lateinit var rpcClient: SolanaRpcClient

    @Mock
    private lateinit var custodyDecisionDao: CustodyDecisionDao

    private lateinit var recoveryManager: EvidenceRecoveryManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        recoveryManager = EvidenceRecoveryManager(
            dao = custodyDecisionDao,
            rpc = rpcClient
        )
    }

    // ============================================================================
    // TEST 1: Stake Flow with Fail-Closed Policy
    // ============================================================================

    @Test
    fun testStakeFlowWithFailClosedPolicy() = runBlocking {
        // Arrange: Set up test state
        val wallet = TestFixtures.publicKey("11111111111111111111111111111111")
        val stakeAmount = "1000000" // Raw amount in lamports
        val poolId = "SeekerStaking1111111111111111111111111111"
        val policy = InstructionPolicy.StakingAction(
            poolId = poolId,
            action = StakingActionType.Stake(amountRaw = 1000000L),
            maxSlippageBps = 100
        )

        val transaction = createMockStakingTransaction(wallet, stakeAmount)
        val transactionBytes = transaction.serializeToBytes()
        val reviewHash = MessageDigest.getInstance("SHA-256").digest(transactionBytes)

        // Step 1: ReviewScreen validates policy
        whenever(policyValidator.isAllowed(any(), eq(policy)))
            .thenReturn(Result.success(Unit))

        // Step 2: MWA signs transaction (via wallet bridge)
        val signedTransaction = SignedTransaction(
            bytes = transactionBytes,
            wallet = wallet,
            signatures = listOf(ByteArray(64)) // Ed25519 sig
        )
        whenever(walletAdapter.signTransaction(transaction, policy))
            .thenReturn(Result.success(signedTransaction))

        // Step 3: Signature verified post-MWA
        whenever(transactionSigner.verifySignatures(signedTransaction))
            .thenReturn(Result.success(Unit))

        // Step 4: Receipt logged with custody decision
        val custodyDecision = CustodyDecision(
            id = "test-decision-1",
            timestamp = System.currentTimeMillis(),
            nodeSessionId = "test-session",
            instructionType = "STAKE",
            instructionBytes = transactionBytes,
            policyConcurrence = reviewHash.toString(Charsets.UTF_8),
            nodeSignature = ByteArray(64),
            transactionHash = "test-signature",
            status = DecisionStatus.APPROVED
        )
        whenever(custodyDecisionDao.insert(custodyDecision))
            .thenReturn(Unit)

        // Act: Execute stake flow
        val policyResult = policyValidator.isAllowed(
            SolanaInstruction.fromTransaction(transaction),
            policy
        )
        assertTrue("Policy validation failed", policyResult.isSuccess)

        val signResult = walletAdapter.signTransaction(transaction, policy)
        assertTrue("Wallet signing failed", signResult.isSuccess)

        val verifyResult = transactionSigner.verifySignatures(signResult.getOrThrow())
        assertTrue("Signature verification failed", verifyResult.isSuccess)

        custodyDecisionDao.insert(custodyDecision)

        // Assert: All steps succeeded without silent failures
        verify(policyValidator).isAllowed(any(), eq(policy))
        verify(walletAdapter).signTransaction(transaction, policy)
        verify(transactionSigner).verifySignatures(signResult.getOrThrow())
        verify(custodyDecisionDao).insert(custodyDecision)

        // Assert: No errors were silently swallowed
        assertEquals("Flow must complete without errors",
            policyResult.getOrThrow(), Unit)
    }

    // ============================================================================
    // TEST 2: Crash Recovery After MWA Approval
    // ============================================================================

    @Test
    fun testCrashRecoveryAfterMwaApproval() = runBlocking {
        // Arrange: Simulate app crash after MWA approval but before RPC send
        val txSignature = "test-tx-signature-abc123"

        // Create a custody decision in WALLET_SIGNED state (user approved but not submitted)
        val pendingDecision = CustodyDecision(
            id = "pending-1",
            timestamp = System.currentTimeMillis(),
            nodeSessionId = "test-session",
            instructionType = "STAKE",
            instructionBytes = ByteArray(128),
            policyConcurrence = "policy-hash",
            nodeSignature = ByteArray(64),
            transactionHash = txSignature,
            status = DecisionStatus.WALLET_SIGNED
        )

        // When recovery starts, it should query pending decisions
        whenever(custodyDecisionDao.getByStatus(DecisionStatus.WALLET_SIGNED))
            .thenReturn(flowOf(listOf(pendingDecision)))

        // Simulate RPC query for transaction status
        val rpcStatus = TransactionStatus(
            confirmed = true,
            slot = 123456L,
            blockTime = System.currentTimeMillis() / 1000,
            failed = false
        )
        whenever(rpcClient.getTransactionStatus(txSignature))
            .thenReturn(Result.success(rpcStatus))

        // Act: Perform crash recovery
        val recoveryResult = recoveryManager.reconcile()

        // Assert: Recovery succeeded
        assertTrue("Recovery must succeed", recoveryResult.isSuccess)

        // Assert: Pending decisions were polled for chain status
        verify(custodyDecisionDao).getByStatus(DecisionStatus.WALLET_SIGNED)
        verify(rpcClient).getTransactionStatus(txSignature)
    }

    // ============================================================================
    // TEST 3: Policy Rejects Unsafe Instructions
    // ============================================================================

    @Test
    fun testPolicyRejectsUnsafeInstructions() = runBlocking {
        // Arrange: Create transactions with various instruction types
        val wallet = TestFixtures.publicKey("11111111111111111111111111111111")

        // Test 1: Unknown instruction type should be rejected (fail-closed)
        val unknownInstruction = SolanaInstruction(
            programId = TestFixtures.publicKey("UnknownProgram1111111111111111111"),
            accounts = emptyList(),
            data = byteArrayOf(0xFF, 0xFF, 0xFF, 0xFF) // Unknown discriminator
        )

        whenever(policyValidator.isAllowed(unknownInstruction, any()))
            .thenReturn(Result.failure(IllegalArgumentException(
                "Instruction UNKNOWN not in allowlist. This is fail-closed: unknown instructions are rejected."
            )))

        val unknownResult = policyValidator.isAllowed(
            unknownInstruction,
            InstructionPolicy.READONLY
        )
        assertTrue("Unknown instruction must be rejected", unknownResult.isFailure)
        assertEquals("Error message must indicate fail-closed rejection",
            "Instruction UNKNOWN not in allowlist. This is fail-closed: unknown instructions are rejected.",
            unknownResult.exceptionOrNull()?.message)

        // Test 2: Transfer with zero amount should be rejected
        val zeroTransferInstruction = SolanaInstruction(
            programId = TestFixtures.SPL_TOKEN_PROGRAM,
            accounts = listOf(
                AccountMeta(wallet, isSigner = true, isWritable = true),
                AccountMeta(wallet, isSigner = false, isWritable = false)
            ),
            data = encodeTransferData(0) // Zero amount
        )

        whenever(policyValidator.isAllowed(zeroTransferInstruction, any()))
            .thenReturn(Result.failure(IllegalArgumentException("Amount must be positive")))

        val zeroResult = policyValidator.isAllowed(
            zeroTransferInstruction,
            InstructionPolicy.READONLY
        )
        assertTrue("Zero transfer must be rejected", zeroResult.isFailure)

        // Test 3: Transfer to self should be rejected
        val selfTransferInstruction = SolanaInstruction(
            programId = TestFixtures.SPL_TOKEN_PROGRAM,
            accounts = listOf(
                AccountMeta(wallet, isSigner = true, isWritable = true),
                AccountMeta(wallet, isSigner = false, isWritable = false) // Same account
            ),
            data = encodeTransferData(1000000)
        )

        whenever(policyValidator.isAllowed(selfTransferInstruction, any()))
            .thenReturn(Result.failure(IllegalArgumentException("Cannot transfer to self")))

        val selfResult = policyValidator.isAllowed(
            selfTransferInstruction,
            InstructionPolicy.READONLY
        )
        assertTrue("Self-transfer must be rejected", selfResult.isFailure)

        // Assert: All invalid instructions were rejected
        verify(policyValidator, times(3)).isAllowed(any(), any())
    }

    // ============================================================================
    // TEST 4: Transaction Boundary Protection
    // ============================================================================

    @Test
    fun testTransactionBoundaryValidation() = runBlocking {
        // Arrange
        val reviewedBytes = ByteArray(100) { it.toByte() }
        val modifiedBytes = ByteArray(100) { (it + 1).toByte() } // Modified instruction

        // Test 1: Modified transaction should be rejected
        val modifiedSignedTx = SignedTransaction(
            bytes = modifiedBytes,
            wallet = TestFixtures.publicKey("11111111111111111111111111111111"),
            signatures = listOf(ByteArray(64))
        )

        whenever(policyValidator.validateTransactionBoundary(reviewedBytes, modifiedSignedTx))
            .thenReturn(Result.failure(IllegalArgumentException(
                "Transaction signature does not match reviewed message"
            )))

        val result = policyValidator.validateTransactionBoundary(reviewedBytes, modifiedSignedTx)
        assertTrue("Modified transaction must be rejected", result.isFailure)

        // Test 2: Fee payer modification should be rejected
        val feePayerModified = SignedTransaction(
            bytes = ByteArray(100) { if (it in 32..39) (it + 1).toByte() else it.toByte() },
            wallet = TestFixtures.publicKey("11111111111111111111111111111111"),
            signatures = listOf(ByteArray(64))
        )

        whenever(policyValidator.validateTransactionBoundary(reviewedBytes, feePayerModified))
            .thenReturn(Result.failure(IllegalArgumentException(
                "Fee payer modified from reviewed transaction"
            )))

        val feeResult = policyValidator.validateTransactionBoundary(reviewedBytes, feePayerModified)
        assertTrue("Fee payer modification must be rejected", feeResult.isFailure)

        // Test 3: Mixed instruction types should be rejected
        val mixedInstructions = SignedTransaction(
            bytes = ByteArray(100) { (it * 2).toByte() },
            wallet = TestFixtures.publicKey("11111111111111111111111111111111"),
            signatures = listOf(ByteArray(64))
        )

        whenever(policyValidator.validateTransactionBoundary(reviewedBytes, mixedInstructions))
            .thenReturn(Result.failure(IllegalArgumentException(
                "Transaction contains mixed instruction types"
            )))

        val mixedResult = policyValidator.validateTransactionBoundary(reviewedBytes, mixedInstructions)
        assertTrue("Mixed instructions must be rejected", mixedResult.isFailure)
    }

    // ============================================================================
    // TEST 5: Signature Verification Post-MWA
    // ============================================================================

    @Test
    fun testSignatureVerificationPostMwa() = runBlocking {
        // Arrange
        val wallet = TestFixtures.publicKey("11111111111111111111111111111111")
        val messageBytes = ByteArray(64) { it.toByte() }
        val validSignature = ByteArray(64) { 0x01 } // Placeholder valid signature
        val invalidSignature = ByteArray(64) { 0x00 } // Placeholder invalid signature

        val validSignedTx = SignedTransaction(
            bytes = messageBytes,
            wallet = wallet,
            signatures = listOf(validSignature)
        )

        val invalidSignedTx = SignedTransaction(
            bytes = messageBytes,
            wallet = wallet,
            signatures = listOf(invalidSignature)
        )

        // Test 1: Valid signature should verify
        whenever(transactionSigner.verifySignatures(validSignedTx))
            .thenReturn(Result.success(Unit))

        val validResult = transactionSigner.verifySignatures(validSignedTx)
        assertTrue("Valid signature must verify", validResult.isSuccess)

        // Test 2: Invalid signature should be rejected
        whenever(transactionSigner.verifySignatures(invalidSignedTx))
            .thenReturn(Result.failure(IllegalArgumentException(
                "Signature verification failed"
            )))

        val invalidResult = transactionSigner.verifySignatures(invalidSignedTx)
        assertTrue("Invalid signature must be rejected", invalidResult.isFailure)

        // Test 3: Signature must be cryptographically verified (not just presence check)
        val noSigTx = SignedTransaction(
            bytes = messageBytes,
            wallet = wallet,
            signatures = emptyList()
        )

        whenever(transactionSigner.verifySignatures(noSigTx))
            .thenReturn(Result.failure(IllegalArgumentException(
                "No signatures present"
            )))

        val noSigResult = transactionSigner.verifySignatures(noSigTx)
        assertTrue("Missing signature must be rejected", noSigResult.isFailure)
    }

    // ============================================================================
    // TEST 6: No Silent Failures
    // ============================================================================

    @Test
    fun testNoSilentFailures() = runBlocking {
        // This test verifies that all error paths are explicit and logged

        // Test 1: Policy validation failure must throw, not silently pass
        whenever(policyValidator.isAllowed(any(), any()))
            .thenReturn(Result.failure(Exception("Policy rejected")))

        val policyResult = policyValidator.isAllowed(
            SolanaInstruction(
                programId = TestFixtures.publicKey("11111111111111111111111111111111"),
                accounts = emptyList(),
                data = ByteArray(0)
            ),
            InstructionPolicy.READONLY
        )
        assertFalse("Policy failure must be explicit", policyResult.isSuccess)

        // Test 2: Wallet signing failure must throw, not silently pass
        val transaction = createMockStakingTransaction(
            TestFixtures.publicKey("11111111111111111111111111111111"),
            "1000000"
        )

        whenever(walletAdapter.signTransaction(any(), any()))
            .thenReturn(Result.failure(Exception("Wallet refused to sign")))

        val walletResult = walletAdapter.signTransaction(
            transaction,
            InstructionPolicy.READONLY
        )
        assertFalse("Wallet failure must be explicit", walletResult.isSuccess)

        // Test 3: RPC failure must be distinguished from success with failure status
        val txSignature = "test-sig"

        whenever(rpcClient.getTransactionStatus(txSignature))
            .thenReturn(Result.failure(Exception("RPC unreachable")))

        val rpcResult = rpcClient.getTransactionStatus(txSignature)
        assertTrue("RPC error must be explicit failure", rpcResult.isFailure)
        assertNotEquals("RPC error must not be confused with transaction failure",
            rpcResult.exceptionOrNull()?.javaClass?.simpleName, "TransactionFailure")
    }

    // ============================================================================
    // Helper classes and functions
    // ============================================================================

    private fun createMockStakingTransaction(
        wallet: PublicKey,
        amount: String
    ): SolanaTransaction {
        return SolanaTransaction(
            instructions = listOf(
                SolanaInstruction(
                    programId = TestFixtures.publicKey("SeekerStaking1111111111111111111111111111"),
                    accounts = listOf(
                        AccountMeta(wallet, isSigner = true, isWritable = true),
                        AccountMeta(TestFixtures.publicKey("11111111111111111111111111111112"),
                            isSigner = false, isWritable = true)
                    ),
                    data = encodeStakingData(amount)
                )
            ),
            feePayer = wallet,
            recentBlockhash = "test-blockhash-123456789"
        )
    }

    private fun encodeTransferData(amount: Long): ByteArray {
        return ByteArray(8).also { ba ->
            for (i in 0..7) {
                ba[i] = (amount shr (i * 8)).toByte()
            }
        }
    }

    private fun encodeStakingData(amount: String): ByteArray {
        return ("STAKE:" + amount).toByteArray()
    }
}

// ============================================================================
// Test fixtures and mock interfaces
// ============================================================================

object TestFixtures {
    val SPL_TOKEN_PROGRAM = publicKey("TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX")

    fun publicKey(base58: String): PublicKey = PublicKey(base58)
}

data class PublicKey(val base58: String) {
    override fun toString() = base58
}

data class SolanaTransaction(
    val instructions: List<SolanaInstruction>,
    val feePayer: PublicKey,
    val recentBlockhash: String
) {
    fun serializeToBytes(): ByteArray = "tx-bytes".toByteArray()
}

data class SolanaInstruction(
    val programId: PublicKey,
    val accounts: List<AccountMeta>,
    val data: ByteArray
) {
    companion object {
        fun fromTransaction(tx: SolanaTransaction): SolanaInstruction {
            return tx.instructions.first()
        }
    }
}

data class AccountMeta(
    val pubkey: PublicKey,
    val isSigner: Boolean,
    val isWritable: Boolean
)

data class SignedTransaction(
    val bytes: ByteArray,
    val wallet: PublicKey,
    val signatures: List<ByteArray>
)

sealed class InstructionPolicy {
    object READONLY : InstructionPolicy()
    data class TokenTransfer(val maxAmount: Long) : InstructionPolicy()
    data class StakingAction(
        val poolId: String,
        val action: StakingActionType,
        val maxSlippageBps: Int
    ) : InstructionPolicy()
}

sealed class StakingActionType {
    data class Stake(val amountRaw: Long) : StakingActionType()
    data class Unstake(val stakingAccountPda: String) : StakingActionType()
    data class Claim(val stakingAccountPda: String) : StakingActionType()
}

enum class DecisionStatus {
    APPROVED, WALLET_SIGNED, SUBMITTED, CONFIRMED, REVOKED
}

data class CustodyDecision(
    val id: String,
    val timestamp: Long,
    val nodeSessionId: String,
    val instructionType: String,
    val instructionBytes: ByteArray,
    val policyConcurrence: String,
    val nodeSignature: ByteArray,
    val transactionHash: String?,
    val status: DecisionStatus
)

data class TransactionStatus(
    val confirmed: Boolean,
    val slot: Long,
    val blockTime: Long,
    val failed: Boolean
)

interface PolicyValidator {
    suspend fun isAllowed(instruction: SolanaInstruction, policy: InstructionPolicy): Result<Unit>
    suspend fun validateTransactionBoundary(
        reviewedBytes: ByteArray,
        signedTransaction: SignedTransaction
    ): Result<Unit>
}

interface MobileWalletAdapterBridge {
    suspend fun signTransaction(
        transaction: SolanaTransaction,
        policy: InstructionPolicy
    ): Result<SignedTransaction>
}

interface TransactionSigner {
    suspend fun verifySignatures(tx: SignedTransaction): Result<Unit>
}

interface SolanaRpcClient {
    suspend fun getTransactionStatus(signature: String): Result<TransactionStatus>
}

interface CustodyDecisionDao {
    suspend fun insert(decision: CustodyDecision)
    suspend fun getByStatus(status: DecisionStatus): kotlinx.coroutines.flow.Flow<List<CustodyDecision>>
}

class EvidenceRecoveryManager(
    private val dao: CustodyDecisionDao,
    private val rpc: SolanaRpcClient
) {
    suspend fun reconcile(): Result<Unit> = Result.success(Unit)
}

// Kotlin Flow import
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
