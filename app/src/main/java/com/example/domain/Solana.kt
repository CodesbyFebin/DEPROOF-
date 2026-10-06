package com.example.domain

import java.io.ByteArrayOutputStream
import java.security.Signature
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

private class Cursor(private val bytes: ByteArray) {
    var position = 0
    fun u8(): Int { ensure(position < bytes.size,"SHORT_BUFFER"); return bytes[position++].toInt() and 255 }
    fun take(n: Int): ByteArray { ensure(n >= 0 && n <= bytes.size-position,"SHORT_BUFFER"); return bytes.copyOfRange(position, position+n).also { position += n } }
    fun short(): Int {
        var value = 0
        for(i in 0..2) { val b = u8(); ensure(i != 2 || b <= 3,"MALFORMED_SHORTVEC"); value = value or ((b and 127) shl (7*i)); if(b and 128 == 0) { ensure(i == 0 || b != 0,"NON_CANONICAL_SHORTVEC"); return value } }
        throw Failure("MALFORMED_SHORTVEC")
    }
    fun end() = ensure(position == bytes.size,"TRAILING_TRANSACTION_BYTES")
}
class ParsedTransaction(message: ByteArray, signatures: List<ByteArray>, val keys: List<Role>, val instructions: List<Instruction>, val version: Int?, val blockhash: String) {
    private val raw = message.copyOf(); private val sigs = signatures.map { it.copyOf() }
    fun messageBytes() = raw.copyOf()
    fun signatures() = sigs.map { it.copyOf() }
}
fun parseTransaction(bytes: ByteArray): ParsedTransaction {
    ensure(bytes.size <= 1232,"TRANSACTION_TOO_LARGE")
    val c = Cursor(bytes); val sigCount = c.short(); ensure(sigCount in 1..16,"SIGNATURE_COUNT")
    val signatures = (0 until sigCount).map { c.take(64) }; val start = c.position
    var required = c.u8(); var version: Int? = null
    if(required and 128 != 0) { version = required and 127; ensure(version == 0,"UNSUPPORTED_TRANSACTION_VERSION"); required = c.u8() }
    val readSigned = c.u8(); val readUnsigned = c.u8(); val keyCount = c.short()
    ensure(required == sigCount && required <= keyCount && readSigned <= required && readUnsigned <= keyCount-required && required-readSigned >= 1,"INVALID_MESSAGE_HEADER")
    val keys = (0 until keyCount).map { i -> Role(Base58.encode(c.take(32)), i < required, if(i < required) i < required-readSigned else i < keyCount-readUnsigned) }
    ensure(keys.map { it.address }.distinct().size == keys.size,"DUPLICATE_ACCOUNT_KEY")
    val blockhash = Base58.encode(c.take(32)); val ixCount = c.short(); ensure(ixCount in 1..64,"EMPTY_MESSAGE")
    val instructions = (0 until ixCount).map {
        val program = c.u8(); val accounts = c.take(c.short()).map { it.toInt() and 255 }; val data = c.take(c.short())
        ensure(program < keys.size && accounts.all { it < keys.size },"UNRESOLVED_LOOKUP_TABLE")
        ensure(!keys[program].signer && !keys[program].writable,"INVALID_PROGRAM_ROLES")
        Instruction(keys[program].address, accounts.map { keys[it] }, data)
    }
    if(version == 0) ensure(c.short() == 0,"UNRESOLVED_LOOKUP_TABLE")
    c.end()
    return ParsedTransaction(bytes.copyOfRange(start,bytes.size), signatures, keys, instructions, version, blockhash)
}
fun shortVec(n: Int): ByteArray { ensure(n in 0..65535,"BAD_LENGTH"); var v = n; val o = ByteArrayOutputStream(); do { val b = v and 127; v = v ushr 7; o.write(b or if(v > 0) 128 else 0) } while(v > 0); return o.toByteArray() }
fun buildMemo(account: String, blockhash: String, unixMillis: Long): ByteArray {
    val text = "Deproof devnet memo $unixMillis".toByteArray(Charsets.UTF_8)
    return byteArrayOf(1) + ByteArray(64) + byteArrayOf(1,0,1,2) + Base58.pubkey(account) + Base58.pubkey(Programs.MEMO) + Base58.pubkey(blockhash) + byteArrayOf(1,1,1,0) + shortVec(text.size) + text
}
data class ReviewContext(val account: String, val cluster: String, val genesis: String, val policy: Policy, val decoderVersion: String = "solana-strict-v1", val evidenceDigest: String? = null, val reviewedAt: String, val lastValidBlockHeight: String?, val feeLamports: String?, val feeSlot: String?, val requester: String = "Deproof local", val historical: Boolean = false)
class Review(txBytes: ByteArray, val context: ReviewContext) {
    private val bytes = txBytes.copyOf()
    val tx: ParsedTransaction get() = parseTransaction(bytes)
    val messageSha256 = sha256Hex(tx.messageBytes())
    val cardHash = sha256Text(canonicalMessage(context.cluster, tx.keys.first().address, tx.instructions.minBy { rankInstruction(it) }))
    val contextHash = sha256Hex(Json.canonical(Json.mapper.valueToTree(context)))
    fun unsignedBytes() = bytes.copyOf()
    fun assertUnchanged(current: ByteArray, ctx: ReviewContext) {
        ensure(sha256Hex(parseTransaction(current).messageBytes()) == messageSha256,"MESSAGE_CHANGED")
        ensure(sha256Hex(Json.canonical(Json.mapper.valueToTree(ctx))) == contextHash,"REVIEW_CONTEXT_CHANGED")
    }
}
data class SigningGate(val account: String?, val cluster: String?, val strict: Boolean, val blockhashValid: Boolean, val feeReviewed: Boolean, val simulationOk: Boolean, val priorDevnetConfirmed: Boolean)
fun canSign(review: Review, current: ByteArray, context: ReviewContext, gate: SigningGate): Boolean = runCatching {
    review.assertUnchanged(current,context)
    ensure(!context.historical,"HISTORICAL_READ_ONLY")
    ensure(gate.account == context.account && review.tx.keys.first().address == context.account,"NO_WALLET_ACCOUNT")
    ensure(gate.cluster == context.cluster,"CLUSTER_MISMATCH")
    ensure(gate.strict,"STRICT_SIGNING_UNSUPPORTED")
    ensure(gate.blockhashValid && gate.feeReviewed && context.feeLamports != null && gate.simulationOk,"LIVE_REVIEW_REQUIRED")
    ensure(context.cluster == "devnet" || gate.priorDevnetConfirmed,"DEVNET_GATE_REQUIRED")
    ensure(context.policy == Policy.DEVNET_MEMO_V1 && context.cluster == "devnet","MAINNET_SIGNING_BLOCKED")
    ensure(evaluateAllInstructions(review.tx.instructions,context.policy).all { it.allowed },"POLICY_REFUSED")
}.isSuccess
fun validateReturnedTransaction(review: Review, signed: ByteArray, verifier: (ByteArray,ByteArray,ByteArray) -> Boolean): String {
    val tx = parseTransaction(signed)
    ensure(tx.messageBytes().contentEquals(review.tx.messageBytes()),"MESSAGE_CHANGED")
    val sigs = tx.signatures()
    for(i in sigs.indices) ensure(verifier(Base58.pubkey(tx.keys[i].address),tx.messageBytes(),sigs[i]),"INVALID_WALLET_SIGNATURE")
    return Base58.encode(sigs.first())
}
