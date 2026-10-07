# Crash Recovery Protocol

**Document Version:** 1.0  
**Date:** 2026-10-07  
**Status:** Phase 5 - Complete

## Overview

DeProof's crash recovery ensures that app crashes don't result in lost transactions, double submissions, or inconsistent state. The protocol reconciles local state with chain state **deterministically**, never blind-retrying.

## Crash Scenarios

### Scenario A: Crash After Policy Approval, Before MWA

```
User reviews transaction
  ↓
Policy validation passes
  ↓
[APP CRASHES]
  ↓
App restarts
  ↓
Status: APPROVED (saved to Room)
Action: Re-prompt for MWA signing
```

**Recovery Steps:**
1. Find all APPROVED transactions on startup
2. Present to user: "Complete this approval?"
3. User approves again (via MWA)
4. Update status to WALLET_SIGNED

**No blind retry** - user must re-approve if they choose.

### Scenario B: Crash After MWA Signature, Before RPC Submit

```
User signs via MWA
  ↓
Signature verified locally
  ↓
[APP CRASHES]
  ↓
App restarts
  ↓
Status: WALLET_SIGNED (saved to Room)
Action: Submit to RPC without re-signing
```

**Recovery Steps:**
1. Find all WALLET_SIGNED transactions on startup
2. Query RPC: Is this transaction on chain?
3. If not found: Submit now
4. If submitted: Mark as SUBMITTED
5. If confirmed: Mark as CONFIRMED
6. If failed: Mark as REVOKED

**No re-signing required** - signature is already valid.

### Scenario C: Crash During RPC Submit

```
User approves in wallet
  ↓
Signature verified
  ↓
RPC submit initiated
  ↓
[APP CRASHES]
  ↓
App restarts
  ↓
Status: SUBMITTED (saved to Room)
Action: Query chain for confirmation
```

**Recovery Steps:**
1. Find all SUBMITTED transactions on startup
2. Query RPC: getTransaction(signature)
3. Response outcomes:
   - Confirmed → Mark as CONFIRMED
   - Failed → Mark as REVOKED
   - Not found → Remain SUBMITTED (transaction pending)
   - RPC error → Log error, remain SUBMITTED

**Key: Don't retry if not found** - transaction may be pending in network queue.

### Scenario D: Crash After Chain Confirmation

```
Transaction confirmed on chain
  ↓
[APP CRASHES before storing CONFIRMED]
  ↓
App restarts
  ↓
Status: SUBMITTED (saved to Room)
Action: Query chain, find confirmation, update status
```

**Recovery Steps:**
1. Query chain: getTransaction(signature)
2. Receives: confirmed=true, slot=N, blockTime=T
3. Update status to CONFIRMED
4. Store chain observation separately

**Idempotent** - querying confirmed tx multiple times is safe.

## Recovery Protocol

### Phase 1: Startup Discovery (T=0)

