package com.example.domain

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// F118 authenticated container; Room and Android wiring live in BackupRepository/BackupWorkspace.
//
// Layout: "DPBK" (4) | version 1 (1) | salt (16) | nonce (12) | AES-256-GCM ciphertext+tag.
// The header is bound as additional authenticated data, so changing the version
// or salt makes authentication fail. Keys come from PBKDF2-HMAC-SHA256.

private val BACKUP_MAGIC = "DPBK".toByteArray(Charsets.US_ASCII)
private const val BACKUP_VERSION: Byte = 1
private const val SALT_BYTES = 16
private const val NONCE_BYTES = 12
private const val HEADER_BYTES = 4 + 1 + SALT_BYTES
private const val MIN_BLOB_BYTES = HEADER_BYTES + NONCE_BYTES + 16
private const val PBKDF2_ITERATIONS = 210_000
private const val MIN_PASSPHRASE_CHARS = 12
private const val MAX_BACKUP_PLAINTEXT_BYTES = 64 * 1024 * 1024
private val backupRandom = SecureRandom()

fun encryptBackup(plaintext: ByteArray, passphrase: CharArray): ByteArray {
    ensure(passphrase.size >= MIN_PASSPHRASE_CHARS, "WEAK_PASSPHRASE")
    ensure(plaintext.size <= MAX_BACKUP_PLAINTEXT_BYTES, "BACKUP_TOO_LARGE")
    val salt = ByteArray(SALT_BYTES).also { backupRandom.nextBytes(it) }
    val nonce = ByteArray(NONCE_BYTES).also { backupRandom.nextBytes(it) }
    val header = backupHeader(salt)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(Cipher.ENCRYPT_MODE, deriveBackupKey(passphrase, salt), GCMParameterSpec(128, nonce))
        updateAAD(header)
    }
    return header + nonce + cipher.doFinal(plaintext)
}

fun decryptBackup(blob: ByteArray, passphrase: CharArray): ByteArray {
    ensure(blob.size <= MAX_BACKUP_PLAINTEXT_BYTES + MIN_BLOB_BYTES, "BACKUP_TOO_LARGE")
    ensure(blob.size >= MIN_BLOB_BYTES, "BACKUP_FORMAT_UNSUPPORTED")
    ensure(blob.copyOfRange(0, 4).contentEquals(BACKUP_MAGIC) && blob[4] == BACKUP_VERSION, "BACKUP_FORMAT_UNSUPPORTED")
    ensure(passphrase.size >= MIN_PASSPHRASE_CHARS, "WEAK_PASSPHRASE")
    val header = blob.copyOfRange(0, HEADER_BYTES)
    val nonce = blob.copyOfRange(HEADER_BYTES, HEADER_BYTES + NONCE_BYTES)
    val ciphertext = blob.copyOfRange(HEADER_BYTES + NONCE_BYTES, blob.size)
    val salt = header.copyOfRange(5, HEADER_BYTES)
    return try {
        Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, deriveBackupKey(passphrase, salt), GCMParameterSpec(128, nonce))
            updateAAD(header)
            doFinal(ciphertext)
        }
    } catch (e: AEADBadTagException) {
        // Same code for a wrong passphrase and for tampering: the two are not distinguished.
        throw Failure("BACKUP_AUTH_FAILED")
    }
}

private fun backupHeader(salt: ByteArray) = BACKUP_MAGIC + byteArrayOf(BACKUP_VERSION) + salt

private fun deriveBackupKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
    val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, 256)
    try {
        return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
    } finally {
        spec.clearPassword()
    }
}
