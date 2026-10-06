package com.example.domain

import org.junit.Test
import org.junit.Assert.*

// FN042: resolveLookupTables(message) must give verified resolution or refusal.
// No lookup-table data is available offline, so the accepted outcomes are the
// static keys of a message without tables, or a refusal when tables are used.
class LookupTableTest {
    private val account = Base58.encode(ByteArray(32) { 7 })
    private val blockhash = Base58.encode(ByteArray(32) { 9 })
    private fun fails(code: String, block: () -> Unit) { try { block(); fail("Expected $code") } catch (e: Failure) { assertEquals(code, e.code) } }

    @Test fun legacyMessageResolvesToItsStaticKeysOnly() {
        val parsed = parseTransaction(buildMemo(account, blockhash, 1000))
        val resolved = resolveLookupTables(parsed)
        assertEquals(parsed.keys, resolved)
        assertEquals(account, resolved.first().address)
    }

    @Test fun versionedMessageWithLookupTableIsRefused() {
        val legacy = buildMemo(account, blockhash, 1000)
        val versioned = legacy.let { it.copyOfRange(0, 65) + byteArrayOf(0x80.toByte()) + it.copyOfRange(65, it.size) + byteArrayOf(0) }
        assertEquals(0, parseTransaction(versioned).version)
        val withTable = versioned.copyOf().also { it[it.lastIndex] = 1 }
        fails("UNRESOLVED_LOOKUP_TABLE") { resolveLookupTables(parseTransaction(withTable)) }
    }

    @Test fun instructionReferencingAnAccountBeyondStaticKeysIsRefused() {
        val legacy = buildMemo(account, blockhash, 1000)
        // Layout: sigCount(1) + sig(64) + header(4) + keys(2*32) + blockhash(32)
        // + ixCount(1) + programIdx(1) + accountsLen(1) -> accountIdx at 168.
        val accountIdxOffset = 1 + 64 + 4 + 64 + 32 + 1 + 1 + 1
        assertEquals(0, legacy[accountIdxOffset].toInt())
        val outOfRange = legacy.copyOf().also { it[accountIdxOffset] = 5 }
        fails("UNRESOLVED_LOOKUP_TABLE") { resolveLookupTables(parseTransaction(outOfRange)) }
    }
}
