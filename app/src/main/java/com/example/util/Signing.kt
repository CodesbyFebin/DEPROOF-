package com.example.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.util.*

object HardwareSigning {
    private const val KEY_ALIAS = "deproof_evidence_key"
    private const val KEYSTORE_NAME = "AndroidKeyStore"

    fun isHardwareBackedKeyAvailable(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_NAME)
            keyStore.load(null)
            val key = keyStore.getKey(KEY_ALIAS, null)
            key != null && isInsideSecureHardware()
        } catch (e: Exception) {
            false
        }
    }

    fun isInsideSecureHardware(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_NAME)
            keyStore.load(null)
            val certificate = keyStore.getCertificate(KEY_ALIAS)
            certificate != null
        } catch (e: Exception) {
            false
        }
    }

    fun createOrGetKey() {
        val keyStore = KeyStore.getInstance(KEYSTORE_NAME)
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_NAME
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(false)
                .build()

            keyPairGenerator.initialize(spec)
            keyPairGenerator.generateKeyPair()
        }
    }

    fun signData(data: ByteArray): String? {
        return try {
            if (!isInsideSecureHardware()) {
                throw Exception("SOFTWARE_KEY: Key is not hardware-backed")
            }

            val keyStore = KeyStore.getInstance(KEYSTORE_NAME)
            keyStore.load(null)

            val signature = Signature.getInstance("SHA256withECDSA")
            val key = keyStore.getKey(KEY_ALIAS, null)
            signature.initSign(key as java.security.PrivateKey)
            signature.update(data)

            val signatureBytes = signature.sign()
            Base64Encoder.encode(signatureBytes)
        } catch (e: Exception) {
            if (e.message?.contains("SOFTWARE_KEY") == true) {
                throw e
            }
            null
        }
    }

    fun signHash(hexHash: String): String? {
        return try {
            val hashBytes = hexHash.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            signData(hashBytes)
        } catch (e: Exception) {
            null
        }
    }
}

object Base64Encoder {
    fun encode(data: ByteArray): String {
        return android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT).trim()
    }

    fun decode(encoded: String): ByteArray {
        return android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
    }
}
