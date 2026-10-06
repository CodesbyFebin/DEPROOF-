package com.example.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.example.domain.NodeProtocol
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.util.Base64

/** Encrypts only the node-control seed; never stores or derives Solana wallet secrets. */
class NodeKeyStore {
    private fun key(): SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply {load(null)}
        if(!store.containsAlias("deproof-node-control-aes-v1")) KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("deproof-node-control-aes-v1",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());generateKey()
        }
        return store.getKey("deproof-node-control-aes-v1",null) as SecretKey
    }
    fun create(): String {
        val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());c.updateAAD("deproof-node-control-v1".toByteArray())
        return Base64.getEncoder().encodeToString(c.iv+c.doFinal(NodeProtocol.newSeed()))
    }
    fun protocol(encrypted: String): NodeProtocol {
        val raw=Base64.getDecoder().decode(encrypted);val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,raw.copyOfRange(0,12)));c.updateAAD("deproof-node-control-v1".toByteArray())
        val seed=c.doFinal(raw.copyOfRange(12,raw.size));return try {NodeProtocol(seed)} finally {seed.fill(0)}
    }
}
