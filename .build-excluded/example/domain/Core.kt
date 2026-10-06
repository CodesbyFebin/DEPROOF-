package com.example.domain

import java.math.BigInteger
import java.security.MessageDigest
import java.io.InputStream

class Failure(val code: String, detail: String = code) : IllegalArgumentException(detail)
fun ensure(ok: Boolean, code: String) { if (!ok) throw Failure(code) }
object Base58 {
    private const val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    fun decode(value: String, max: Int = 128): ByteArray {
        ensure(value.isNotEmpty() && value.length <= max, "BAD_ADDRESS")
        var n = BigInteger.ZERO
        for (c in value) { val i = alphabet.indexOf(c); ensure(i >= 0, "BAD_ADDRESS"); n = n.multiply(BigInteger.valueOf(58)).add(BigInteger.valueOf(i.toLong())) }
        val bytes = if (n == BigInteger.ZERO) byteArrayOf() else n.toByteArray().let { if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }
        return ByteArray(value.takeWhile { it == '1' }.length) + bytes
    }
    fun encode(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        var n = BigInteger(1, bytes); val out = StringBuilder()
        while (n.signum() > 0) { val qr = n.divideAndRemainder(BigInteger.valueOf(58)); out.append(alphabet[qr[1].toInt()]); n = qr[0] }
        repeat(bytes.takeWhile { it == 0.toByte() }.size) { out.append('1') }
        return out.reverse().toString()
    }
    fun pubkey(s: String): ByteArray = decode(s, 44).also { ensure(it.size == 32, "BAD_ADDRESS") }
}
fun isPubkey(s: String) = runCatching { Base58.pubkey(s) }.isSuccess
fun sha256Hex(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
fun sha256Text(s: String) = sha256Hex(s.toByteArray(Charsets.UTF_8))
fun formatAmount(raw: BigInteger, decimals: Int): String {
    ensure(raw.signum() >= 0 && decimals in 0..18, "BAD_AMOUNT")
    val s = raw.toString().padStart(decimals + 1, '0')
    return if (decimals == 0) s else s.dropLast(decimals) + "." + s.takeLast(decimals)
}
fun formatSol(raw: BigInteger) = formatAmount(raw, 9)
fun formatSkr(raw: BigInteger) = formatAmount(raw, 6)
fun readU64LE(bytes: ByteArray, offset: Int): BigInteger {
    ensure(offset >= 0 && offset <= bytes.size - 8, "SHORT_BUFFER")
    return BigInteger(1, bytes.copyOfRange(offset, offset + 8).reversedArray())
}
fun readU32LE(bytes: ByteArray, offset: Int): Long {
    ensure(offset >= 0 && offset <= bytes.size - 4, "SHORT_BUFFER")
    return (0..3).fold(0L) { a, i -> a or ((bytes[offset+i].toLong() and 255) shl (8*i)) }
}
val U64_MAX = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE)
fun transferCheckedData(amount: BigInteger, decimals: Int): ByteArray {
    ensure(amount in BigInteger.ZERO..U64_MAX && decimals in 0..255, "BAD_AMOUNT")
    return ByteArray(10).also { it[0] = 12; for(i in 0..7) it[i+1] = amount.shiftRight(i*8).toByte(); it[9] = decimals.toByte() }
}
data class FileHash(val sha256: String, val byteLength: String)
fun sha256File(input: InputStream, maxBytes: Long = 64L*1024*1024, cancelled: () -> Boolean = { false }): FileHash {
    val md = MessageDigest.getInstance("SHA-256"); var count = 0L; val buf = ByteArray(65536)
    while(true) {
        ensure(!cancelled(), "CANCELLED")
        val n = input.read(buf); if(n < 0) break; if(n == 0) continue
        ensure(count <= maxBytes - n, "FILE_TOO_LARGE"); count += n; md.update(buf, 0, n)
    }
    return FileHash(md.digest().joinToString("") { "%02x".format(it.toInt() and 255) }, count.toString())
}
fun shortKey(s: String) = if(s.length > 8) s.take(4) + "…" + s.takeLast(4) else s
object Programs {
    const val TOKEN = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
    const val SKR = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"
    const val MEMO = "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"
    const val STAKE = "Stake11111111111111111111111111111111111111"
    const val SKR_STAKE = "SKRskrmtL83pcL4YqLWt6iPefDqwXQWHSw9S9vz94BZ"
}
data class Role(val address: String, val signer: Boolean, val writable: Boolean)
data class Instruction(val program: String, val accounts: List<Role>, val data: ByteArray)
data class TokenAccount(val program: String, val mint: String, val owner: String, val state: String)
data class TokenContext(val source: TokenAccount, val destination: TokenAccount, val recipient: String, val mintDecimals: Int, val deploymentVerified: Boolean)
data class Verdict(val allowed: Boolean, val reason: String, val summary: String) {
    val title get() = if(allowed) "Allowed by current policy" else "Do not sign"
}
enum class Policy { DEVNET_MEMO_V1, SKR_TRANSFER_V1 }
fun decodeInstruction(ix: Instruction, policy: Policy, token: TokenContext? = null): Verdict {
    fun refuse(reason: String, summary: String = "Unsupported instruction") = Verdict(false, reason, summary)
    if(ix.program == Programs.STAKE) {
        val op = runCatching { readU32LE(ix.data,0) }.getOrNull()
        return refuse("SOL_STAKE_UNSUPPORTED", "SOL stake " + (mapOf(0L to "initialize",1L to "authorize",2L to "delegate",3L to "split",4L to "withdraw",5L to "deactivate",7L to "merge")[op] ?: "unknown"))
    }
    if(ix.program == Programs.SKR_STAKE) return refuse("SKR stake layout not verified. Do not sign.")
    if(policy == Policy.DEVNET_MEMO_V1) {
        if(ix.program != Programs.MEMO) return refuse("UNQUALIFIED_PROGRAM")
        val text = runCatching { Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(ix.data)).toString() }.getOrNull()
        if(text == null || !Regex("Deproof devnet memo [0-9]{1,20}").matches(text)) return refuse("MALFORMED_MEMO")
        if(ix.accounts.size != 1 || !ix.accounts[0].signer) return refuse("MEMO_SIGNER_MISMATCH")
        return Verdict(true,"Devnet memo only; no asset transfer",text)
    }
    if(ix.program != Programs.TOKEN) return refuse("UNQUALIFIED_PROGRAM")
    val d = ix.data.firstOrNull()?.toInt()?.and(255) ?: return refuse("MALFORMED_INSTRUCTION")
    if(d != 12) return refuse("UNSUPPORTED_TOKEN_INSTRUCTION_$d", mapOf(3 to "Unchecked Transfer",4 to "Approve",5 to "Revoke",6 to "SetAuthority",7 to "MintTo",8 to "Burn",9 to "CloseAccount",13 to "ApproveChecked",14 to "BurnChecked")[d] ?: "Unknown token instruction $d")
    if(ix.data.size != 10 || ix.accounts.size != 4) return refuse("MALFORMED_INSTRUCTION")
    if(ix.data[9].toInt() != 6) return refuse("SKR_DECIMALS_MISMATCH")
    val a = ix.accounts
    if(a.any { !isPubkey(it.address) } || a.map { it.address }.distinct().size != 4) return refuse("TOKEN_ACCOUNT_MISMATCH")
    if(a[1].address != Programs.SKR) return refuse("WRONG_MINT")
    if(!a[0].writable || a[0].signer || a[1].writable || a[1].signer || !a[2].writable || a[2].signer || !a[3].signer) return refuse("INVALID_ACCOUNT_ROLES")
    if(token == null || !token.deploymentVerified) return refuse("MINT_DEPLOYMENT_UNVERIFIED")
    if(token.mintDecimals != 6) return refuse("SKR_DECIMALS_MISMATCH")
    if(token.source.program != Programs.TOKEN || token.destination.program != Programs.TOKEN || token.source.mint != Programs.SKR || token.destination.mint != Programs.SKR || token.source.owner != a[3].address || token.destination.owner != token.recipient || token.source.state != "initialized" || token.destination.state != "initialized") return refuse("TOKEN_ACCOUNT_MISMATCH")
    return Verdict(true,"TransferChecked policy; this cannot be reversed", "Transfer ${formatSkr(readU64LE(ix.data,1))} SKR to token account ${a[2].address}; recipient ${token.recipient}")
}
fun evaluateAllInstructions(ixs: List<Instruction>, policy: Policy, token: TokenContext? = null): List<Verdict> {
    ensure(ixs.isNotEmpty(), "EMPTY_MESSAGE")
    if(policy == Policy.DEVNET_MEMO_V1 && ixs.size != 1) return ixs.map { Verdict(false,"MEMO_POLICY_REQUIRES_ONE_INSTRUCTION","Do not sign") }
    return ixs.map { decodeInstruction(it, policy, token) }
}
fun canonicalMessage(network: String, payer: String, ix: Instruction): String {
    ensure(network in listOf("devnet","mainnet-beta"), "CLUSTER_MISMATCH")
    ensure(payer == "unknown" || isPubkey(payer), "BAD_ADDRESS")
    Base58.pubkey(ix.program); ix.accounts.forEach { Base58.pubkey(it.address) }
    return "deproof-review-v1\nnetwork=$network\nfeePayer=$payer\nprogramId=${ix.program}\nkeys=${ix.accounts.joinToString(",") { it.address }}\ndata=${ix.data.joinToString("") { "%02x".format(it.toInt() and 255) }}"
}
