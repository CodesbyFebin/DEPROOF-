package com.example.data

import android.os.Build
import android.security.keystore.*
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import com.example.domain.*
import com.fasterxml.jackson.databind.JsonNode

class EvidenceSigner {
    private val alias = "deproof-evidence-p256-v2"
    fun signEvidenceEnvelope(manifest: ByteArray): JsonNode = sign(evidenceEnvelope(manifest),alias,"deproof-evidence-sig-v2","v2")
    // Legacy import compatibility only. New evidence always uses the v2 envelope above.
    fun signHash(digest: ByteArray): JsonNode = sign(legacyDigestBytes(digest),"deproof-legacy-digest-p256-v1","legacy-digest-bytes-v1","legacy-v1")
    private fun sign(envelope: ByteArray, alias: String, domain: String, version: String): JsonNode {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if(!ks.containsAlias(alias)) {
            fun generate(strong: Boolean) {
                val spec = KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY).setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")).setDigests(KeyProperties.DIGEST_SHA256)
                if(Build.VERSION.SDK_INT >= 28 && strong) spec.setIsStrongBoxBacked(true)
                KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC,"AndroidKeyStore").apply { initialize(spec.build()); generateKeyPair() }
            }
            if (Build.VERSION.SDK_INT >= 28) {
                try { generate(true) } catch(e: StrongBoxUnavailableException) { generate(false) }
            } else {
                generate(false)
            }
        }
        val key = ks.getKey(alias,null) as PrivateKey
        val info = KeyFactory.getInstance(key.algorithm,"AndroidKeyStore").getKeySpec(key,KeyInfo::class.java)
        val level = if(Build.VERSION.SDK_INT >= 31) when(info.securityLevel) {
            KeyProperties.SECURITY_LEVEL_STRONGBOX -> "STRONGBOX"
            KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> "TRUSTED_ENVIRONMENT"
            KeyProperties.SECURITY_LEVEL_SOFTWARE -> "SOFTWARE"
            else -> "UNAVAILABLE"
        } else if(info.isInsideSecureHardware) "HARDWARE_UNSPECIFIED" else "SOFTWARE"
        ensure(level != "SOFTWARE","SOFTWARE_KEY"); ensure(level in listOf("STRONGBOX","TRUSTED_ENVIRONMENT"),"UNQUALIFIED_KEY")
        val der = Signature.getInstance("SHA256withECDSA").run { initSign(key); update(envelope); sign() }
        val spki = ks.getCertificate(alias).publicKey.encoded
        ensure(verifyLocalSignature(envelope,der,spki),"SIGNATURE_SELF_CHECK_FAILED")
        val b64 = Base64.getEncoder()
        return Json.obj("domain" to domain,"algorithm" to "SHA256withECDSA","envelopeBase64" to b64.encodeToString(envelope),"signatureDerBase64" to b64.encodeToString(der),"spkiBase64" to b64.encodeToString(spki),"aliasVersion" to version,"securityLevel" to level,"qualification" to "LOCAL_KEYINFO_OBSERVATION; NOT_REMOTE_ATTESTATION")
    }
}
