package com.deproof.domain

import com.deproof.domain.model.Instruction
import com.deproof.domain.InstructionPolicy.TokenTransfer
import com.deproof.domain.InstructionPolicy.StakingAction
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolicyValidatorTest {

    private lateinit var validator: PolicyValidator

    @Before
    fun setup() {
        validator = PolicyValidator()
    }

    @Test
    fun `validateInstructionPolicy should pass for readonly policy`() {
        val instruction = Instruction(
            programId = "any_program",
            discriminator = 0,
            accounts = listOf("acc1"),
            data = ByteArray(0)
        )

        val result = validator.validateInstructionPolicy(
            instruction,
            InstructionPolicy.READONLY
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateInstructionPolicy should validate token transfer policy`() {
        val instruction = Instruction(
            programId = TokenTransfer.TOKEN_PROGRAM_ID,
            discriminator = 3,
            accounts = listOf("mint", "source", "dest", "owner"),
            data = ByteArray(0)
        )

        val policy = TokenTransfer(
            mint = "EPjFWaJyUCND5QKu6Yp9xQrHT4fJd1exG1MsxV5GhHLU",
            maxAmount = 1000000,
            destinationWhitelist = setOf("dest_address")
        )

        val result = validator.validateInstructionPolicy(instruction, policy)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateInstructionPolicy should reject non-token program for token transfer`() {
        val instruction = Instruction(
            programId = "wrong_program_id",
            discriminator = 3,
            accounts = listOf("mint", "source", "dest", "owner"),
            data = ByteArray(0)
        )

        val policy = TokenTransfer(
            mint = "EPjFWaJyUCND5QKu6Yp9xQrHT4fJd1exG1MsxV5GhHLU",
            maxAmount = 1000000,
            destinationWhitelist = setOf("dest_address")
        )

        val result = validator.validateInstructionPolicy(instruction, policy)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("Not a token program") == true)
    }

    @Test
    fun `validateTransactionBoundary should pass for unmodified transaction`() {
        val messageBytes = "tx_data".toByteArray()
        val currentBytes = "tx_data".toByteArray()
        val instructions = listOf(
            Instruction("prog1", 0, listOf("acc1"), ByteArray(0))
        )
        val constraints = ActionConstraints(maxInstructionCount = 5)

        val result = validator.validateTransactionBoundary(
            messageBytes,
            currentBytes,
            instructions,
            constraints
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateTransactionBoundary should fail for modified transaction`() {
        val messageBytes = "tx_data".toByteArray()
        val currentBytes = "modified_tx_data".toByteArray()
        val instructions = listOf(
            Instruction("prog1", 0, listOf("acc1"), ByteArray(0))
        )
        val constraints = ActionConstraints(maxInstructionCount = 5)

        val result = validator.validateTransactionBoundary(
            messageBytes,
            currentBytes,
            instructions,
            constraints
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("size changed") == true)
    }

    @Test
    fun `validateTransactionBoundary should fail for too many instructions`() {
        val messageBytes = "tx_data".toByteArray()
        val currentBytes = "tx_data".toByteArray()
        val instructions = (1..6).map { i ->
            Instruction("prog$i", i.toByte(), listOf("acc$i"), ByteArray(0))
        }
        val constraints = ActionConstraints(maxInstructionCount = 5)

        val result = validator.validateTransactionBoundary(
            messageBytes,
            currentBytes,
            instructions,
            constraints
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("Too many instructions") == true)
    }

    @Test
    fun `validateTransactionBoundary should fail for unauthorized programs`() {
        val messageBytes = "tx_data".toByteArray()
        val currentBytes = "tx_data".toByteArray()
        val instructions = listOf(
            Instruction("unauthorized_program", 0, listOf("acc1"), ByteArray(0))
        )
        val constraints = ActionConstraints(
            maxInstructionCount = 5,
            allowedProgramIds = setOf("TokenkegQfeZyiNwAJsyFbPVwwQQfg5bgUripnT2m7g")
        )

        val result = validator.validateTransactionBoundary(
            messageBytes,
            currentBytes,
            instructions,
            constraints
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("not in whitelist") == true)
    }

    @Test
    fun `validateMixedInstructions should pass for single program`() {
        val instructions = listOf(
            Instruction("TokenkegQfeZyiNwAJsyFbPVwwQQfg5bgUripnT2m7g", 0, listOf("acc1"), ByteArray(0)),
            Instruction("TokenkegQfeZyiNwAJsyFbPVwwQQfg5bgUripnT2m7g", 1, listOf("acc2"), ByteArray(0))
        )

        val result = validator.validateMixedInstructions(instructions)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateMixedInstructions should fail for multiple programs`() {
        val instructions = listOf(
            Instruction("TokenkegQfeZyiNwAJsyFbPVwwQQfg5bgUripnT2m7g", 0, listOf("acc1"), ByteArray(0)),
            Instruction("Stake11111111111111111111111111111111111111", 1, listOf("acc2"), ByteArray(0))
        )

        val result = validator.validateMixedInstructions(instructions)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("Mixed instructions") == true)
    }

    @Test
    fun `validateMixedInstructions should fail for empty instructions`() {
        val instructions = emptyList<Instruction>()

        val result = validator.validateMixedInstructions(instructions)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("No instructions") == true)
    }
}
