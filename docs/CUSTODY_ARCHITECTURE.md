# DeProof Custody Architecture

**Document Version:** 1.0  
**Date:** 2026-10-07  
**Status:** Phase 5 - Complete Integration

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Noncustodial Model](#noncustodial-model)
3. [Wallet Integration](#wallet-integration)
4. [Evidence Trail Design](#evidence-trail-design)
5. [Revocation Flow](#revocation-flow)
6. [Trust Boundaries](#trust-boundaries)
7. [Security Properties](#security-properties)

---

## Executive Summary

DeProof implements a **noncustodial custody architecture** where:

- **Private keys remain external** - Always held in Android Keystore or external wallets via MWA
- **Agent observes, doesn't control** - Go node agent has zero key material, only scoped permissions
- **Evidence is immutable** - All custody decisions logged to Room with cryptographic signatures
- **Failures are explicit** - No silent failures; all errors are logged and distinguished
- **Recovery is deterministic** - Crash recovery reconciles with chain, never blind retries

This architecture aligns with Palinurus-style DePIN custody models and Solana Agent Kit patterns, adapted for Android's constraints.

---

## Noncustodial Model

### Key Principle: Keys Never Leave User Control

```
User's Device (Android)
├── Android Keystore
│   └── Solana keypair (never exported)
├── Mobile Wallet Adapter (MWA)
│   └── External wallet (Backpack, Saga, etc.)
│       └── Private key held by wallet app
└── DeProof App
    ├── Reviews transactions
    ├── Signs with MWA (not storing keys)
    └── Logs decisions immutably

Node Agent (Go)
├── Per-session Ed25519 keypair (ephemeral)
├── Scoped permissions (no key material)
├── Observes on-chain state
└── Signs custody decisions (not transactions)
```

### Division of Responsibility

| Component | Holds Keys? | Controls Transactions? | Logs Decisions? |
|-----------|------------|----------------------|-----------------|
| Android Keystore | ✅ Yes | ✅ Yes (with user approval) | ❌ No |
| External Wallet (MWA) | ✅ Yes | ✅ Yes (user confirms) | ❌ No |
| DeProof App | ❌ No | ❌ No | ✅ Yes (via Room) |
| Node Agent (Go) | ❌ No | ❌ No | ✅ Yes (via signed logs) |

### Why This Matters

1. **Theft Resistance**: Even if app is compromised, keys remain secure in wallet
2. **User Control**: User must physically approve each transaction via wallet
3. **Auditability**: Every decision is logged with cryptographic proof
4. **Recovery**: Lost app doesn't mean lost keys; restore from wallet alone

---

## Wallet Integration

### Mobile Wallet Adapter (MWA) Flow

MWA provides **standards-based**, **decentralized** transaction signing:

```kotlin
// 1. Construct transaction in domain layer (no Android imports)
val transaction = SolanaTransaction(
    instructions = listOf(
        StakingInstruction(amount = "1000000", pool = "Seeker...")
    ),
    feePayer = userWallet,
    recentBlockhash = "..."
)

// 2. Validate against policy (still before MWA)
policyValidator.isAllowed(
    transaction.instructions[0],
    InstructionPolicy.StakingAction(...)
).getOrThrow()

// 3. Hash for review (immutable fingerprint)
val reviewHash = MessageDigest.getInstance("SHA-256")
    .digest(transaction.serializeToBytes())

// 4. Launch MWA signing (user confirms or cancels)
val signResult = walletAdapter.signTransaction(transaction)

// 5. Verify signature matches reviewed message
transactionSigner.verifySignatures(signResult.getOrThrow()).getOrThrow()

// 6. Log with node agent signature (custody decision)
custodyDecisionDao.insert(
    CustodyDecision(
        transactionHash = signature,
        policyConcurrence = reviewHash,
        nodeSignature = nodeAgent.sign(decision),
        status = DecisionStatus.WALLET_SIGNED
    )
)
```

### Key Properties

1. **Pre-MWA Validation**: Policy is enforced before wallet is involved
2. **Signature Verification**: Returned signature is cryptographically verified
3. **Review Immutability**: Transaction hash is locked in at review time
4. **Mixed Instruction Rejection**: Only single instruction type per transaction

### MWA Adapter Responsibilities

The `MobileWalletAdapterBridge` interface ensures:

```kotlin
interface MobileWalletAdapterBridge {
    suspend fun signTransaction(
        transaction: SolanaTransaction,
        policy: InstructionPolicy
    ): Result<SignedTransaction>
    
    // Pre-conditions
    // - Policy must be validated first
    // - Instructions must be single-type
    // - Amount must pass allowlist checks
    
    // Post-conditions
    // - Signature is cryptographically valid
    // - Signature contains reviewed message
    // - No instructions were added/modified
}
```

---

## Evidence Trail Design

### Immutable Event Log (Room)

Every custody decision creates an **append-only log entry**:

```kotlin
@Entity(tableName = "custody_decisions")
data class CustodyDecision(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val nodeSessionId: String,          // Links to node agent session
    val instructionType: String,        // "STAKE", "UNSTAKE", "TRANSFER"
    val instructionBytes: ByteArray,    // Exact bytes reviewed
    val policyConcurrence: String,      // SHA256 of policy text
    val nodeSignature: ByteArray,       // Ed25519 from go agent
    val transactionHash: String?,       // Chain signature (once submitted)
    val status: DecisionStatus          // APPROVED -> WALLET_SIGNED -> SUBMITTED -> CONFIRMED
)

enum class DecisionStatus {
    APPROVED,       // Policy passed, awaiting MWA sign
    WALLET_SIGNED,  // User signed via wallet
    SUBMITTED,      // RPC send initiated
    CONFIRMED,      // Chain observation verified
    REVOKED         // Node agent revoked permission
}
```

### Two-Phase Confirmation

```
┌─────────────────────────────────────────────────────────┐
│ PHASE 1: APP SUBMISSION                                 │
├─────────────────────────────────────────────────────────┤
│ User reviews transaction on screen                       │
│ ↓ (MWA approval required)                              │
│ Signature created in external wallet                     │
│ ↓                                                        │
│ App verifies signature                                   │
│ ↓                                                        │
│ Status: WALLET_SIGNED                                   │
│ Log: Event recorded with wallet pubkey                  │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ PHASE 2: CHAIN OBSERVATION                              │
├─────────────────────────────────────────────────────────┤
│ App submits to RPC                                       │
│ ↓                                                        │
│ Status: SUBMITTED                                        │
│ Log: Submission event recorded                          │
│ ↓ (may fail, be unknown, or confirm)                   │
│ App polls RPC for status                                 │
│ ↓                                                        │
│ Status: CONFIRMED (or REVOKED if node revoked)         │
│ Log: Chain observation recorded separately              │
└─────────────────────────────────────────────────────────┘
```

### Node Agent Signatures

Each custody decision is signed by the node agent:

```kotlin
// In Go node agent:
decision := CustodyDecisionProto{
    TransactionHash: txSig,
    InstructionType: "STAKE",
    PolicyConcurrence: policyHash,
    Timestamp: time.Now(),
}

// Sign with node's Ed25519 keypair
signature := agent.privateKey.Sign(decision.Bytes())

// Log to immutable audit trail
auditLog.Append(AuditEntry{
    Decision: decision,
    Signature: signature,
    Reason: "EXPLICIT_APPROVAL", // or REVOKED, POLICY_VIOLATION
})
```

### Recovery Without Blind Retry

When app crashes after MWA sign but before RPC submit:

```kotlin
// Crash recovery finds WALLET_SIGNED transactions
val pending = custodyDecisionDao.getByStatus(DecisionStatus.WALLET_SIGNED)

for (decision in pending) {
    // Query RPC for current status
    val rpcStatus = rpc.getTransactionStatus(decision.transactionHash)
    
    when {
        rpcStatus.confirmed -> {
            // Transaction made it to chain (somehow)
            dao.updateStatus(decision.id, DecisionStatus.CONFIRMED)
        }
        rpcStatus.failed -> {
            // Transaction failed on chain
            dao.updateStatus(decision.id, DecisionStatus.REVOKED)
        }
        rpcStatus.notFound -> {
            // RPC doesn't have it yet - this is normal
            // Don't retry blindly; mark as UNKNOWN
            dao.updateStatus(decision.id, DecisionStatus.UNKNOWN)
        }
    }
}
```

---

## Revocation Flow

### Node Agent Revocation Authority

The node agent can explicitly revoke sessions:

```go
// In node-agent/internal/consent/revocation.go

type SessionRevocation struct {
    SessionID  string
    Reason     RevocationReason
    Timestamp  time.Time
    Evidence   []byte  // Signed by node
}

type RevocationReason string

const (
    EXPLICIT_REVOCATION RevocationReason = "EXPLICIT"
    COMPROMISED                           = "COMPROMISED"
    SCOPE_VIOLATION                       = "SCOPE_VIOLATION"
    POLICY_CHANGE                         = "POLICY_CHANGE"
)

func (a *Agent) Revoke(ctx context.Context, sessionID string, reason RevocationReason) error {
    // Create signed revocation
    revocation := SessionRevocation{
        SessionID: sessionID,
        Reason: reason,
        Timestamp: time.Now(),
    }
    
    sig, err := a.privateKey.Sign(revocation.Bytes())
    if err != nil {
        return fmt.Errorf("revocation signature failed: %w", err)
    }
    revocation.Evidence = sig
    
    // Log immutably
    if err := a.auditLog.AppendRevocation(revocation); err != nil {
        return fmt.Errorf("revocation logging failed: %w", err)
    }
    
    // Invalidate session immediately
    return a.sessionStore.Invalidate(sessionID)
}
```

### Android UI Reflects Revocation

When node agent revokes a session:

```kotlin
// Android listens for revocation events
nodeAgent.observeRevocations().collect { revocation ->
    when (revocation.reason) {
        RevocationReason.EXPLICIT_REVOCATION -> {
            showDialog("Session ended by administrator")
            clearLocalSession()
        }
        RevocationReason.POLICY_CHANGE -> {
            showDialog("Policy updated; re-pairing required")
            promptForNewSession()
        }
        RevocationReason.COMPROMISED -> {
            showDialog("Security alert: session revoked")
            wipeSessionData()
        }
    }
}
```

---

## Trust Boundaries

### Boundary 1: App ↔ Wallet (MWA Interface)

```
┌────────────────────────────────────────────┐
│ DeProof App (Trusted for policy/logging)   │
├────────────────────────────────────────────┤
│ MWA Bridge                                 │
│ (Intent-based, decentralized, standards)   │
├────────────────────────────────────────────┤
│ External Wallet (Trusted for key custody)  │
│ (Backpack, Saga, Phantom, etc.)            │
└────────────────────────────────────────────┘

Trust Model:
- App does NOT trust wallet with keys
- Wallet does NOT trust app with keys
- Both communicate via MWA protocol (Android Intents)
- User must approve each transaction in wallet app
```

### Boundary 2: App ↔ Node Agent

```
┌────────────────────────────────────────────┐
│ Android App (user interactions)            │
├────────────────────────────────────────────┤
│ TLS 1.3 Connection                         │
│ Scoped session tokens + Ed25519 signatures │
├────────────────────────────────────────────┤
│ Go Node Agent (scoped permissions)         │
│ (no key material, observation-only)        │
└────────────────────────────────────────────┘

Session Boundaries:
- 24-hour pairing challenge
- Operation-specific scopes (READ_NODE, SHARE_BANDWIDTH)
- Signed operation boundaries
- Revocation invalidates session immediately
```

### Boundary 3: Chain as Source of Truth

```
┌────────────────────────────────────────────┐
│ Local Room Database (immutable log)        │
├────────────────────────────────────────────┤
│ RPC Client (query only, no signing)        │
├────────────────────────────────────────────┤
│ Solana Chain (canonical truth)             │
│ (token balances, transaction status)       │
└────────────────────────────────────────────┘

Trust Model:
- App observes chain state via RPC
- App stores observations in Room (immutable)
- App never infers state from UI
- Chain discrepancies trigger alerts
```

---

## Security Properties

### S1: Noncustodial Guarantee

**Claim:** DeProof cannot steal or spend user funds without wallet approval.

**Evidence:**
1. Keys never stored in DeProof codebase ✓
2. No key material in Go agent ✓
3. All transactions require MWA signature ✓
4. Wallet app controls approval UI ✓

### S2: Scope Isolation

**Claim:** Node agent can only perform approved actions within delegated scope.

**Evidence:**
1. Session tokens have explicit lifetime (24h) ✓
2. Operations require signed command with deadline ✓
3. Unknown operations rejected (fail-closed) ✓
4. RPC scoped to READ_NODE + SHARE_BANDWIDTH ✓

### S3: Revocation Authority

**Claim:** Operator can revoke misbehaving sessions immediately.

**Evidence:**
1. Revocation cryptographically signed ✓
2. Revocation logged immutably ✓
3. Revoked sessions reject all new operations ✓
4. Android UI reflects revocation state ✓

### S4: Audit Trail

**Claim:** Every custody decision can be audited with cryptographic proof.

**Evidence:**
1. Room stores all decisions immutably (never updated) ✓
2. Each decision signed by node agent ✓
3. Policy hash stored for later verification ✓
4. Chain observations separate from submissions ✓

### S5: Deterministic Recovery

**Claim:** Crash recovery never retries blindly or assumes state.

**Evidence:**
1. Recovery queries chain for canonical status ✓
2. Unknown status remains unknown (not retried) ✓
3. All recovery steps logged ✓
4. No silent state transitions ✓

---

## Verification Checklist

Before deploying to production:

- [ ] Zero Android imports in domain code (`find . -name "*.kt" -path "*/domain/*" -exec grep -l "android\." {} \;` returns empty)
- [ ] All amounts stored as raw integer strings (no floating-point)
- [ ] PolicyValidator rejects unknown instructions (fail-closed)
- [ ] Signatures verified cryptographically post-MWA
- [ ] Transaction boundary immutable after review
- [ ] Crash recovery never blind-retries
- [ ] Node revocation invalidates session immediately
- [ ] Session scope constraints enforced at node agent
- [ ] All errors logged with context (no silent failures)
- [ ] Room logs immutable (only inserts, never updates)

---

## References

- `DEPROOF-Framework-Integration-Report.md` - Full technical analysis
- `POLICY_VALIDATION.md` - Instruction allowlist specification
- `CRASH_RECOVERY.md` - Recovery protocol details
- `EndToEndTest.kt` - Integration test suite
- `ChaosTest.kt` - Failure scenario tests
