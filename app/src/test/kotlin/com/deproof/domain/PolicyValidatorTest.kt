package com.deproof.domain

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PolicyValidatorTest {

    private lateinit var validator: FailClosedPolicyValidator

    @Before
    fun setUp() {
        validator = FailClosedPolicyValidator()
    }

    // ===== READONLY POLICY TESTS =====

    @Test
    fun `readonly policy rejects any instruction`() = runTest {
        val instruction = SolanaInstruction(
            programId = PublicKey("11111111111111111111111111111111"),
            accounts = emptyList(),
            data = byteArrayOf()
        )

        val result = validator.isAllowed(instruction, InstructionPolicy.ReadOnly)
        assertTrue(result.isFailure)
    }

    // ===== TOKEN TRANSFER POLICY TESTS =====

    @Test
    fun `token transfer policy validates correct destination`() = runTest {
        val policy = InstructionPolicy.TokenTransfer(
            mint = PublicKey("EPjFWaLb3odcccccccccccccccccccccccccccccccc"),
            maxAmount = 1_000_000L,
            destinationWhitelist = setOf("11111111111111111111111111111111")
        )

        val instruction = SolanaInstruction(
            programId = PublicKey("TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX"),
            accounts = listOf(
                AccountMeta(PublicKey("2222222222222222222222222222222"), isSigner = false, isWritable = true),
                AccountMeta(PublicKey("11111111111111111111111111111111"), isSigner = false, isWritable = true),
                AccountMeta(PublicKey("3333333333333333333333333333333"), isSigner = true, isWritable = false)
            ),
            data = byteArrayOf()
        )

        val result = validator.isAllowed(instruction, policy)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `token transfer policy rejects unlisted destination`() = runTest {
        val policy = InstructionPolicy.TokenTransfer(
            mint = PublicKey("EPjFWaLb3odcccccccccccccccccccccccccccccccc"),
            maxAmount = 1_000_000L,
            destinationWhitelist = setOf("11111111111111111111111111111111")
        )

        val instruction = SolanaInstruction(
            programId = PublicKey("TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX"),
            accounts = listOf(
                AccountMeta(PublicKey("2222222222222222222222222222222"), isSigner = false, isWritable = true),
                AccountMeta(PublicKey("99999999999999999999999999999999"), isSigner = false, isWritable = true),
                AccountMeta(PublicKey("3333333333333333333333333333333"), isSigner = true, isWritable = false)
            ),
            data = byteArrayOf()
        )

        val result = validator.isAllowed(instruction, policy)
        assertTrue(result.isFailure)
    }

    @Test
    fun `token transfer policy rejects wrong program`() = runTest {
        val policy = InstructionPolicy.TokenTransfer(
            mint = PublicKey("EPjFWaLb3odcccccccccccccccccccccccccccccccc"),
            maxAmount = 1_000_000L,
            destinationWhitelist = setOf("11111111111111111111111111111111")
        )

        val instruction = SolanaInstruction(
            programId = PublicKey("11111111111111111111111111111111"),  // Wrong program
            accounts = listOf(
                AccountMeta(PublicKey("2222222222222222222222222222222"), isSigner = false, isWritable = true),
                AccountMeta(PublicKey("11111111111111111111111111111111"), isSigner = false, isWritable = true),
                AccountMeta(PublicKey("3333333333333333333333333333333"), isSigner = true, isWritable = false)
            ),
            data = byteArrayOf()
        )

        val result = validator.isAllowed(instruction, policy)
        assertTrue(result.isFailure)
    }

    @Test
    fun `token transfer policy requires minimum accounts`() = runTest {
        val policy = InstructionPolicy.TokenTransfer(
            mint = PublicKey("EPjFWaLb3odcccccccccccccccccccccccccccccccc"),
            maxAmount = 1_000_000L,
            destinationWhitelist = setOf("11111111111111111111111111111111")
        )

        val instruction = SolanaInstruction(
            programId = PublicKey("TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX"),
            accounts = listOf(
                AccountMeta(PublicKey("2222222222222222222222222222222"), isSigner = false, isWritable = true)
            ),
            data = byteArrayOf()
        )

        val result = validator.isAllowed(instruction, policy)
        assertTrue(result.isFailure)
    }

    // ===== SOL TRANSFER POLICY TESTS =====

    @Test
    fun `sol transfer policy validates correct destination`() = runTest {
        val policy = InstructionPolicy.SolTransfer(
            maxAmount = 10_000_000_000L,  // 10 SOL
            destinationWhitelist = setOf("11111111111111111111111111111111")
        )

        val instruction = SolanaInstruction(
            programId = PublicKey("11111111111111111111111111111111"),
            accounts = listOf(
                AccountMeta(PublicKey("2222222222222222222222222222222"), isSigner = true, isWritable = true),
                AccountMeta(PublicKey("11111111111111111111111111111111"), isSigner = false, isWritable = true)
            ),
            data = byteArrayOf()
        )

        val result = validator.isAllowed(instruction, policy)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `sol transfer policy rejects unlisted destination`() = runTest {
        val policy = InstructionPolicy.SolTransfer(
            maxAmount = 10_000_000_000L,
            destinationWhitelist = setOf("11111111111111111111111111111111")
        )

        val instruction = SolanaInstruction(
            programId = PublicKey("11111111111111111111111111111111"),
            accounts = listOf(
                AccountMeta(PublicKey("2222222222222222222222222222222"), isSigner = true, isWritable = true),
                AccountMeta(PublicKey("99999999999999999999999999999999"), isSigner = false, isWritable = true)
            ),
            data = byteArrayOf()
        )

        val result = validator.isAllowed(instruction, policy)
        assertTrue(result.isFailure)
    }

    // ===== STAKING POLICY TESTS =====

    @Test
    fun `staking policy validates delegate operation`() = runTest {
        val policy = InstructionPolicy.StakingAction(
            poolId = PublicKey("PoolID11111111111111111111111111"),
            allowedOperations = setOf(
                StakingOperationType.DELEGATE,
                StakingOperationType.STAKE
            ),
            maxSlippageBps = 500
        )

        val instruction = SolanaInstruction(
            programId = PublicKey("Stake11111111111111111111111111111111111111"),
            accounts = listOf(
                AccountMeta(PublicKey("2222222222222222222222222222222"), isSigner = false, isWritable = true),
                AccountMeta(PublicKey("3333333333333333333333333333333"), isSigner = false, isWritable = false),
                AccountMeta(PublicKey("4444444444444444444444444444444"), isSigner = false, isWritable = false),
                AccountMeta(PublicKey("5555555555555555555555555555555"), isSigner = true, isWritable = false)
            ),
            data = byteArrayOf()
        )

        val result = validator.isAllowed(instruction, policy)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `staking policy rejects invalid slippage`() = runTest {
        val result = runCatching {
            InstructionPolicy.StakingAction(
                poolId = PublicKey("PoolID11111111111111111111111111"),
                allowedOperations = setOf(StakingOperationType.STAKE),
                maxSlippageBps = 15000  // > 10000 bps
            )
        }

        assertTrue(result.isFailure)
    }

    // ===== DEFI SWAP POLICY TESTS =====

    @Test
    fun `defi swap policy validates token pair`() = runTest {
        val policy = InstructionPolicy.DeFiSwap(
            poolId = PublicKey("PoolID11111111111111111111111111"),
            tokenInMint = PublicKey("EPjFWaLb3odcccccccccccccccccccccccccccccccc"),
            tokenOutMint = PublicKey("SRMuGgxvcccccccccccccccccccccccccccccccccccc"),
            maxAmountIn = 1_000_000L,
            minAmountOut = 100L,
            maxSlippageBps = 500
        )

        val instruction = SolanaInstruction(
            programId = PublicKey("PoolID11111111111111111111111111"),
            accounts = listOf(
                AccountMeta(PublicKey("2222222222222222222222222222222"), isSigner = false, isWritable = true),
                AccountMeta(PublicKey("3333333333333333333333333333333"), isSigner = false, isWritable = true)
            ),
            data = byteArrayOf()
        )

        val result = validator.isAllowed(instruction, policy)
        assertTrue(result.isSuccess)
    }

    // ===== NFT POLICY TESTS =====

    @Test
    fun `nft mint policy validates metaplex program`() = runTest {
        val policy = InstructionPolicy.NftMintPolicy(
            metaplexProgram = PublicKey("metaqbxxUerdq28cj1RbAqKEsbh1HkMi8cHz92RsmjK"),
            maxSupply = 10_000L,
            allowedCreators = setOf("Creator11111111111111111111111111")
        )

        val instruction = SolanaInstruction(
            programId = PublicKey("metaqbxxUerdq28cj1RbAqKEsbh1HkMi8cHz92RsmjK"),
            accounts = listOf(
                AccountMeta(PublicKey("2222222222222222222222222222222"), isSigner = false, isWritable = true),
                AccountMeta(PublicKey("3333333333333333333333333333333"), isSigner = false, isWritable = true),
                AccountMeta(PublicKey("Creator11111111111111111111111111"), isSigner = true, isWritable = false)
            ),
            data = byteArrayOf()
        )

        val result = validator.isAllowed(instruction, policy)
        assertTrue(result.isSuccess)
    }

    // ===== TRANSACTION BOUNDARY TESTS =====

    @Test
    fun `transaction boundary validation accepts non-empty bytes`() = runTest {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)

        val result = validator.validateTransactionBoundary(bytes, null)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `transaction boundary validation rejects empty bytes`() = runTest {
        val result = validator.validateTransactionBoundary(byteArrayOf(), null)
        assertTrue(result.isFailure)
    }

    @Test
    fun `transaction boundary validation rejects oversized transactions`() = runTest {
        val hugeBytes = ByteArray(2_000_000)

        val result = validator.validateTransactionBoundary(hugeBytes, null)
        assertTrue(result.isFailure)
    }

    @Test
    fun `transaction boundary validates signed transaction signatures`() = runTest {
        val signedTx = SignedTransaction(
            bytes = byteArrayOf(1, 2, 3, 4, 5),
            wallet = PublicKey("2222222222222222222222222222222"),
            signatures = listOf(ByteArray(64))
        )

        val result = validator.validateTransactionBoundary(byteArrayOf(1, 2, 3), signedTx)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `transaction boundary rejects invalid signature length`() = runTest {
        val signedTx = SignedTransaction(
            bytes = byteArrayOf(1, 2, 3, 4, 5),
            wallet = PublicKey("2222222222222222222222222222222"),
            signatures = listOf(ByteArray(32))  // Wrong size
        )

        val result = validator.validateTransactionBoundary(byteArrayOf(1, 2, 3), signedTx)
        assertTrue(result.isFailure)
    }

    // ===== ACTION VALIDATION TESTS =====

    @Test
    fun `action validation validates action first`() = runTest {
        val action = SolanaAction.TransferSplToken(
            mint = PublicKey("EPjFWaLb3odcccccccccccccccccccccccccccccccc"),
            from = PublicKey("2222222222222222222222222222222"),
            to = PublicKey("3333333333333333333333333333333"),
            amount = 1_000_000L,
            decimals = 6,
            owner = PublicKey("4444444444444444444444444444444")
        )

        val policy = InstructionPolicy.TokenTransfer(
            mint = PublicKey("EPjFWaLb3odcccccccccccccccccccccccccccccccc"),
            maxAmount = 1_000_000L,
            destinationWhitelist = setOf("3333333333333333333333333333333")
        )

        val result = validator.validateAction(action, policy)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `action validation rejects invalid decimals`() = runTest {
        val action = SolanaAction.TransferSplToken(
            mint = PublicKey("EPjFWaLb3odcccccccccccccccccccccccccccccccc"),
            from = PublicKey("2222222222222222222222222222222"),
            to = PublicKey("3333333333333333333333333333333"),
            amount = 1_000_000L,
            decimals = 20,  // Invalid
            owner = PublicKey("4444444444444444444444444444444")
        )

        val policy = InstructionPolicy.TokenTransfer(
            mint = PublicKey("EPjFWaLb3odcccccccccccccccccccccccccccccccc"),
            maxAmount = 1_000_000L,
            destinationWhitelist = setOf("3333333333333333333333333333333")
        )

        val result = validator.validateAction(action, policy)
        assertTrue(result.isFailure)
    }

    @Test
    fun `action validation rejects zero amount`() = runTest {
        val action = SolanaAction.TransferSplToken(
            mint = PublicKey("EPjFWaLb3odcccccccccccccccccccccccccccccccc"),
            from = PublicKey("2222222222222222222222222222222"),
            to = PublicKey("3333333333333333333333333333333"),
            amount = 0L,  // Invalid
            decimals = 6,
            owner = PublicKey("4444444444444444444444444444444")
        )

        val policy = InstructionPolicy.TokenTransfer(
            mint = PublicKey("EPjFWaLb3odcccccccccccccccccccccccccccccccc"),
            maxAmount = 1_000_000L,
            destinationWhitelist = setOf("3333333333333333333333333333333")
        )

        assertFailsWith<IllegalArgumentException> {
            val result = validator.validateAction(action, policy)
            result.getOrThrow()
        }
    }

    // ===== COMPOSITE POLICY TESTS =====

    @Test
    fun `composite policy with requireAll validates all sub-policies`() = runTest {
        val policy1 = InstructionPolicy.ReadOnly
        val policy2 = InstructionPolicy.SolTransfer(
            maxAmount = 10_000_000_000L,
            destinationWhitelist = setOf("11111111111111111111111111111111")
        )

        val compositePolicy = InstructionPolicy.CompositePolicy(
            policies = listOf(policy1, policy2),
            requireAll = true
        )

        val instruction = SolanaInstruction(
            programId = PublicKey("11111111111111111111111111111111"),
            accounts = emptyList(),
            data = byteArrayOf()
        )

        // Should fail because ReadOnly rejects all instructions
        val result = validator.isAllowed(instruction, compositePolicy)
        assertTrue(result.isFailure)
    }

    // ===== FAIL-CLOSED SEMANTICS VERIFICATION =====

    @Test
    fun `unknown program is rejected`() = runTest {
        val policy = InstructionPolicy.ReadOnly

        val instruction = SolanaInstruction(
            programId = PublicKey("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"),  // Unknown program
            accounts = emptyList(),
            data = byteArrayOf()
        )

        val result = validator.isAllowed(instruction, policy)
        assertTrue(result.isFailure)
    }

    @Test
    fun `malformed instruction is rejected`() = runTest {
        val policy = InstructionPolicy.TokenTransfer(
            mint = PublicKey("EPjFWaLb3odcccccccccccccccccccccccccccccccc"),
            maxAmount = 1_000_000L,
            destinationWhitelist = setOf("11111111111111111111111111111111")
        )

        // Instruction with wrong program and no accounts
        val instruction = SolanaInstruction(
            programId = PublicKey("11111111111111111111111111111111"),
            accounts = emptyList(),
            data = byteArrayOf()
        )

        val result = validator.isAllowed(instruction, policy)
        assertTrue(result.isFailure)
    }

    @Test
    fun `signature validation requires correct length`() = runTest {
        val tx = SignedTransaction(
            bytes = byteArrayOf(1, 2, 3),
            wallet = PublicKey("2222222222222222222222222222222"),
            signatures = listOf(ByteArray(32))  // Too short
        )

        val result = validator.validateSignatures(tx, listOf(PublicKey("2222222222222222222222222222222")))
        assertTrue(result.isFailure)
    }
}
