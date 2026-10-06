package com.example.domain

import org.junit.Test
import org.junit.Assert.*
import java.math.BigInteger
import java.io.ByteArrayInputStream
import java.time.Instant
import java.security.KeyPairGenerator
import java.security.Signature

class CoreTest {
    private val account = Base58.encode(ByteArray(32) { 7 })
    private val blockhash = Base58.encode(ByteArray(32) { 9 })
    private fun context() = ReviewContext(account,"devnet","EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG",Policy.DEVNET_MEMO_V1,reviewedAt="2026-10-06T00:00:00Z",lastValidBlockHeight="123",feeLamports="5001",feeSlot="120")
    private fun fails(code: String, block: () -> Unit) { try { block(); fail("Expected $code") } catch(e: Failure) { assertEquals(code,e.code) } }
    @Test fun addressesRejectAlphabetLengthAndOversize() {
        assertTrue(isPubkey("11111111111111111111111111111111")); assertTrue(isPubkey(account))
        for(s in listOf("","0OIl","1","1".repeat(45),"1".repeat(100000))) assertFalse(isPubkey(s))
        for(i in 0..255) { val b = ByteArray(32) { i.toByte() }; assertArrayEquals(b,Base58.decode(Base58.encode(b))) }
    }
    @Test fun exactUnsignedVectorsAndDecimalFormatting() {
        for(n in listOf(BigInteger.ZERO,BigInteger.ONE.shiftLeft(63).subtract(BigInteger.ONE),BigInteger.ONE.shiftLeft(63),U64_MAX)) assertEquals(n,readU64LE(transferCheckedData(n,6),1))
        assertEquals("18446744073.709551615",formatSol(U64_MAX)); assertEquals("18446744073709.551615",formatSkr(U64_MAX))
        assertEquals("0.000000001",formatSol(BigInteger.ONE)); assertEquals("0.000000",formatSkr(BigInteger.ZERO))
        assertArrayEquals(byteArrayOf(12,0,202.toByte(),154.toByte(),59,0,0,0,0,6),transferCheckedData(BigInteger("1000000000"),6))
        fails("BAD_AMOUNT") { transferCheckedData(U64_MAX+BigInteger.ONE,6) }; fails("BAD_AMOUNT") { transferCheckedData(BigInteger.valueOf(-1),6) }; fails("SHORT_BUFFER") { readU64LE(ByteArray(7),0) }
    }
    @Test fun strictPolicyChecksAllRolesMintsOwnersAndSiblings() {
        val source = Base58.encode(ByteArray(32) { 1 }); val dest = Base58.encode(ByteArray(32) { 2 })
        val ix = Instruction(Programs.TOKEN,listOf(Role(source,false,true),Role(Programs.SKR,false,false),Role(dest,false,true),Role(account,true,false)),transferCheckedData(BigInteger.ONE,6))
        val token = TokenContext(TokenAccount(Programs.TOKEN,Programs.SKR,account,"initialized"),TokenAccount(Programs.TOKEN,Programs.SKR,account,"initialized"),account,6,true)
        assertTrue(decodeInstruction(ix,Policy.SKR_TRANSFER_V1,token).allowed)
        for(d in 0..255) if(d != 12) assertFalse(decodeInstruction(ix.copy(data=byteArrayOf(d.toByte())),Policy.SKR_TRANSFER_V1,token).allowed)
        assertFalse(decodeInstruction(ix.copy(data=ix.data+0),Policy.SKR_TRANSFER_V1,token).allowed)
        assertFalse(decodeInstruction(ix.copy(data=transferCheckedData(BigInteger.ONE,9)),Policy.SKR_TRANSFER_V1,token).allowed)
        assertFalse(decodeInstruction(ix.copy(program=Programs.MEMO),Policy.SKR_TRANSFER_V1,token).allowed)
        assertFalse(decodeInstruction(ix.copy(accounts=ix.accounts.reversed()),Policy.SKR_TRANSFER_V1,token).allowed)
        assertFalse(decodeInstruction(ix.copy(accounts=ix.accounts.map { it.copy(signer=false) }),Policy.SKR_TRANSFER_V1,token).allowed)
        assertFalse(decodeInstruction(ix,Policy.SKR_TRANSFER_V1,token.copy(source=token.source.copy(state="frozen"))).allowed)
        assertFalse(decodeInstruction(ix,Policy.SKR_TRANSFER_V1,token.copy(destination=token.destination.copy(owner=source))).allowed)
        assertFalse(decodeInstruction(ix,Policy.SKR_TRANSFER_V1,token.copy(deploymentVerified=false)).allowed)
        assertFalse(evaluateAllInstructions(listOf(ix,ix.copy(data=byteArrayOf(4))),Policy.SKR_TRANSFER_V1,token).all { it.allowed })
        val stake = decodeInstruction(Instruction(Programs.STAKE,emptyList(),byteArrayOf(5,0,0,0)),Policy.SKR_TRANSFER_V1)
        assertEquals("SOL stake deactivate",stake.summary); assertFalse(stake.allowed)
    }
    @Test fun memoParserAndImmutableMessageContextMutations() {
        val bytes = buildMemo(account,blockhash,1000); val review = Review(bytes,context())
        val tx = parseTransaction(bytes); assertTrue(evaluateAllInstructions(tx.instructions,Policy.DEVNET_MEMO_V1).all { it.allowed })
        assertFalse(evaluateAllInstructions(tx.instructions,Policy.SKR_TRANSFER_V1).all { it.allowed })
        assertEquals(64,review.messageSha256.length); assertNotEquals(review.cardHash,review.messageSha256)
        val copy = bytes.copyOf(); copy[copy.lastIndex] = '1'.code.toByte()
        fails("MESSAGE_CHANGED") { review.assertUnchanged(copy,context()) }; review.assertUnchanged(bytes,context())
        fails("REVIEW_CONTEXT_CHANGED") { review.assertUnchanged(bytes,context().copy(evidenceDigest="a".repeat(64))) }
        fails("REVIEW_CONTEXT_CHANGED") { review.assertUnchanged(bytes,context().copy(cluster="mainnet-beta")) }
        bytes[bytes.lastIndex] = '2'.code.toByte(); assertEquals(review.messageSha256,sha256Hex(parseTransaction(review.unsignedBytes()).messageBytes()))
        fails("TRAILING_TRANSACTION_BYTES") { parseTransaction(review.unsignedBytes()+0) }
        val versioned = review.unsignedBytes().let { it.copyOfRange(0,65) + byteArrayOf(0x80.toByte()) + it.copyOfRange(65,it.size) + byteArrayOf(0) }
        assertEquals(0,parseTransaction(versioned).version)
        val lookup = versioned.copyOf(); lookup[lookup.lastIndex]=1
        fails("UNRESOLVED_LOOKUP_TABLE") { parseTransaction(lookup) }
    }
    @Test fun approvalPredicateCannotBeOverriddenByDisplayOrAi() {
        val b = buildMemo(account,blockhash,1000); val r = Review(b,context()); val g = SigningGate(account,"devnet",true,true,true,true,false)
        assertTrue(canSign(r,b,context(),g))
        for(bad in listOf(g.copy(account=null),g.copy(cluster="mainnet-beta"),g.copy(strict=false),g.copy(blockhashValid=false),g.copy(feeReviewed=false),g.copy(simulationOk=false))) assertFalse(canSign(r,b,context(),bad))
        val history = Review(b,context().copy(historical=true)); assertFalse(canSign(history,b,history.context,g))
    }
    @Test fun returnedSignatureAndMessageTamperAreRejected() {
        val b = buildMemo(account,blockhash,1000); val r=Review(b,context())
        fails("INVALID_WALLET_SIGNATURE") { validateReturnedTransaction(r,b) { _,_,_ -> false } }
        val changed = b.copyOf(); changed[changed.lastIndex]='2'.code.toByte()
        fails("MESSAGE_CHANGED") { validateReturnedTransaction(r,changed) { _,_,_ -> true } }
    }
    @Test fun rawStreamingLimitCancellationAndCanonicalUnicode() {
        val raw="abc".toByteArray(); assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",sha256File(ByteArrayInputStream(raw)).sha256)
        assertNotEquals(sha256Hex(raw),sha256Hex(java.util.Base64.getEncoder().encode(raw)))
        fails("FILE_TOO_LARGE") { sha256File(ByteArrayInputStream(raw),2) }; fails("CANCELLED") { sha256File(ByteArrayInputStream(raw),cancelled={true}) }
        val f=EvidenceFile("b",sha256Hex(raw),"text/plain","3","import"); val a=f.copy(id="a")
        assertArrayEquals(canonicalEvidence(listOf(f,a),"नमस्ते\n😀",null,null,"2026-10-06T00:00:00Z"),canonicalEvidence(listOf(a,f),"नमस्ते\n😀",null,null,"2026-10-06T00:00:00Z"))
        fails("DUPLICATE_EVIDENCE") { canonicalEvidence(listOf(f,f),"",null,null,"2026-10-06T00:00:00Z") }
        assertThrows(Exception::class.java) { Json.parse("{\"a\":null,\"a\":true}") }
        assertThrows(Exception::class.java) { Json.parse("{} {}") }
        fails("JCS_PROFILE_NUMBERS_FORBIDDEN") { Json.canonical(Json.parse("{\"a\":9007199254740993}")) }
        assertEquals("{\"a\":null,\"z\":\"é\\n\"}",Json.canonical(Json.parse("{\"z\":\"é\\n\",\"a\":null}")).toString(Charsets.UTF_8))
    }
    @Test fun evidenceEnvelopeSignatureAndReceiptProvenance() {
        val m=canonicalEvidence(emptyList(),"note\n",null,null,"2026-10-06T00:00:00Z"); val env=evidenceEnvelope(m)
        val k=KeyPairGenerator.getInstance("EC").apply { initialize(java.security.spec.ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val sig=Signature.getInstance("SHA256withECDSA").run { initSign(k.private); update(env); sign() }
        assertTrue(verifyLocalSignature(env,sig,k.public.encoded)); assertFalse(verifyLocalSignature(env+0,sig,k.public.encoded)); assertFalse(verifyLocalSignature(m,sig,k.public.encoded))
        val rejected=receipt("REJECTED",account,"devnet","Refused"); validateReceipt(Json.parse(exportReceipt(rejected)))
        assertTrue(rejected["signature"].isNull); assertFalse(rejected["submission"]["broadcast"].asBoolean())
        val forged=rejected.deepCopy<com.fasterxml.jackson.databind.node.ObjectNode>(); forged.put("signature",Base58.encode(ByteArray(64){1})); fails("REJECTED_PROVENANCE") { validateReceipt(forged) }
        val observed=receipt("OBSERVED",account,"devnet","Observed",signature=Base58.encode(ByteArray(64){1})); assertFalse(observed["submission"]["submittedByDeproof"].asBoolean())
    }
    @Test fun crossLanguageGoldenVectors() {
        val vectors=Json.parse(java.io.File(if(java.io.File("fixtures").exists()) "fixtures/jcs-vectors.json" else "../fixtures/jcs-vectors.json").readText())
        for(v in vectors) { val canonical=Json.canonical(v["input"]); assertEquals(v["canonical"].asText(),canonical.toString(Charsets.UTF_8)); assertEquals(v["sha256"].asText(),sha256Hex(canonical)) }
        val transfer=Json.parse(java.io.File(if(java.io.File("fixtures").exists()) "fixtures/transfer-checked-vectors.json" else "../fixtures/transfer-checked-vectors.json").readText())
        for(v in transfer) assertEquals(v["hex"].asText(),transferCheckedData(BigInteger(v["amountRaw"].asText()),6).joinToString("") { "%02x".format(it.toInt() and 255) })
    }
    @Test fun rpcFixturesAreIndependentAndPreserveExactIntegers() {
        val rpc=com.example.data.Rpc("https://api.mainnet-beta.solana.com","mainnet-beta") { method, _ ->
            when(method) {
                "getGenesisHash" -> Json.mapper.valueToTree("5eykt4UsFv8P8NJdTREpY1vzqKqZKvdpKuc147dw2N9d")
                "getBalance" -> Json.parse("{\"context\":{\"slot\":123},\"value\":18446744073709551615}")
                else -> throw Failure("RPC_ERROR")
            }
        }
        assertEquals("18446744073.709551615",rpc.balance(account).value)
        assertEquals("123",rpc.balance(account).slot)
        assertEquals(account,rpc.balance(account).address)
        fails("RPC_ERROR") { rpc.skrBalance(account) }
        assertEquals("18446744073.709551615",rpc.balance(account).value)
        val wrong=com.example.data.Rpc("https://api.devnet.solana.com","devnet") { _,_ -> Json.mapper.valueToTree("wrong-genesis") }
        fails("CLUSTER_MISMATCH") { wrong.balance(account) }
        fails("BAD_RPC_URL") { com.example.data.Rpc("http://example.com","devnet") }
        fails("BAD_RPC_URL") { com.example.data.Rpc("https://secret@example.com","devnet") }
    }
    @Test fun completeMessageMutationsNeverRetainApproval() {
        val original=buildMemo(account,blockhash,1000);val r=Review(original,context())
        for(variant in listOf(buildMemo(Base58.encode(ByteArray(32){8}),blockhash,1000),buildMemo(account,Base58.encode(ByteArray(32){10}),1000),buildMemo(account,blockhash,1001))) fails("MESSAGE_CHANGED") { r.assertUnchanged(variant,context()) }
        val exposed=r.tx.instructions.first().data;exposed[0]=0
        assertTrue(evaluateAllInstructions(r.tx.instructions,Policy.DEVNET_MEMO_V1).all {it.allowed})
        assertFalse(r.messageSha256==sha256Hex(original))
        val ix=r.tx.instructions.first();val expected="deproof-review-v1\nnetwork=devnet\nfeePayer=$account\nprogramId=${Programs.MEMO}\nkeys=$account\ndata=${ix.data.joinToString(""){"%02x".format(it.toInt() and 255)}}"
        assertEquals(expected,canonicalMessage("devnet",account,ix));assertFalse(expected.endsWith("\n"))
    }
    @Test fun timerExpiryNeverAuthorizesWithdrawalAndRankingNeverAuthorizesSibling() {
        val t=IllustrativeTimer("2026-10-04T00:00:00Z","172800")
        assertEquals(java.time.Duration.ZERO,cooldownRemaining(t,Instant.parse("2026-10-06T00:00:00Z")))
        fails("FUTURE_TIMER_START") { cooldownRemaining(t,Instant.parse("2026-10-03T00:00:00Z")) }
        fails("BAD_TIMER_DURATION") { cooldownRemaining(t.copy(durationSeconds="-1"),Instant.parse("2026-10-06T00:00:00Z")) }
        assertEquals("devnet",selectCluster("devnetMemo"));fails("UNQUALIFIED_ACTION_CLUSTER") {selectCluster("skrStake")}
        val valid=Instruction(Programs.TOKEN,emptyList(),byteArrayOf(12));val prohibited=valid.copy(data=byteArrayOf(4))
        assertTrue(rankInstruction(valid)<rankInstruction(prohibited));assertFalse(evaluateAllInstructions(listOf(prohibited,valid).sortedBy(::rankInstruction),Policy.SKR_TRANSFER_V1).all {it.allowed})
    }
}