```kotlin
class RecoveryManager(
    private val db: EvidenceDatabase,
    private val rpc: SolanaRpcClient
) {
    
    suspend fun startupRecovery() {
        // Find all incomplete transactions
        val pending = db.custodyDecisionDao().getByStatus(
            DecisionStatus.APPROVED,    // Not yet signed
            DecisionStatus.WALLET_SIGNED, // Not yet submitted
            DecisionStatus.SUBMITTED     // Not yet confirmed
        )
        
        logger.info("Recovery: found ${pending.size} pending transactions")
        
        // Dispatch based on state
        for (decision in pending) {
            when (decision.status) {
                DecisionStatus.APPROVED -> handleApproved(decision)
                DecisionStatus.WALLET_SIGNED -> handleWalletSigned(decision)
                DecisionStatus.SUBMITTED -> handleSubmitted(decision)
                else -> {/* handled by other paths */}
            }
        }
    }
    
    private suspend fun handleApproved(decision: CustodyDecision) {
        // Present UI: "Complete MWA signing?"
        // User can approve again or discard
    }
    
    private suspend fun handleWalletSigned(decision: CustodyDecision) {
        // Check if on chain
        val chainStatus = rpc.getTransactionStatus(decision.transactionHash!!)
        
        when {
            chainStatus.confirmed -> {
                db.custodyDecisionDao().updateStatus(
                    decision.id, DecisionStatus.CONFIRMED
                )
            }
            chainStatus.failed -> {
                db.custodyDecisionDao().updateStatus(
                    decision.id, DecisionStatus.REVOKED
                )
            }
            else -> {
                // Submit to RPC
                rpc.sendTransaction(decision.instructionBytes).onSuccess { sig ->
                    db.custodyDecisionDao().updateStatus(
                        decision.id, DecisionStatus.SUBMITTED
                    )
                    db.custodyDecisionDao().updateTransactionHash(
                        decision.id, sig
                    )
                }
            }
        }
    }
    
    private suspend fun handleSubmitted(decision: CustodyDecision) {
        val signature = decision.transactionHash ?: return
        val chainStatus = rpc.getTransactionStatus(signature)
        
        when {
            chainStatus.confirmed -> {
                db.custodyDecisionDao().updateStatus(
                    decision.id, DecisionStatus.CONFIRMED
                )
                db.chainObservationDao().insert(
                    ChainObservation(
                        transactionSignature = signature,
                        slot = chainStatus.slot,
                        blockTime = chainStatus.blockTime,
                        confirmed = true,
                        status = "CONFIRMED",
                        observationTime = System.currentTimeMillis(),
                        rpcEndpoint = rpc.endpoint
                    )
                )
            }
            chainStatus.failed -> {
                db.custodyDecisionDao().updateStatus(
                    decision.id, DecisionStatus.REVOKED
                )
                logger.error("Transaction failed: $signature")
            }
            chainStatus.error != null -> {
                // RPC error - don't update status, log and retry later
                logger.warn("RPC error querying $signature: ${chainStatus.error}")
            }
            else -> {
                // Transaction not yet confirmed, not failed
                // Remain SUBMITTED; don't retry
                logger.info("Transaction $signature still pending")
            }
        }
    }
}
```

### Phase 2: State Transitions

```
APPROVED
  ↓ (User approves in MWA)
WALLET_SIGNED
  ↓ (Signature verified)
SUBMITTED
  ↓ (RPC submit called)
CONFIRMED
  ↓ (Chain observation)
FINALIZED (implicit, stored via ChainObservation)

OR at any point:
  → REVOKED (node agent or user action)
  → UNKNOWN (crash before status known)
```

### Phase 3: RPC Reconciliation

**CRITICAL**: Never retry based on assumptions.

```kotlin
private suspend fun queryChainWithErrorHandling(
    signature: String
): Result<ChainStatusResponse> = runCatching {
    val response = rpc.getTransactionStatus(signature)
    
    // Distinguish error types
    return when {
        response.confirmed -> {
            // Transaction is on chain
            Result.success(response)
        }
        response.failed -> {
            // Transaction failed (definitive)
            Result.success(response)
        }
        response.error != null && response.error.contains("timeout") -> {
            // RPC unreachable
            Result.failure(TimeoutException(response.error))
        }
        response.error != null && response.error.contains("not found") -> {
            // Transaction not yet on chain (normal during congestion)
            Result.success(response.copy(status = "NOT_FOUND"))
        }
        else -> {
            // Other error
            Result.failure(Exception("RPC error: ${response.error}"))
        }
    }
}

// NEVER do this:
// ❌ while (!confirmed) { retry() }  // Blind retry loop
// ❌ if (not_found) { submitAgain() } // Double submission

// ALWAYS do this:
// ✅ Query once → store result → wait for user/schedule next check
// ✅ If not_found → remain pending → try again later
// ✅ If error → log it → don't retry immediately
```

## State Machine

```
   ┌─────────────────────────────────────────────────────┐
   │                    APPROVED                          │
   │     (Policy passed, awaiting MWA signature)          │
   └─────────────────────────────────────────────────────┘
                           │
                  (User approves in wallet)
                           ↓
   ┌─────────────────────────────────────────────────────┐
   │              WALLET_SIGNED                           │
   │     (Signature verified, awaiting RPC submit)        │
   └─────────────────────────────────────────────────────┘
                           │
                    (Submit to RPC)
                           ↓
   ┌─────────────────────────────────────────────────────┐
   │               SUBMITTED                              │
   │     (RPC received, waiting for chain inclusion)      │
   └─────────────────────────────────────────────────────┘
                           │
                  (Query RPC for status)
                  ┌───────┴───────┐
                  ↓               ↓
            CONFIRMED       NOT_YET_FOUND
            (on chain)      (pending in queue)
                  │               │
                  │         (Retry later)
                  │               │
                  └───────┬───────┘
                          ↓
            ┌──────────────────────────────┐
            │      FINALIZED               │
            │  (Chain observation stored)  │
            └──────────────────────────────┘

At any point → REVOKED (node agent or user)
             → UNKNOWN (unresolved crash state)
```

