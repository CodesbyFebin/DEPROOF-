package com.example.domain

import org.junit.Test
import org.junit.Assert.*
import java.security.KeyPairGenerator
import java.security.Signature

/**
 * Receipt outcome tests: each outcome is generated and validated per the
 * deproof-receipt-v2 schema.
 *
 * F075: REJECTED — refusal receipt with no chain signature
 * F076: OBSERVED — historical observation receipt (signature present, submittedByDeproof=false)
 * F077: LOCAL_EVIDENCE_SIGNED — local evidence-signature receipt
 * F078: SUBMITTED — only when submittedByDeproof=true, broadcast=true, rpcAcceptedAt not null
 */
class ReceiptOutcomeTest {

    private val account = Base58.encode(ByteArray(32) { 7 })
    private val blockhash = Base58.encode(ByteArray(32) { 9 })
    private val genesis = "EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG"
    private val sig64 = Base58.encode(ByteArray(64) { 1 })

    private fun fails(code: String, block: () -> Unit) {
        try { block(); fail("Expected Failure($code)") }
        catch (e: Failure) { assertEquals(code, e.code) }
    }

    private fun context() = ReviewContext(
        account, "devnet", genesis, Policy.DEVNET_MEMO_V1,
        reviewedAt = "2026-10-06T00:00:00Z", lastValidBlockHeight = "123",
        feeLamports = "5001", feeSlot = "120"
    )

    // --- F075: REJECTED receipt ---

    @Test fun rejectedReceiptHasNullSignatureAndIsNotSubmitted() {
        // F075: refusal receipt carries no chain signature and broadcast=false
        val r = receipt("REJECTED", account, "devnet", "User refused")
        validateReceipt(r)
        assertTrue("signature must be null on REJECTED", r["signature"].isNull)
        assertFalse("broadcast must be false", r["submission"]["broadcast"].asBoolean())
        assertFalse("submittedByDeproof must be false", r["submission"]["submittedByDeproof"].asBoolean())
        assertEquals("REJECTED", r["outcome"].asText())
        assertEquals("NOT_SUBMITTED", r["submission"]["state"].asText())
    }

    @Test fun rejectedReceiptWithFakeSignatureFailsValidation() {
        // F075: a REJECTED receipt with a non-null signature is provenance fraud
        val r = receipt("REJECTED", account, "devnet", "User refused")
        val forged = r.deepCopy<com.fasterxml.jackson.databind.node.ObjectNode>()
        forged.put("signature", sig64)
        fails("REJECTED_PROVENANCE") { validateReceipt(forged) }
    }

    @Test fun rejectedReceiptCanBeExportedAndRoundTripped() {
        // F075: exportReceipt serializes a valid REJECTED receipt without error
        val r = receipt("REJECTED", account, "devnet", "User refused")
        val exported = exportReceipt(r)
        assertTrue("export must be non-empty JSON", exported.startsWith("{"))
        val reimported = Json.parse(exported)
        validateReceipt(reimported)
    }

    // --- F076: OBSERVED receipt ---

    @Test fun observedReceiptCarriesSignatureButIsNotSubmittedByDeproof() {
        // F076: historical observation has a signature but submittedByDeproof=false
        val r = receipt("OBSERVED", account, "devnet", "Observed on-chain", signature = sig64)
        validateReceipt(r)
        assertFalse("signature must not be null on OBSERVED", r["signature"].isNull)
        assertEquals(sig64, r["signature"].asText())
        assertFalse("submittedByDeproof must be false", r["submission"]["submittedByDeproof"].asBoolean())
        assertFalse("broadcast must be false", r["submission"]["broadcast"].asBoolean())
        assertEquals("OBSERVED", r["outcome"].asText())
    }

    @Test fun observedReceiptWithSubmittedByDeproofFailsValidation() {
        // F076: submittedByDeproof=true on an OBSERVED receipt is a provenance error
        val r = receipt("OBSERVED", account, "devnet", "Observed", signature = sig64)
        val sub = r["submission"] as com.fasterxml.jackson.databind.node.ObjectNode
        sub.put("submittedByDeproof", true)
        fails("OBSERVATION_PROVENANCE") { validateReceipt(r) }
    }

    // --- F077: LOCAL_EVIDENCE_SIGNED receipt ---

