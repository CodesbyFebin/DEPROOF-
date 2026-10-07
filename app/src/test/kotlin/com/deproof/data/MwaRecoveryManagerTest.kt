package com.deproof.data

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MwaRecoveryManagerTest {

    private lateinit var recoveryManager: MwaRecoveryManager

    @Before
    fun setup() {
        recoveryManager = MwaRecoveryManager(null as Any?)
    }

    @Test
    fun `recordPendingApproval should store approval`() = runBlocking {
        val txId = "test_tx_123"
        val messageHash = "hash_abc123"

        val result = recoveryManager.recordPendingApproval(txId, messageHash)

        assertTrue(result.isSuccess)
        val pending = recoveryManager.getPendingApprovals()
        assertEquals(1, pending.size)
        assertEquals(txId, pending[0].transactionId)
        assertEquals(messageHash, pending[0].reviewedMessageHash)
    }

    @Test
    fun `clearPendingApproval should remove approval`() = runBlocking {
        val txId = "test_tx_123"

        recoveryManager.recordPendingApproval(txId, "hash_abc123")
        assertEquals(1, recoveryManager.getPendingApprovals().size)

        val result = recoveryManager.clearPendingApproval(txId)

        assertTrue(result.isSuccess)
        assertEquals(0, recoveryManager.getPendingApprovals().size)
    }

    @Test
    fun `recordPendingApproval should handle multiple approvals`() = runBlocking {
        val txId1 = "tx_001"
        val txId2 = "tx_002"

        recoveryManager.recordPendingApproval(txId1, "hash_001")
        recoveryManager.recordPendingApproval(txId2, "hash_002")

        val pending = recoveryManager.getPendingApprovals()
        assertEquals(2, pending.size)
    }

    @Test
    fun `reconcilePendingApprovals should process pending approvals`() = runBlocking {
        val txId1 = "tx_001"
        val txId2 = "tx_002"

        recoveryManager.recordPendingApproval(txId1, "hash_001")
        recoveryManager.recordPendingApproval(txId2, "hash_002")

        val result = recoveryManager.reconcilePendingApprovals()

        assertTrue(result.isSuccess)
        val reconciliations = result.getOrNull()
        assertTrue(reconciliations != null)
        assertEquals(2, reconciliations?.size)
    }

    @Test
    fun `reconcilePendingApprovals should return results for all pending`() = runBlocking {
        val txIds = (1..5).map { "tx_$String.format("%03d", it)" }

        for (txId in txIds) {
            recoveryManager.recordPendingApproval(txId, "hash_$txId")
        }

        val result = recoveryManager.reconcilePendingApprovals()

        assertTrue(result.isSuccess)
        val reconciliations = result.getOrNull()
        assertEquals(5, reconciliations?.size)
    }

    @Test
    fun `getReconciliationLog should return log entries`() = runBlocking {
        val txId = "test_tx_123"

        recoveryManager.recordPendingApproval(txId, "hash_abc123")
        recoveryManager.reconcilePendingApprovals()

        val log = recoveryManager.getReconciliationLog()
        assertEquals(1, log.size)
    }

    @Test
    fun `clearReconciliationLog should empty log`() = runBlocking {
        val txId = "test_tx_123"

        recoveryManager.recordPendingApproval(txId, "hash_abc123")
        recoveryManager.reconcilePendingApprovals()

        var log = recoveryManager.getReconciliationLog()
        assertEquals(1, log.size)

        val result = recoveryManager.clearReconciliationLog()
        assertTrue(result.isSuccess)

        log = recoveryManager.getReconciliationLog()
        assertEquals(0, log.size)
    }

    @Test
    fun `getPendingApprovals should reflect current state`() = runBlocking {
        assertEquals(0, recoveryManager.getPendingApprovals().size)

        recoveryManager.recordPendingApproval("tx_001", "hash_001")
        assertEquals(1, recoveryManager.getPendingApprovals().size)

        recoveryManager.recordPendingApproval("tx_002", "hash_002")
        assertEquals(2, recoveryManager.getPendingApprovals().size)

        recoveryManager.clearPendingApproval("tx_001")
        assertEquals(1, recoveryManager.getPendingApprovals().size)
    }

    @Test
    fun `pending approval should track creation time`() = runBlocking {
        val txId = "test_tx_123"
        val before = System.currentTimeMillis()

        recoveryManager.recordPendingApproval(txId, "hash_abc123")

        val after = System.currentTimeMillis()
        val pending = recoveryManager.getPendingApprovals()[0]

        assertTrue(pending.createdAt in before..after)
    }

    @Test
    fun `reconcilePendingApprovals should handle error gracefully`() = runBlocking {
        recoveryManager.recordPendingApproval("tx_001", "hash_001")

        val result = recoveryManager.reconcilePendingApprovals()

        assertTrue(result.isSuccess)
        // Should not throw, result should contain reconciliation
        val reconciliations = result.getOrNull()
        assertTrue(reconciliations != null)
    }

    @Test
    fun `clearReconciliationLog should succeed when log empty`() = runBlocking {
        val result = recoveryManager.clearReconciliationLog()

        assertTrue(result.isSuccess)
        assertEquals(0, recoveryManager.getReconciliationLog().size)
    }
}