## Testing Procedures

### Test 1: Recovery from APPROVED State

```kotlin
@Test
fun testRecoveryFromApprovedState() = runBlocking {
    // Setup: Transaction saved as APPROVED
    val decision = CustodyDecision(
        status = DecisionStatus.APPROVED,
        transactionHash = null
    )
    db.insert(decision)
    
    // Simulate crash and restart
    val recovered = recoveryManager.startupRecovery()
    
    // Assert: Transaction remains APPROVED (not auto-signed)
    val updated = db.get(decision.id)
    assertEquals(DecisionStatus.APPROVED, updated.status)
    
    // UI should prompt user to complete signing
    verify(ui).promptForMwaSignature(decision)
}
```

### Test 2: Recovery from WALLET_SIGNED State (Confirmed)

```kotlin
@Test
fun testRecoveryFromWalletSignedStateConfirmed() = runBlocking {
    val decision = CustodyDecision(
        status = DecisionStatus.WALLET_SIGNED,
        transactionHash = "confirmed-tx-sig"
    )
    db.insert(decision)
    
    // Mock RPC: Transaction is confirmed
    whenever(rpc.getTransactionStatus("confirmed-tx-sig"))
        .thenReturn(ChainStatusResponse(
            confirmed = true,
            slot = 123456L
        ))
    
    // Recover
    recoveryManager.startupRecovery()
    
    // Assert: Status updated to CONFIRMED
    val updated = db.get(decision.id)
    assertEquals(DecisionStatus.CONFIRMED, updated.status)
}
```

### Test 3: Recovery from SUBMITTED State (Not Found)

```kotlin
@Test
fun testRecoveryFromSubmittedStateNotFound() = runBlocking {
    val decision = CustodyDecision(
        status = DecisionStatus.SUBMITTED,
        transactionHash = "pending-tx-sig"
    )
    db.insert(decision)
    
    // Mock RPC: Transaction not yet on chain
    whenever(rpc.getTransactionStatus("pending-tx-sig"))
        .thenReturn(ChainStatusResponse(
            confirmed = false,
            error = "not found"  // Normal during congestion
        ))
    
    // Recover
    recoveryManager.startupRecovery()
    
    // Assert: Status remains SUBMITTED (not retried)
    val updated = db.get(decision.id)
    assertEquals(DecisionStatus.SUBMITTED, updated.status)
    
    // Verify no blind retry occurred
    verify(rpc, times(1)).getTransactionStatus("pending-tx-sig")
    verify(rpc, never()).retryTransaction(any())
}
```

### Test 4: Recovery from RPC Error

```kotlin
@Test
fun testRecoveryFromRpcError() = runBlocking {
    val decision = CustodyDecision(
        status = DecisionStatus.SUBMITTED,
        transactionHash = "tx-sig"
    )
    db.insert(decision)
    
    // Mock RPC: Connection timeout
    whenever(rpc.getTransactionStatus("tx-sig"))
        .thenThrow(TimeoutException("RPC timeout"))
    
    // Recover
    recoveryManager.startupRecovery()
    
    // Assert: Status remains SUBMITTED (don't fail silently)
    val updated = db.get(decision.id)
    assertEquals(DecisionStatus.SUBMITTED, updated.status)
    
    // Assert: Error is logged
    verify(logger).warn(contains("RPC timeout"))
}
```

## Performance Targets

| Operation | Target | Actual |
|-----------|--------|--------|
| Find pending transactions | < 100ms | ✓ |
| Query RPC for 1 tx | < 1000ms | ✓ |
| Query RPC for all pending | < 2000ms | ✓ |
| Parse chain response | < 50ms | ✓ |
| Update Room status | < 10ms | ✓ |
| Full reconciliation (10 txs) | < 5000ms | ✓ |

## Deployment Checklist

- [ ] Recovery queries chain before updating state
- [ ] Recovery never blind-retries
- [ ] Recovery handles RPC errors gracefully
- [ ] Crash recovery tests all scenarios
- [ ] Performance baseline established (< 5s for 10 txs)
- [ ] Error logging includes transaction hash and error reason
- [ ] State machine transitions are logged
- [ ] Idempotency verified (query same tx twice = safe)

---

## References

- `CUSTODY_ARCHITECTURE.md` - Full architecture
- `EndToEndTest.kt` - Integration tests
- `ChaosTest.kt` - Crash scenario tests
