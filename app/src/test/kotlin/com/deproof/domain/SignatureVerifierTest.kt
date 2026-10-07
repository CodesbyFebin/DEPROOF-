package com.deproof.domain

import com.deproof.data.solana.SignedTransaction
import com.deproof.domain.model.Instruction
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignatureVerifierTest {

    private lateinit var verifier: SignatureVerifier

    @Before
    fun setup() {
        verifier = SignatureVerifier()
    }

    @Test
    fun `verify should accept valid signature`() {
        val messageBytes = "test message".toByteArray()
        val signedTx = SignedTransaction(
            signature = "valid_sig_64_bytes_minimum".padEnd(64, '0'),
            publicKey = "valid_public_key"
        )

        val result = verifier.verify(messageBytes, signedTx)

        assertTrue(result.isSuccess)
        val verification = result.getOrNull()
        assertTrue(verification?.isValid == true)
        assertTrue(verification?.detectedIssues?.isEmpty() == true)
    }

    @Test
    fun `verify should detect short signature`() {
        val messageBytes = "test message".toByteArray()
        val signedTx = SignedTransaction(
            signature = "short_sig",
            publicKey = "valid_public_key"
        )

        val result = verifier.verify(messageBytes, signedTx)

        assertTrue(result.isSuccess)
        val verification = result.getOrNull()
        assertFalse(verification?.isValid == true)
        assertTrue(verification?.detectedIssues?.any { it.contains("length") } == true)
    }

    @Test
    fun `verify should detect empty public key`() {
        val messageBytes = "test message".toByteArray()
        val signedTx = SignedTransaction(
            signature = "valid_sig_64_bytes_minimum".padEnd(64, '0'),
            publicKey = ""
        )

        val result = verifier.verify(messageBytes, signedTx)

        assertTrue(result.isSuccess)
        val verification = result.getOrNull()
        assertFalse(verification?.isValid == true)
        assertTrue(verification?.detectedIssues?.any { it.contains("Public key") } == true)
    }

    @Test
    fun `verifyTransactionNotModified should pass for identical bytes`() {
        val originalBytes = "transaction data".toByteArray()
        val currentBytes = "transaction data".toByteArray()

        val result = verifier.verifyTransactionNotModified(originalBytes, currentBytes)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `verifyTransactionNotModified should fail for size mismatch`() {
        val originalBytes = "transaction data".toByteArray()
        val modifiedBytes = "modified transaction data".toByteArray()

        val result = verifier.verifyTransactionNotModified(originalBytes, modifiedBytes)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("size changed") == true)
    }

    @Test
    fun `verifyTransactionNotModified should fail for content change`() {
        val originalBytes = byteArrayOf(1, 2, 3, 4)
        val modifiedBytes = byteArrayOf(1, 2, 3, 5) // Different last byte, same size

        val result = verifier.verifyTransactionNotModified(originalBytes, modifiedBytes)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("modified") == true)
    }

    @Test
    fun `verifyInstructionCount should pass for matching counts`() {
        val instructions1 = listOf(
            Instruction("prog1", 0, listOf("acc1"), ByteArray(0))
        )
        val instructions2 = listOf(
            Instruction("prog2", 1, listOf("acc2"), ByteArray(0))
        )

        val result = verifier.verifyInstructionCount(instructions1, instructions2)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `verifyInstructionCount should fail for different counts`() {
        val instructions1 = listOf(
            Instruction("prog1", 0, listOf("acc1"), ByteArray(0))
        )
        val instructions2 = listOf(
            Instruction("prog2", 1, listOf("acc2"), ByteArray(0)),
            Instruction("prog3", 2, listOf("acc3"), ByteArray(0))
        )

        val result = verifier.verifyInstructionCount(instructions1, instructions2)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("Instruction count changed") == true)
    }

    @Test
    fun `verifyFeeNotModified should pass for matching fees`() {
        val result = verifier.verifyFeeNotModified(5000, 5000)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `verifyFeeNotModified should fail for different fees`() {
        val result = verifier.verifyFeeNotModified(5000, 10000)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("fee modified") == true)
    }

    @Test
    fun `verifyPayerNotModified should pass for matching payers`() {
        val payer = "EPjFWaJyUCND5QKu6Yp9xQrHT4fJd1exG1MsxV5GhHLU"
        val result = verifier.verifyPayerNotModified(payer, payer)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `verifyPayerNotModified should fail for different payers`() {
        val originalPayer = "EPjFWaJyUCND5QKu6Yp9xQrHT4fJd1exG1MsxV5GhHLU"
        val modifiedPayer = "TokenkegQfeZyiNwAJsyFbPVwwQQfg5bgUripnT2m7g"

        val result = verifier.verifyPayerNotModified(originalPayer, modifiedPayer)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("payer modified") == true)
    }

    @Test
    fun `verifyNoMixedInstructions should pass for single program`() {
        val instructions = listOf(
            Instruction("TokenkegQfeZyiNwAJsyFbPVwwQQfg5bgUripnT2m7g", 0, listOf("acc1"), ByteArray(0)),
            Instruction("TokenkegQfeZyiNwAJsyFbPVwwQQfg5bgUripnT2m7g", 1, listOf("acc2"), ByteArray(0))
        )

        val result = verifier.verifyNoMixedInstructions(instructions)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `verifyNoMixedInstructions should fail for multiple programs`() {
        val instructions = listOf(
            Instruction("TokenkegQfeZyiNwAJsyFbPVwwQQfg5bgUripnT2m7g", 0, listOf("acc1"), ByteArray(0)),
            Instruction("Stake11111111111111111111111111111111111111", 1, listOf("acc2"), ByteArray(0))
        )

        val result = verifier.verifyNoMixedInstructions(instructions)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("Mixed instructions") == true)
    }

    @Test
    fun `verifyCompleteTransaction should validate transaction integrity`() {
        val messageBytes = "transaction data".toByteArray()
        val signedTx = SignedTransaction(
            signature = "valid_sig_64_bytes_minimum".padEnd(64, '0'),
            publicKey = "valid_public_key"
        )

        val result = verifier.verifyCompleteTransaction(
            messageBytes,
            signedTx,
            TransactionConstraints()
        )

        assertTrue(result.isSuccess)
        val verification = result.getOrNull()
        assertTrue(verification?.isValid == true)
    }
}
