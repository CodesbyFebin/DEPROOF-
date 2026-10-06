package com.example.domain

import org.junit.Test
import org.junit.Assert.*
import java.math.BigInteger

// F040 (inspect every instruction) and F041 (plain-language summary).
// CoreTest covers refusals; these tests cover per-instruction verdicts,
// message-level refusal, and the wording of allowed and refused summaries.
class InspectionTest {
    private val account = Base58.encode(ByteArray(32) { 7 })
    private val blockhash = Base58.encode(ByteArray(32) { 9 })
    private val source = Base58.encode(ByteArray(32) { 1 })
    private val dest = Base58.encode(ByteArray(32) { 2 })
    private val token = TokenContext(
        TokenAccount(Programs.TOKEN, Programs.SKR, account, "initialized"),
        TokenAccount(Programs.TOKEN, Programs.SKR, account, "initialized"),
        account, 6, true)
    private val transfer = Instruction(Programs.TOKEN,
        listOf(Role(source, false, true), Role(Programs.SKR, false, false), Role(dest, false, true), Role(account, true, false)),
        transferCheckedData(BigInteger.ONE, 6))
    private fun fails(code: String, block: () -> Unit) { try { block(); fail("Expected $code") } catch (e: Failure) { assertEquals(code, e.code) } }

    @Test fun verdictsArePerInstructionInOrderAndIndependent() {
        val approve = transfer.copy(data = byteArrayOf(4))
        val v = evaluateAllInstructions(listOf(transfer, approve), Policy.SKR_TRANSFER_V1, token)
        assertEquals(2, v.size)
        assertTrue(v[0].allowed)
        assertFalse(v[1].allowed)
        assertEquals("UNSUPPORTED_TOKEN_INSTRUCTION_4", v[1].reason)
        assertEquals("Approve", v[1].summary)
    }

    @Test fun reversedOrderChangesWhichVerdictIsRefused() {
        val approve = transfer.copy(data = byteArrayOf(4))
        val v = evaluateAllInstructions(listOf(approve, transfer), Policy.SKR_TRANSFER_V1, token)
        assertFalse(v[0].allowed)
        assertTrue(v[1].allowed)
    }

    @Test fun devnetMemoPolicyRefusesEveryInstructionOfAMultiInstructionMessage() {
        val memo = buildMemo(account, blockhash, 1000).let { parseTransaction(it).instructions.first() }
        val v = evaluateAllInstructions(listOf(memo, memo), Policy.DEVNET_MEMO_V1)
        assertEquals(2, v.size)
        assertTrue(v.all { !it.allowed && it.reason == "MEMO_POLICY_REQUIRES_ONE_INSTRUCTION" })
    }

    @Test fun emptyMessageIsRejectedBeforeAnyVerdictIsShown() {
        fails("EMPTY_MESSAGE") { evaluateAllInstructions(emptyList(), Policy.SKR_TRANSFER_V1, token) }
    }

    @Test fun allowedTransferSummaryStatesAmountTokenAccountAndRecipient() {
        val v = decodeInstruction(transfer, Policy.SKR_TRANSFER_V1, token)
        assertTrue(v.allowed)
        assertEquals("Allowed by current policy", v.title)
        assertEquals("Transfer ${formatSkr(BigInteger.ONE)} SKR to token account $dest; recipient $account", v.summary)
        assertEquals("TransferChecked policy; this cannot be reversed", v.reason)
    }

    @Test fun allowedMemoSummaryShowsTheExactMemoText() {
        val memo = parseTransaction(buildMemo(account, blockhash, 1000)).instructions.first()
        val v = decodeInstruction(memo, Policy.DEVNET_MEMO_V1)
        assertTrue(v.allowed)
        assertEquals("Devnet memo only; no asset transfer", v.reason)
        assertEquals("Deproof devnet memo 1000", v.summary)
    }
}
