package com.example.domain

import com.fasterxml.jackson.databind.JsonNode
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

/** Independent node identity, never a wallet key. Exact-byte signatures; no mock transport. */
class NodeProtocol(seed: ByteArray) {
    private val key = Ed25519PrivateKeyParameters(seed.copyOf(),0)
    val publicKey: String = Base64.getEncoder().encodeToString(key.generatePublicKey().encoded)
    fun sign(bytes: ByteArray): String = Base64.getEncoder().encodeToString(Ed25519Signer().run { init(true,key); update(bytes,0,bytes.size); generateSignature() })
    fun pair(challenge: JsonNode, code: String, fingerprint: String, scopes: List<String>, now: Instant = Instant.now()): JsonNode {
        ensure(fingerprint.matches(Regex("[0-9a-f]{64}")) && challenge["fingerprint"]?.asText()==fingerprint,"NODE_FINGERPRINT_MISMATCH")
        ensure(Instant.parse(challenge["expiresAt"].asText()).isAfter(now),"PAIRING_EXPIRED")
        ensure(scopes.isNotEmpty() && scopes.distinct().size==scopes.size && scopes.all {it in SCOPES},"BAD_NODE_SCOPES")
        // Match the Go PairPayload field order, signing the reconstructed bounded challenge.
        val c=Json.obj("id" to challenge["id"].asText(),"nonce" to challenge["nonce"].asText(),"fingerprint" to fingerprint,"expiresAt" to challenge["expiresAt"].asText())
        val payload=Json.mapper.writeValueAsBytes(Json.obj("domain" to "deproof-pair-v1","challenge" to c,"publicKey" to publicKey,"scopes" to scopes))
        return Json.obj("challengeId" to c["id"].asText(),"code" to code,"publicKey" to publicKey,"signatureBase64" to sign(payload),"approvedFingerprint" to fingerprint,"scopes" to scopes)
    }
    fun command(session: String, action: String, params: JsonNode, operationId: String = UUID.randomUUID().toString(), now: Instant = Instant.now()): JsonNode {
        ensure(action in ACTIONS,"UNKNOWN_NODE_ACTION")
        ensure(operationId.matches(Regex("[A-Za-z0-9_-]{16,128}")),"BAD_OPERATION_ID")
        val payload=Json.mapper.writeValueAsBytes(Json.obj("sessionId" to session,"operationId" to operationId,"deadline" to now.plusSeconds(45).toString(),"policy" to "node-policy-v1","action" to action,"params" to params))
        return Json.obj("payloadBase64" to Base64.getEncoder().encodeToString(payload),"signatureBase64" to sign(payload))
    }
    companion object {
        val SCOPES=setOf("READ_NODE","MANAGE_SERVICE","SHARE_BANDWIDTH","RUN_PROOF_JOB","EXPORT_PUBLIC_RECORDS")
        val ACTIONS=setOf("observe","revoke","consent","transfer","stop","startService","stopService","discoverProofJobs","proof","cancelProof","export")
        fun newSeed()=ByteArray(32).also { SecureRandom().nextBytes(it) }
        fun verifyRecord(record: JsonNode, fingerprint: String, operationId: String? = null): JsonNode {
            ensure(record["schema"]?.asText()=="deproof-node-signed-v1","NODE_SCHEMA")
            val pub=Base64.getDecoder().decode(record["publicKeyBase64"].asText())
            ensure(pub.size==32 && sha256Hex(pub)==fingerprint,"NODE_FINGERPRINT_MISMATCH")
            val raw=Base64.getDecoder().decode(record["payloadBase64"].asText());ensure(raw.size<=2*1024*1024,"NODE_RESPONSE_TOO_LARGE")
            val signature=Base64.getDecoder().decode(record["signatureBase64"].asText())
            val signer=Ed25519Signer();signer.init(false,Ed25519PublicKeyParameters(pub,0));signer.update(raw,0,raw.size)
            ensure(signer.verifySignature(signature),"NODE_SIGNATURE_INVALID")
            val event=Json.parse(raw.toString(Charsets.UTF_8))
            ensure(event["domain"]?.asText() in setOf("deproof-metering-v1","deproof-contribution-v1"),"NODE_DOMAIN")
            ensure(event["nodeFingerprint"]?.asText()==fingerprint,"NODE_FINGERPRINT_MISMATCH")
            ensure(operationId==null || event["operationId"]?.asText()==operationId,"NODE_OPERATION_MISMATCH")
            return event
        }
    }
}
object RecoveryPolicy {
    val pending=setOf("PREPARED","RUNNING","SUBMITTING")
    fun afterCrash(state: String): String = if(state in pending) "OUTCOME_UNKNOWN" else state
    fun mayRetry(state: String)=state=="PREPARED" // dispatch changes it durably before any network I/O
}