    @Test fun localEvidenceSignedReceiptRequiresLocalSignatureAndEvidenceDigest() {
        // F077: LOCAL_EVIDENCE_SIGNED must carry both localSignature and evidenceDigest
        val kp = KeyPairGenerator.getInstance("EC").apply {
            initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()
        val evidence = canonicalEvidence(emptyList(), "work note\n", null, null, "2026-10-06T00:00:00Z")
        val envelope = evidenceEnvelope(evidence)
        val sig = Signature.getInstance("SHA256withECDSA").run {
            initSign(kp.private); update(envelope); sign()
        }
        val localSig = Json.obj(
            "algorithm" to "EC-SHA256withECDSA",
            "publicKey" to Base58.encode(kp.public.encoded),
            "signature" to Base58.encode(sig)
        )
        val r = receipt(
            "LOCAL_EVIDENCE_SIGNED", account, "devnet", "Evidence signed",
            manifest = evidence, localSignature = localSig
        )
        validateReceipt(r)
        assertFalse("localSignature must not be null", r["localSignature"].isNull)
        assertFalse("evidenceDigest must not be null", r["evidenceDigest"].isNull)
        assertEquals("LOCAL_EVIDENCE_SIGNED", r["outcome"].asText())
    }

    @Test fun localEvidenceSignedWithRemovedLocalSignatureFailsValidation() {
        // F077: LOCAL_EVIDENCE_SIGNED with localSignature stripped out is rejected
        val kp = KeyPairGenerator.getInstance("EC").apply {
            initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()
        val evidence = canonicalEvidence(emptyList(), "note\n", null, null, "2026-10-06T00:00:00Z")
        val envelope = evidenceEnvelope(evidence)
        val sig = Signature.getInstance("SHA256withECDSA").run {
            initSign(kp.private); update(envelope); sign()
        }
        val localSig = Json.obj(
            "algorithm" to "EC-SHA256withECDSA",
            "publicKey" to Base58.encode(kp.public.encoded),
            "signature" to Base58.encode(sig)
        )
        val r = receipt(
            "LOCAL_EVIDENCE_SIGNED", account, "devnet", "Evidence signed",
            manifest = evidence, localSignature = localSig
        )
        // Receipt is valid now; strip localSignature to simulate corruption
        val corrupted = r.deepCopy<com.fasterxml.jackson.databind.node.ObjectNode>()
        corrupted.putNull("localSignature")
        fails("MISSING_LOCAL_SIGNATURE") { validateReceipt(corrupted) }
    }

    // --- F078: SUBMITTED receipt ---

    private fun validSubmittedReceipt(): com.fasterxml.jackson.databind.node.ObjectNode {
        // Build the smallest valid SUBMITTED receipt: create as OBSERVED then rewrite fields
        val r = receipt("OBSERVED", account, "devnet", "Submitted to devnet", signature = sig64)
        val node = r.deepCopy<com.fasterxml.jackson.databind.node.ObjectNode>()
        node.put("outcome", "SUBMITTED")
        val sub = node["submission"] as com.fasterxml.jackson.databind.node.ObjectNode
        sub.put("state", "SUBMITTED")
        sub.put("broadcast", true)
        sub.put("submittedByDeproof", true)
        sub.put("rpcAcceptedAt", "2026-10-06T00:00:01Z")
        return node
    }

    @Test fun submittedReceiptRequiresBroadcastAndRpcAcceptance() {
        // F078: SUBMITTED only valid when submittedByDeproof=true, broadcast=true, rpcAcceptedAt present
        val r = validSubmittedReceipt()
        validateReceipt(r)
        assertTrue("broadcast must be true", r["submission"]["broadcast"].asBoolean())
        assertTrue("submittedByDeproof must be true", r["submission"]["submittedByDeproof"].asBoolean())
        assertFalse("rpcAcceptedAt must not be null", r["submission"]["rpcAcceptedAt"].isNull)
        assertEquals("SUBMITTED", r["outcome"].asText())
    }

    @Test fun submittedReceiptWithoutRpcAcceptanceFailsValidation() {
        // F078: SUBMITTED without rpcAcceptedAt is rejected
        val r = validSubmittedReceipt()
        (r["submission"] as com.fasterxml.jackson.databind.node.ObjectNode).putNull("rpcAcceptedAt")
        fails("MISSING_RPC_ACCEPTANCE") { validateReceipt(r) }
    }

    @Test fun receiptSchemaVersionIsV2() {
        // All outcomes use deproof-receipt-v2 schema
        for (outcome in listOf("REJECTED", "OBSERVED")) {
            val r = if (outcome == "OBSERVED")
                receipt(outcome, account, "devnet", "test", signature = sig64)
            else
                receipt(outcome, account, "devnet", "test")
            assertEquals("deproof-receipt-v2", r["schema"].asText())
        }
    }
}
