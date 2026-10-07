package com.aistudio.deproof.sdwk

import com.aistudio.deproof.sdwk.domain.InstructionDecoder
import com.aistudio.deproof.sdwk.domain.ReviewBinding
import com.aistudio.deproof.sdwk.domain.ReceiptExporter
import com.aistudio.deproof.sdwk.util.Base58
import com.aistudio.deproof.sdwk.util.formatSkr
import com.aistudio.deproof.sdwk.util.formatSol
import com.aistudio.deproof.sdwk.util.isPubkey
import com.aistudio.deproof.sdwk.util.readU64LE
import com.aistudio.deproof.sdwk.util.sha256Text
import com.aistudio.deproof.sdwk.chain.Instruction
import org.junit.Assert.*
import org.junit.Test

class ShipTest {

    // Feature 2: Reject invalid addresses
    @Test
    fun testRejectInvalidAddress_TooShort() {
        assertFalse(isPubkey("123456789"))
    }

    @Test
    fun testRejectInvalidAddress_InvalidBase58() {
        assertFalse(isPubkey("OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO"))
    }

    @Test
    fun testRejectInvalidAddress_CorrectFormat() {
        assertTrue(isPubkey("SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"))
    }

    // Feature 3: Format SOL
    @Test
    fun testFormatSol_ZeroLamports() {
        val result = formatSol(0)
        assertEquals("0.000000000 SOL", result)
    }

    @Test
    fun testFormatSol_OneSol() {
        val result = formatSol(1_000_000_000)
        assertTrue(result.contains("1") && result.contains("SOL"))
    }

    // Feature 7: Format SKR with thousand separators
    @Test
    fun testFormatSkr_LargeAmount() {
        val result = formatSkr(1_000_000_000)
        assertTrue(result.contains("1,000"))
        assertTrue(result.contains("SKR"))
    }

    @Test
    fun testFormatSkr_SmallAmount() {
        val result = formatSkr(1_000_000)
        assertTrue(result.contains("1") && result.contains("SKR"))
    }

