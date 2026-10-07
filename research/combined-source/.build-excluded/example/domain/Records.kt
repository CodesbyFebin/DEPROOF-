package com.example.domain

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.UUID
import java.time.Instant

object Json {
    val mapper = ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
    fun parse(raw: String): JsonNode { ensure(raw.toByteArray(Charsets.UTF_8).size <= 2*1024*1024,"JSON_TOO_LARGE"); return mapper.readTree(raw) ?: throw Failure("EMPTY_JSON") }
    // JCS restricted I-JSON profile: numbers are prohibited; exact counters are decimal strings.
    fun canonical(node: JsonNode): ByteArray {
        fun string(s: String): String {
            var i = 0
            while(i < s.length) { val c = s[i]; if(c.isHighSurrogate()) { ensure(i+1 < s.length && s[i+1].isLowSurrogate(),"INVALID_UNICODE"); i += 2 } else { ensure(!c.isLowSurrogate(),"INVALID_UNICODE"); i++ } }
            return mapper.writeValueAsString(s)
        }
        fun render(n: JsonNode): String = when {
            n.isObject -> n.fieldNames().asSequence().toList().sorted().joinToString(",","{","}") { string(it)+":"+render(n[it]) }
            n.isArray -> n.joinToString(",","[","]") { render(it) }
            n.isTextual -> string(n.textValue())
            n.isNull -> "null"
            n.isBoolean -> n.booleanValue().toString()
            else -> throw Failure("JCS_PROFILE_NUMBERS_FORBIDDEN")
        }
        return render(node).toByteArray(Charsets.UTF_8)
    }
    fun obj(vararg fields: Pair<String,Any?>): JsonNode = mapper.valueToTree(fields.toMap())
}
data class EvidenceFile(val id: String, val sha256: String, val mime: String, val byteLength: String, val provenance: String)
fun canonicalEvidence(files: List<EvidenceFile>, note: String, taskId: String?, messageSha256: String?, createdAt: String, location: LocationObservation? = null): ByteArray {
    ensure(files.map { it.id }.distinct().size == files.size,"DUPLICATE_EVIDENCE")
    files.forEach { ensure(Regex("[0-9a-f]{64}").matches(it.sha256) && Regex("0|[1-9][0-9]*").matches(it.byteLength) && Regex("[A-Za-z0-9_-]{1,80}").matches(it.id) && it.provenance in listOf("import","capture"),"BAD_EVIDENCE") }
    ensure(messageSha256 == null || Regex("[0-9a-f]{64}").matches(messageSha256),"BAD_DIGEST")
    Instant.parse(createdAt)
    val manifest=Json.obj("schema" to "deproof-evidence-v2", "taskId" to taskId, "messageSha256" to messageSha256, "files" to files.sortedBy { it.id }, "note" to note, "createdAt" to createdAt) as com.fasterxml.jackson.databind.node.ObjectNode
    if(location!=null) {location.validate();manifest.set<JsonNode>("location",Json.mapper.valueToTree(location))}
    return Json.canonical(manifest)
}
fun evidenceEnvelope(manifest: ByteArray) = Json.canonical(Json.obj("domain" to "deproof-evidence-sig-v2","manifestDigest" to sha256Hex(manifest),"manifestSchema" to "deproof-evidence-v2","algorithm" to "SHA256withECDSA"))
fun verifyLocalSignature(payload: ByteArray, signatureDer: ByteArray, spki: ByteArray): Boolean = runCatching {
    val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(spki))
    val expected = java.security.AlgorithmParameters.getInstance("EC").apply { init(java.security.spec.ECGenParameterSpec("secp256r1")) }.getParameterSpec(java.security.spec.ECParameterSpec::class.java)
    val actual = (key as java.security.interfaces.ECPublicKey).params
    ensure(actual.curve == expected.curve && actual.generator == expected.generator && actual.order == expected.order && actual.cofactor == expected.cofactor,"WRONG_KEY_CURVE")
    Signature.getInstance("SHA256withECDSA").run { initVerify(key); update(payload); verify(signatureDer) }
}.getOrDefault(false)
fun receipt(outcome: String, account: String?, network: String?, summary: String, review: Review? = null, signature: String? = null, taskId: String? = null, manifest: ByteArray? = null, localSignature: JsonNode? = null): JsonNode {
    val r = Json.obj("schema" to "deproof-receipt-v2", "id" to UUID.randomUUID().toString(), "taskId" to taskId,"outcome" to outcome,"account" to account,"network" to network,"summary" to summary,"cardHash" to review?.cardHash,"messageSha256" to review?.messageSha256,"currentMessageSha256" to null,"reviewContextHash" to review?.contextHash,"evidenceDigest" to manifest?.let { sha256Hex(it) },"manifest" to manifest?.let { Json.parse(it.toString(Charsets.UTF_8)) },"signature" to signature,"localSignature" to localSignature,"submission" to Json.obj("state" to "NOT_SUBMITTED","submittedByDeproof" to false,"broadcast" to false,"attemptedAt" to null,"rpcAcceptedAt" to null),"chainObservation" to Json.obj("availability" to "NOT_QUERIED","lastKnownStatus" to "UNKNOWN","slot" to null,"blockTime" to null,"observedAt" to null,"error" to null),"policyVersion" to review?.context?.policy?.name,"decoderVersion" to "solana-strict-v1","createdAt" to Instant.now().toString(),"limitations" to listOf("Local integrity does not prove physical truth; RPC status is a provider observation."))
    validateReceipt(r); return r
}
fun validateReceipt(r: JsonNode) {
    ensure(r["schema"]?.asText() == "deproof-receipt-v2", "BAD_RECEIPT_SCHEMA")
    UUID.fromString(r["id"].asText()); Instant.parse(r["createdAt"].asText())
    val outcome = r["outcome"].asText(); ensure(outcome in listOf("REJECTED","OBSERVED","LOCAL_EVIDENCE_SIGNED","WALLET_SIGNED","SUBMITTED"),"BAD_RECEIPT_OUTCOME")
    for(f in listOf("cardHash","messageSha256","currentMessageSha256","reviewContextHash","evidenceDigest")) ensure(r.has(f) && (r[f].isNull || Regex("[0-9a-f]{64}").matches(r[f].asText())),"BAD_DIGEST")
    val s = r["submission"] ?: throw Failure("BAD_SUBMISSION")
    ensure(s["state"].asText() in listOf("NOT_SUBMITTED","SUBMITTING","SUBMITTED","SUBMISSION_UNKNOWN"),"BAD_SUBMISSION")
    ensure(r["chainObservation"]["availability"].asText() in listOf("NOT_QUERIED","AVAILABLE","UNAVAILABLE"),"BAD_OBSERVATION")
    ensure(r["chainObservation"]["lastKnownStatus"].asText() in listOf("UNKNOWN","PROCESSED","CONFIRMED","FINALIZED","FAILED"),"BAD_CHAIN_STATUS")
    if(outcome == "REJECTED") ensure(r["signature"].isNull && s["state"].asText() == "NOT_SUBMITTED" && s["broadcast"].isBoolean && !s["broadcast"].booleanValue() && s["submittedByDeproof"].isBoolean && !s["submittedByDeproof"].booleanValue(),"REJECTED_PROVENANCE")
    if(outcome == "OBSERVED") ensure(s["state"].asText() == "NOT_SUBMITTED" && s["submittedByDeproof"].isBoolean && !s["submittedByDeproof"].booleanValue() && s["broadcast"].isBoolean && !s["broadcast"].booleanValue(),"OBSERVATION_PROVENANCE")
    if(!r["signature"].isNull) ensure(Base58.decode(r["signature"].asText()).size == 64,"BAD_SIGNATURE")
    if(outcome == "SUBMITTED") ensure(!r["signature"].isNull && s["state"].asText() == "SUBMITTED" && s["broadcast"].booleanValue() && s["submittedByDeproof"].booleanValue() && !s["rpcAcceptedAt"].isNull,"MISSING_RPC_ACCEPTANCE")
    if(outcome == "LOCAL_EVIDENCE_SIGNED") ensure(!r["localSignature"].isNull && !r["evidenceDigest"].isNull,"MISSING_LOCAL_SIGNATURE")
    if(!r["manifest"].isNull) ensure(sha256Hex(Json.canonical(r["manifest"])) == r["evidenceDigest"].asText(),"MANIFEST_DIGEST_MISMATCH")
    if(!r["manifest"].isNull && r["manifest"].has("location"))parseLocationObservation(r["manifest"]["location"])
}
fun exportReceipt(r: JsonNode): String { validateReceipt(r); return Json.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(r) }
