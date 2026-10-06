package com.example.domain

import org.junit.Test
import org.junit.Assert.*
import java.time.Instant
import java.util.Base64

class NodeProtocolTest {
    @Test fun pairingRejectsWrongHostExpiredChallengeAndUnknownScope() {
        val p=NodeProtocol(ByteArray(32){it.toByte()});val now=Instant.parse("2026-10-06T00:00:00Z");val fp="a".repeat(64)
        val c=Json.obj("id" to "id","nonce" to "nonce","fingerprint" to fp,"expiresAt" to now.plusSeconds(60).toString())
        val paired=p.pair(c,"one-use",fp,listOf("READ_NODE"),now)
        assertEquals(p.publicKey,paired["publicKey"].asText())
        for(f in listOf<()->Unit>({p.pair(c,"code","b".repeat(64),listOf("READ_NODE"),now)},{p.pair(c,"code",fp,listOf("READ_NODE"),now.plusSeconds(120))},{p.pair(c,"code",fp,listOf("ROOT"),now)})) assertTrue(runCatching(f).isFailure)
    }
    @Test fun exactCommandSignatureAndContextAreBound() {
        val p=NodeProtocol(ByteArray(32){(it+1).toByte()});val c=p.command("session","transfer",Json.obj("bytes" to "1"),"operation-12345678",Instant.parse("2026-10-06T00:00:00Z"))
        val raw=Base64.getDecoder().decode(c["payloadBase64"].asText());val record=Json.obj("schema" to "deproof-node-signed-v1","publicKeyBase64" to p.publicKey,"signatureBase64" to c["signatureBase64"].asText(),"payloadBase64" to c["payloadBase64"].asText())
        assertEquals("transfer",Json.parse(raw.toString(Charsets.UTF_8))["action"].asText())
        // A valid command cannot be relabeled as a signed receipt (domain separation).
        assertTrue(runCatching {NodeProtocol.verifyRecord(record,sha256Hex(Base64.getDecoder().decode(p.publicKey)))}.isFailure)
        val payload=Json.mapper.writeValueAsBytes(Json.obj("domain" to "deproof-metering-v1","nodeFingerprint" to sha256Hex(Base64.getDecoder().decode(p.publicKey))))
        val signed=Json.obj("schema" to "deproof-node-signed-v1","publicKeyBase64" to p.publicKey,"signatureBase64" to p.sign(payload),"payloadBase64" to Base64.getEncoder().encodeToString(payload))
        assertEquals("deproof-metering-v1",NodeProtocol.verifyRecord(signed,sha256Hex(Base64.getDecoder().decode(p.publicKey)))["domain"].asText())
        val changed=Json.obj("schema" to "deproof-node-signed-v1","publicKeyBase64" to p.publicKey,"signatureBase64" to p.sign(payload),"payloadBase64" to Base64.getEncoder().encodeToString(payload+byteArrayOf(32)))
        assertTrue(runCatching {NodeProtocol.verifyRecord(changed,sha256Hex(Base64.getDecoder().decode(p.publicKey)))}.isFailure)
    }
    @Test fun rpcChecksFullGenesisAndRejectsTruncatedOrWrongCluster() {
        val full="EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG"
        assertEquals(full,com.example.data.Rpc("https://api.devnet.solana.com","devnet") {_,_->Json.mapper.valueToTree(full)}.genesis())
        for(value in listOf(full.take(32),"5eykt4UsFv8P8NJdTREpY1vzqKqZKvdpKuc147dw2N9d")) assertTrue(runCatching {com.example.data.Rpc("https://api.devnet.solana.com","devnet") {_,_->Json.mapper.valueToTree(value)}.genesis()}.isFailure)
    }
    @Test fun crashDoesNotRetryPossibleSideEffects() {
        for(s in RecoveryPolicy.pending) assertEquals("OUTCOME_UNKNOWN",RecoveryPolicy.afterCrash(s))
        assertFalse(RecoveryPolicy.mayRetry("OUTCOME_UNKNOWN"));assertEquals("COMPLETED",RecoveryPolicy.afterCrash("COMPLETED"))
    }
    @Test fun privatePreviewChecksBytesPathsEncodingAndTypes() {
        val dir=java.nio.file.Files.createTempDirectory("deproof-preview").toFile()
        try {
            val f=java.io.File(dir,"fixture");f.writeBytes("exact note".toByteArray())
            val h=f.inputStream().use {sha256File(it)}
            assertEquals(f,verifiedEvidenceFile(dir,"fixture",h.sha256,h.byteLength))
            assertEquals("exact note",previewText(f.readBytes()))
            assertEquals(PreviewKind.TEXT,previewKind("text/plain"));assertEquals(PreviewKind.IMAGE,previewKind("image/png"))
            assertEquals(PreviewKind.UNSUPPORTED,previewKind("text/html"))
            assertTrue(runCatching {verifiedEvidenceFile(dir,"../fixture",h.sha256,h.byteLength)}.isFailure)
            f.appendText("changed");assertTrue(runCatching {verifiedEvidenceFile(dir,"fixture",h.sha256,h.byteLength)}.isFailure)
            assertTrue(runCatching {previewText(byteArrayOf(0xc3.toByte()))}.isFailure)
            assertTrue(runCatching {previewText(ByteArray(65537))}.isFailure)
        } finally {dir.deleteRecursively()}
    }
    @Test fun legacyDigestAndFirstSeenIndicatorsNeverGrantTrust() {
        val raw=ByteArray(32){it.toByte()};val copied=legacyDigestBytes(raw);raw[0]=99
        assertEquals(0,copied[0].toInt());assertTrue(runCatching {legacyDigestBytes(ByteArray(31))}.isFailure)
        assertEquals(setOf(Programs.MEMO),firstSeenPrograms(listOf(Programs.MEMO,Programs.TOKEN),setOf(Programs.TOKEN)))
        assertTrue(firstSeenPrograms(listOf(Programs.MEMO),setOf(Programs.MEMO)).isEmpty())
        assertTrue(runCatching {firstSeenPrograms(listOf("invalid"),emptySet())}.isFailure)
    }
    @Test fun legacySignatureUsesRawDigestBytesNotHexOrBase64() {
        val bytes=legacyDigestBytes(ByteArray(32){(it*3).toByte()})
        val keys=java.security.KeyPairGenerator.getInstance("EC").apply {initialize(java.security.spec.ECGenParameterSpec("secp256r1"))}.generateKeyPair()
        val signature=java.security.Signature.getInstance("SHA256withECDSA").run {initSign(keys.private);update(bytes);sign()}
        assertTrue(verifyLocalSignature(bytes,signature,keys.public.encoded))
        assertFalse(verifyLocalSignature(bytes.joinToString(""){"%02x".format(it.toInt() and 255)}.toByteArray(),signature,keys.public.encoded))
        assertFalse(verifyLocalSignature(Base64.getEncoder().encode(bytes),signature,keys.public.encoded))
    }
    @Test fun accountEffectsDecodeOnlyExactReadOnlyCreateAccount() {
        val from=Base58.encode(ByteArray(32){2});val to=Base58.encode(ByteArray(32){3})
        val bytes=ByteArray(52);bytes[4]=42;bytes[12]=64;ByteArray(32){4}.copyInto(bytes,20)
        val ix=Instruction("11111111111111111111111111111111",listOf(Role(from,true,true),Role(to,true,true)),bytes)
        val effects=explainAccountEffects(ix,Policy.DEVNET_MEMO_V1)
        assertEquals("READ_ONLY_CREATE_ACCOUNT",effects.status);assertEquals("42",effects.fundingLamports);assertEquals("64",effects.spaceBytes)
        assertFalse(decodeInstruction(ix,Policy.DEVNET_MEMO_V1).allowed)
        for(changed in listOf(ix.copy(data=bytes+byteArrayOf(0)),ix.copy(accounts=listOf(Role(from,true,true),Role(to,false,true))),ix.copy(program=Programs.STAKE),ix.copy(data=bytes.copyOf().also {it[0]=14}))) assertEquals("UNKNOWN",explainAccountEffects(changed,Policy.DEVNET_MEMO_V1).status)
        val memo=Instruction(Programs.MEMO,listOf(Role(from,true,false)),"Deproof devnet memo 1".toByteArray())
        assertEquals("KNOWN_MEMO",explainAccountEffects(memo,Policy.DEVNET_MEMO_V1).status)
        assertEquals("UNKNOWN",explainAccountEffects(memo.copy(data=byteArrayOf(0)),Policy.DEVNET_MEMO_V1).status)
    }
    @Test fun stagedExplanationNeverTransmitsOrChangesAuthorization() {
        val raw=byteArrayOf(1,2,3);val before=raw.copyOf();val provider=ExplanationProviderConfig("grok")
        assertEquals("NOT_CONFIGURED",explainPacket(raw,true,null).status)
        assertEquals("GROK_NOT_CALLED",explainPacket(raw,false,provider).status)
        val unavailable=explainPacket(raw,true,provider);assertEquals("PROVIDER_ERROR",unavailable.status);assertEquals("NONE",unavailable.authorizationEffect)
        assertEquals("PROVIDER_ERROR",explainPacket(ByteArray(128*1024+1),true,provider).status)
        assertArrayEquals(before,raw)
    }
}
