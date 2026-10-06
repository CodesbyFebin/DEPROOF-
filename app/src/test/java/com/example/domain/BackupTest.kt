package com.example.domain

import org.junit.Test
import org.junit.Assert.*

// F118 cryptographic container (partial). Runs locally on the JVM.
class BackupTest {
    private val pass = "correct horse battery".toCharArray()
    private val data = "deproof-backup payload: tasks, attachments, receipts".toByteArray(Charsets.UTF_8)
    private fun fails(code: String, block: () -> Unit) { try { block(); fail("Expected $code") } catch (e: Failure) { assertEquals(code, e.code) } }

    @Test fun roundTripRestoresExactBytes() {
        val blob = encryptBackup(data, pass)
        assertArrayEquals(data, decryptBackup(blob, pass))
    }

    @Test fun plaintextIsNotVisibleInTheBlob() {
        val blob = encryptBackup(data, pass)
        assertFalse(String(blob, Charsets.ISO_8859_1).contains("deproof-backup payload"))
    }

    @Test fun eachBackupUsesFreshSaltAndNonce() {
        val a = encryptBackup(data, pass); val b = encryptBackup(data, pass)
        assertFalse(a.contentEquals(b))
    }

    @Test fun wrongPassphraseIsRefused() {
        val blob = encryptBackup(data, pass)
        fails("BACKUP_AUTH_FAILED") { decryptBackup(blob, "wrong horse battery!".toCharArray()) }
    }

    @Test fun tamperedCiphertextIsRefused() {
        val blob = encryptBackup(data, pass)
        blob[blob.lastIndex] = (blob[blob.lastIndex].toInt() xor 1).toByte()
        fails("BACKUP_AUTH_FAILED") { decryptBackup(blob, pass) }
    }

    @Test fun tamperedSaltIsRefusedBecauseTheHeaderIsAuthenticated() {
        val blob = encryptBackup(data, pass)
        blob[5] = (blob[5].toInt() xor 1).toByte()
        fails("BACKUP_AUTH_FAILED") { decryptBackup(blob, pass) }
    }

    @Test fun unsupportedMagicOrVersionIsRefused() {
        val blob = encryptBackup(data, pass)
        val badMagic = blob.copyOf().also { it[0] = 'X'.code.toByte() }
        fails("BACKUP_FORMAT_UNSUPPORTED") { decryptBackup(badMagic, pass) }
        val badVersion = blob.copyOf().also { it[4] = 2 }
        fails("BACKUP_FORMAT_UNSUPPORTED") { decryptBackup(badVersion, pass) }
        fails("BACKUP_FORMAT_UNSUPPORTED") { decryptBackup(blob.copyOf(20), pass) }
    }

    @Test fun shortPassphrasesAreRefusedOnBothSides() {
        fails("WEAK_PASSPHRASE") { encryptBackup(data, "short".toCharArray()) }
        val blob = encryptBackup(data, pass)
        fails("WEAK_PASSPHRASE") { decryptBackup(blob, "short".toCharArray()) }
    }
}
