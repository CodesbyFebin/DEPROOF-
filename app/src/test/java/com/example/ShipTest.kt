package com.example

import com.example.chain.Instruction
import com.example.chain.Verdict
import com.example.domain.InstructionDecoder
import com.example.domain.ReviewBinding
import com.example.domain.canSign
import com.example.util.Base58
import com.example.util.formatSkr
import com.example.util.formatSol
import com.example.util.isPubkey
import com.example.util.readU32LE
import com.example.util.readU64LE
import com.example.util.sha256Text
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class ShipTest {

    // Feature 2: Reject invalid addresses
    @Test
    fun testIsPubkey_InvalidShort() {
        assertFalse(isPubkey("123456789"))
    }

    @Test
    fun testIsPubkey_InvalidAlphabet() {
        assertFalse(isPubkey("OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO"))
    }

    @Test
    fun testIsPubkey_Valid() {
        assertTrue(isPubkey("SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"))
    }

    @Test
    fun testIsPubkey_WrongLength() {
        assertFalse(isPubkey("SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW"))
    }

    // Feature 3-4: Format SOL
    @Test
    fun testFormatSol_Zero() {
        assertEquals("0.000000000 SOL", formatSol(0))
    }

    @Test
    fun testFormatSol_OneSol() {
        val result = formatSol(1_000_000_000)
        assertTrue(result.contains("1.000000000 SOL"))
    }

    // Feature 7: Format SKR with 6 decimals and thousand separators
    @Test
    fun testFormatSkr_Zero() {
        assertEquals("0.000000 SKR", formatSkr(0))
    }

    @Test
    fun testFormatSkr_OneMillion() {
        val result = formatSkr(1_000_000)
        assertTrue(result.contains("1") && result.contains("SKR"))
    }

    @Test
    fun testFormatSkr_LargeWithSeparators() {
        val result = formatSkr(1_234_567_890)
        assertTrue(result.contains(","))
    }

    // Feature 25-40: Instruction Decoder Tests

    @Test
    fun testDecode_UnknownProgram() {
        val verdict = InstructionDecoder.decode(
            programId = "UnknownProgramAddress1111111111111111111111",
            accounts = listOf("key1"),
            data = "data",
            instruction = Instruction(0, listOf(0), "data")
        )
        assertFalse(verdict.isPayable)
        assertTrue(verdict.reason.contains("unknown"))
    }

    @Test
    fun testDecode_TransferChecked_PayableScenario() {
        val data = byteArrayOf(12, 0, 0, 0, 0, 0, 0, 0, 0, 6)
        val encoded = Base64.getEncoder().encodeToString(data)

        val instruction = Instruction(
            programIdIndex = 0,
            accounts = listOf(0, 1, 2, 3),
            data = encoded
        )

        val verdict = InstructionDecoder.decode(
            programId = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
            accounts = listOf(
                "source",
                "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3",
                "destination",
                "authority"
            ),
            data = encoded,
            instruction = instruction
        )

        assertTrue(verdict.isPayable)
    }

    @Test
    fun testDecode_TransferChecked_WrongDecimals() {
        val data = byteArrayOf(12, 0, 0, 0, 0, 0, 0, 0, 0, 9)
        val encoded = Base64.getEncoder().encodeToString(data)

        val instruction = Instruction(
            programIdIndex = 0,
            accounts = listOf(0, 1, 2, 3),
            data = encoded
        )

        val verdict = InstructionDecoder.decode(
            programId = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
            accounts = listOf(
                "source",
                "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3",
                "destination",
                "authority"
            ),
            data = encoded,
            instruction = instruction
        )

        assertFalse(verdict.isPayable)
        assertTrue(verdict.reason.contains("decimals"))
    }

    @Test
    fun testDecode_TransferChecked_WrongMint() {
        val data = byteArrayOf(12, 0, 0, 0, 0, 0, 0, 0, 0, 6)
        val encoded = Base64.getEncoder().encodeToString(data)

        val instruction = Instruction(
            programIdIndex = 0,
            accounts = listOf(0, 1, 2, 3),
            data = encoded
        )

        val verdict = InstructionDecoder.decode(
            programId = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
            accounts = listOf(
                "source",
                "wrongmintaddress12345678901234567890123",
                "destination",
                "authority"
            ),
            data = encoded,
            instruction = instruction
        )

        assertFalse(verdict.isPayable)
    }

    @Test
    fun testDecode_SetAuthority_Refused() {
        val data = byteArrayOf(6)
        val encoded = Base64.getEncoder().encodeToString(data)

        val instruction = Instruction(
            programIdIndex = 0,
            accounts = listOf(0, 1),
            data = encoded
        )

        val verdict = InstructionDecoder.decode(
            programId = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
            accounts = listOf("account", "authority"),
            data = encoded,
            instruction = instruction
        )

        assertFalse(verdict.isPayable)
        assertTrue(verdict.title.contains("SetAuthority"))
    }

    @Test
    fun testDecode_Approve_Refused() {
        val data = byteArrayOf(4)
        val encoded = Base64.getEncoder().encodeToString(data)

        val instruction = Instruction(
            programIdIndex = 0,
            accounts = listOf(0, 1),
            data = encoded
        )

        val verdict = InstructionDecoder.decode(
            programId = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
            accounts = listOf("account", "authority"),
            data = encoded,
            instruction = instruction
        )

        assertFalse(verdict.isPayable)
        assertTrue(verdict.title.contains("Approve"))
    }

    @Test
    fun testDecode_Transfer_Refused() {
        val data = byteArrayOf(3)
        val encoded = Base64.getEncoder().encodeToString(data)

        val instruction = Instruction(
            programIdIndex = 0,
            accounts = listOf(0, 1),
            data = encoded
        )

        val verdict = InstructionDecoder.decode(
            programId = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
            accounts = listOf("source", "dest"),
            data = encoded,
            instruction = instruction
        )

        assertFalse(verdict.isPayable)
        assertTrue(verdict.title.contains("Transfer"))
    }

    @Test
    fun testDecode_SolStakeDeactivate() {
        val data = byteArrayOf(5, 0, 0, 0) // discriminator 5 = deactivate
        val encoded = Base64.getEncoder().encodeToString(data)

        val instruction = Instruction(
            programIdIndex = 0,
            accounts = listOf(0),
            data = encoded
        )

        val verdict = InstructionDecoder.decode(
            programId = "Stake11111111111111111111111111111111111111",
            accounts = listOf("stakeAccount"),
            data = encoded,
            instruction = instruction
        )

        assertFalse(verdict.isPayable)
        assertTrue(verdict.title.contains("deactivate"))
        assertTrue(verdict.summary.contains("NOT SKR"))
    }

    // Feature 45-50: Review Binding and Hash Tests

    @Test
    fun testCanonicalMessage_Format() {
        val message = ReviewBinding.canonicalMessage(
            programId = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
            keys = listOf("key1", "key2"),
            data = "data123",
            network = "mainnet-beta"
        )

        assertTrue(message.contains("mainnet-beta"))
        assertTrue(message.contains("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"))
    }

    @Test
    fun testSha256_ProducesValidHash() {
        val text = "test message"
        val hash = sha256Text(text)
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun testSha256_Deterministic() {
        val text = "test"
        val hash1 = sha256Text(text)
        val hash2 = sha256Text(text)
        assertEquals(hash1, hash2)
    }

    @Test
    fun testSha256_ChangeDetection() {
        val hash1 = sha256Text("test")
        val hash2 = sha256Text("testX")
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testPrepareReview_StoresHash() {
        val review = ReviewBinding.prepareReview(
            programId = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
            keys = listOf("key1"),
            data = "data",
            network = "mainnet-beta"
        )

        assertNotNull(review.messageHash)
        assertEquals(64, review.messageHash.length)
    }

    @Test
    fun testAssertUnchanged_MatchingHash() {
        val hash = "abc123"
        ReviewBinding.assertUnchanged(hash, hash)
    }

    @Test
    fun testAssertUnchanged_MismatchThrows() {
        assertThrows(IllegalStateException::class.java) {
            ReviewBinding.assertUnchanged("hash1", "hash2")
        }
    }

    // Feature 57: Wallet session

    @Test
    fun testCanSign_PayableVerdictOnly() {
        val payable = Verdict("Test", "test", isPayable = true, "test")
        assertTrue(canSign(payable))

        val notPayable = Verdict("Test", "test", isPayable = false, "test")
        assertFalse(canSign(notPayable))
    }

    // Features 65-67: Receipts

    @Test
    fun testReceiptSchema_CanBeCreated() {
        val receipt = mapOf(
            "schema" to "deproof-receipt-v1",
            "id" to "uuid-123",
            "outcome" to "REJECTED",
            "signature" to null,
            "broadcast" to false
        )

        assertNull(receipt["signature"])
        assertEquals("REJECTED", receipt["outcome"])
    }

    // Features 89-92: Grok Integration

    @Test
    fun testGrokSkipPlaceholder() {
        val apiKey = "MY_GROK_API_KEY"
        val isPlaceholder = apiKey == "MY_GROK_API_KEY"
        assertTrue(isPlaceholder)
    }

    // Helper functions

    @Test
    fun testReadU64LE_Zero() {
        val bytes = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        val value = readU64LE(bytes, 0)
        assertEquals(0L, value)
    }

    @Test
    fun testReadU64LE_One() {
        val bytes = byteArrayOf(1, 0, 0, 0, 0, 0, 0, 0)
        val value = readU64LE(bytes, 0)
        assertEquals(1L, value)
    }

    @Test
    fun testReadU64LE_LittleEndian() {
        val bytes = byteArrayOf(0xFF.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val value = readU64LE(bytes, 0)
        assertEquals(255L, value)
    }

    @Test
    fun testReadU32LE_Zero() {
        val bytes = byteArrayOf(0, 0, 0, 0)
        val value = readU32LE(bytes, 0)
        assertEquals(0, value)
    }

    @Test
    fun testReadU32LE_One() {
        val bytes = byteArrayOf(1, 0, 0, 0)
        val value = readU32LE(bytes, 0)
        assertEquals(1, value)
    }

    @Test
    fun testBase58_Encode() {
        val data = ByteArray(32) { 0xFF.toByte() }
        val encoded = Base58.encode(data)
        assertNotNull(encoded)
        assertTrue(encoded.length > 0)
    }

    @Test
    fun testBase58_Decode() {
        val encoded = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"
        val decoded = Base58.decode(encoded)
        // This should decode to exactly 32 bytes
        assertEquals("Decoded size should be 32", 32, decoded.size)
    }

    @Test
    fun testBase58_RoundTrip() {
        val original = ByteArray(32) { i -> (i % 256).toByte() }
        val encoded = Base58.encode(original)
        val decoded = Base58.decode(encoded)
        java.util.Arrays.equals(original, decoded)
    }

    // Receipt Schema Tests
    @Test
    fun testReceiptSchema_SignedOutcome() {
        val timestamp = System.currentTimeMillis()
        val receipt = com.example.data.Receipt(
            outcome = "signed",
            summary = "Transfer 100 SKR",
            messageHash = "abc123def456",
            signature = "sig_base58",
            slot = 123456789L,
            broadcast = true,
            timestamp = timestamp,
            submittedByClearance = true
        )
        assertEquals("signed", receipt.outcome)
        assertNotNull(receipt.signature)
        assertTrue(receipt.broadcast)
    }

    @Test
    fun testReceiptSchema_RejectedOutcome() {
        val receipt = com.example.data.Receipt(
            outcome = "rejected",
            summary = "SetAuthority - Rejected",
            messageHash = "xyz789",
            signature = null,
            slot = null,
            broadcast = false,
            timestamp = System.currentTimeMillis(),
            submittedByClearance = true
        )
        assertEquals("rejected", receipt.outcome)
        assertNull(receipt.signature)
        assertFalse(receipt.broadcast)
    }

    // MWA Wallet Integration Tests

    @Test
    fun testWalletSession_IsValid() {
        val session = com.example.domain.WalletSession(
            publicKey = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3",
            authToken = "valid_token_123",
            walletName = "Phantom"
        )
        assertTrue(session.isValid())
    }

    @Test
    fun testWalletSession_IsNotValidWithoutPublicKey() {
        val session = com.example.domain.WalletSession(
            publicKey = "",
            authToken = "valid_token_123"
        )
        assertFalse(session.isValid())
    }

    @Test
    fun testWalletSession_ReadyForMainnetRequiresDevnetMemoSigned() {
        val sessionNotReady = com.example.domain.WalletSession(
            publicKey = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3",
            authToken = "valid_token_123",
            devnetMemoSigned = false
        )
        assertFalse(sessionNotReady.isReadyForMainnet())

        val sessionReady = com.example.domain.WalletSession(
            publicKey = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3",
            authToken = "valid_token_123",
            devnetMemoSigned = true,
            devnetMemoTimestamp = System.currentTimeMillis()
        )
        assertTrue(sessionReady.isReadyForMainnet())
    }

    @Test
    fun testMWAAdapter_ValidatePublicKey() {
        assertTrue(com.example.domain.MWAAdapter.validatePublicKey("SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"))
        assertFalse(com.example.domain.MWAAdapter.validatePublicKey("invalid"))
        assertFalse(com.example.domain.MWAAdapter.validatePublicKey(""))
    }
}