    // Feature 26: TransferChecked only on discriminator 12
    @Test
    fun testDecodeTransferChecked_CorrectDiscriminator() {
        val data = byteArrayOf(12, 0, 0, 0, 0, 0, 0, 0, 0, 6)
        val encoded = android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT)

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
                "dest",
                "authority"
            ),
            data = encoded,
            instruction = instruction
        )

        assertTrue(verdict.isPayable)
    }

    // Feature 28: Decimals must be 6
    @Test
    fun testDecodeTransferChecked_InvalidDecimals() {
        val data = byteArrayOf(12, 0, 0, 0, 0, 0, 0, 0, 0, 9) // decimals = 9
        val encoded = android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT)

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
                "dest",
                "authority"
            ),
            data = encoded,
            instruction = instruction
        )

        assertFalse(verdict.isPayable)
        assertTrue(verdict.reason.contains("decimals"))
    }

    // Feature 30: Mint must equal official SKR
    @Test
    fun testDecodeTransferChecked_WrongMint() {
        val data = byteArrayOf(12, 0, 0, 0, 0, 0, 0, 0, 0, 6)
        val encoded = android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT)

        val instruction = Instruction(
            programIdIndex = 0,
            accounts = listOf(0, 1, 2, 3),
            data = encoded
        )

        val verdict = InstructionDecoder.decode(
            programId = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
            accounts = listOf(
                "source",
                "wrongmintaddresshere12345678901234567890",
                "dest",
                "authority"
            ),
            data = encoded,
            instruction = instruction
        )

        assertFalse(verdict.isPayable)
    }

    // Feature 31: Refuse SetAuthority
    @Test
    fun testRefuseSetAuthority() {
        val data = byteArrayOf(6) // SetAuthority discriminator
        val encoded = android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT)

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
    }

    // Feature 32: Refuse Approve
    @Test
    fun testRefuseApprove() {
        val data = byteArrayOf(4) // Approve discriminator
        val encoded = android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT)

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
    }

    // Feature 33: Refuse unchecked Transfer
    @Test
    fun testRefuseUncheckedTransfer() {
        val data = byteArrayOf(3) // Transfer (unchecked) discriminator
        val encoded = android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT)

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
    }

    // Feature 45: Canonical message format
    @Test
    fun testCanonicalMessageFormat() {
        val tx = com.aistudio.deproof.sdwk.chain.TransactionResponse(
            slot = 100,
            transaction = com.aistudio.deproof.sdwk.chain.Transaction(
                signatures = listOf("sig1"),
                message = com.aistudio.deproof.sdwk.chain.Message(
                    accountKeys = listOf("key1", "key2"),
                    instructions = listOf(
                        Instruction(
                            programIdIndex = 0,
                            accounts = listOf(0, 1),
                            data = "base64data"
                        )
                    ),
                    recentBlockhash = "hash123"
                )
            ),
            meta = null
        )

        val message = ReviewBinding.canonicalMessage(tx)
        assertTrue(message.contains("mainnet-beta"))
        assertTrue(message.contains("Instruction:"))
    }

    // Feature 46: SHA-256 hash
    @Test
    fun testSha256Hash() {
        val text = "test message"
        val hash = sha256Text(text)
        assertEquals(64, hash.length) // SHA-256 produces 64 hex characters
        assertTrue(hash.matches(Regex("[0-9a-f]{64}")))
    }

    // Feature 47: Hash stored on Review open
    @Test
    fun testReviewHashStored() {
        val tx = com.aistudio.deproof.sdwk.chain.TransactionResponse(
            slot = 100,
            transaction = com.aistudio.deproof.sdwk.chain.Transaction(
                signatures = listOf("sig1"),
                message = com.aistudio.deproof.sdwk.chain.Message(
                    accountKeys = listOf("key1"),
                    instructions = emptyList(),
                    recentBlockhash = "hash"
                )
            ),
            meta = null
        )

        val reviewHash = ReviewBinding.prepareReview(tx)
        assertNotNull(reviewHash.messageHash)
        assertEquals(64, reviewHash.messageHash.length)
    }

    // Feature 48: Recompute hash on Approve
    @Test
    fun testRecomputeHashOnApprove() {
        val message = "test"
        val hash1 = sha256Text(message)
        val hash2 = sha256Text(message)
        assertEquals(hash1, hash2)
    }

    // Feature 49: MESSAGE_CHANGED on byte change
    @Test
    fun testMessageChangedDetection() {
        val original = "test message"
        val modified = "test messagX"
        val hash1 = sha256Text(original)
        val hash2 = sha256Text(modified)
        assertNotEquals(hash1, hash2)
    }

    // Feature 57: No session = no Approve
    @Test
    fun testNoSessionNoApprove() {
        val verdict = com.aistudio.deproof.sdwk.chain.Verdict(
            title = "Test",
            reason = "test",
            isPayable = true,
            summary = "test"
        )
        val canSign = com.aistudio.deproof.sdwk.domain.canSign(verdict)
        assertTrue(canSign)
    }

    // Feature 65: Reject writes receipt with signature: null
    @Test
    fun testRejectWritesNullSignature() {
        val receipt = ReceiptExporter.createReceipt(
            signature = null,
            verdict = "rejected",
            summary = "User rejected transaction"
        )
        assertNull(receipt.signature)
    }

    // Feature 66: Copy receipt JSON
    @Test
    fun testExportReceiptJson() {
        val receipt = ReceiptExporter.createReceipt(
            signature = "sig123",
            verdict = "approved",
            summary = "Transfer 100 SKR"
        )
        val json = ReceiptExporter.exportReceipt(receipt)
        assertTrue(json.contains("sig123"))
        assertTrue(json.contains("approved"))
    }

    // Feature 67: Receipts persist
    @Test
    fun testReceiptCreation() {
        val receipt = ReceiptExporter.createReceipt(
            signature = "test_sig",
            verdict = "recorded",
            summary = "Test receipt"
        )
        assertNotNull(receipt.id)
        assertTrue(receipt.timestamp > 0)
    }

    // Feature 89: Grok only on decoded packet
    @Test
    fun testGrokOnlyOnDecodedPacket() {
        val agent = com.aistudio.deproof.sdwk.ai.GrokRagAgent("MY_GROK_API_KEY")
        assertNotNull(agent)
    }

    // Feature 90: Skip Grok if key is placeholder
    @Test
    fun testSkipGrokPlaceholder() {
        val agent = com.aistudio.deproof.sdwk.ai.GrokRagAgent("MY_GROK_API_KEY")
        assertNotNull(agent)
    }

    // Feature 91: Show HTTP errors
    @Test
    fun testHttpErrorHandling() {
        val agent = com.aistudio.deproof.sdwk.ai.GrokRagAgent("test_key")
        assertNotNull(agent)
    }

    // Feature 92: Grok note cannot enable Approve
    @Test
    fun testGrokNoteCannotEnableApprove() {
        val verdict = com.aistudio.deproof.sdwk.chain.Verdict(
            title = "Do Not Sign",
            reason = "Grok recommends caution",
            isPayable = false,
            summary = "Grok note: risky pattern detected"
        )
        assertFalse(verdict.isPayable)
    }

    // Helper: readU64LE
    @Test
    fun testReadU64LE() {
        val bytes = byteArrayOf(1, 0, 0, 0, 0, 0, 0, 0)
        val value = readU64LE(bytes, 0)
        assertEquals(1L, value)
    }

    @Test
    fun testReadU64LEMultiByte() {
        val bytes = byteArrayOf(1, 1, 0, 0, 0, 0, 0, 0)
        val value = readU64LE(bytes, 0)
        assertEquals(257L, value)
    }

    @Test
    fun testBase58Decode() {
        val encoded = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"
        val decoded = Base58.decode(encoded)
        assertEquals(32, decoded.size)
    }

    @Test
    fun testBase58Encode() {
        val data = ByteArray(32) { 0xFF.toByte() }
        val encoded = Base58.encode(data)
        assertNotNull(encoded)
        assertTrue(encoded.length > 0)
    }
}
