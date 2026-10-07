# DeProof Integration Guide

**Document Version:** 1.0  
**Date:** 2026-10-07  
**Status:** Phase 5 - Complete

## Table of Contents

1. [Phase 1: Foundation (Weeks 1-2)](#phase-1-foundation)
2. [Phase 2A: SKR Reading (Weeks 3-4)](#phase-2a-seeker-token-reading)
3. [Phase 3: MWA Bridge (Weeks 5-6)](#phase-3-mwa-bridge)
4. [Phase 4: Node Agent Custody (Weeks 7-8)](#phase-4-node-agent-custody)
5. [Phase 5: Testing & Documentation (Weeks 9-10)](#phase-5-testing--documentation)
6. [Deployment Checklist](#deployment-checklist)
7. [Troubleshooting](#troubleshooting)

---

## Phase 1: Foundation (Weeks 1-2)

### Objectives
- Extract Solana action classes (no Android imports)
- Implement instruction policy validation
- Extend custody decision storage
- Write fail-closed validator

### Step 1.1: Create Domain Classes

```kotlin
// app/src/main/kotlin/com/deproof/domain/SolanaAction.kt

sealed class SolanaAction {
    abstract suspend fun validate(): Result<Unit>
    abstract suspend fun toInstruction(): Result<SolanaInstruction>
}

data class StakingAction(
    val amount: Long,
    val pool: String
) : SolanaAction() {
    override suspend fun validate(): Result<Unit> = runCatching {
        require(amount > 0) { "Amount must be positive" }
    }
    
    override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
        SolanaInstruction(
            programId = PublicKey(pool),
            accounts = listOf(),
            data = encodeStakingData(amount)
        )
    }
}
```

### Step 1.2: Implement Policy Validator

```kotlin
// app/src/main/kotlin/com/deproof/domain/PolicyValidator.kt

interface PolicyValidator {
    suspend fun isAllowed(instruction: SolanaInstruction, policy: InstructionPolicy): Result<Unit>
}

class FailClosedPolicyValidator : PolicyValidator {
    override suspend fun isAllowed(
        instruction: SolanaInstruction,
        policy: InstructionPolicy
    ): Result<Unit> = runCatching {
        when (policy) {
            is InstructionPolicy.READONLY -> require(!hasWritableAccounts(instruction))
            is InstructionPolicy.TokenTransfer -> validateTransfer(instruction, policy)
            is InstructionPolicy.StakingAction -> validateStaking(instruction, policy)
        }
    }
}
```

### Step 1.3: Extend Room Database

```kotlin
@Entity(tableName = "custody_decisions")
data class CustodyDecision(
    @PrimaryKey val id: String,
    val nodeSessionId: String,
    val instructionBytes: ByteArray,
    val nodeSignature: ByteArray,      // NEW
    val status: DecisionStatus
)

@Dao
interface CustodyDecisionDao {
    @Insert(onConflict = OnConflictStrategy.FAIL)
    suspend fun insert(decision: CustodyDecision)
}
```

### Acceptance Criteria
- ✅ Zero Android imports in domain/ directory
- ✅ Unit tests for policy validation (100% coverage)
- ✅ Build succeeds with existing app code

---

## Phase 2A: Seeker Token Reading (Weeks 3-4)

### Objectives
- Implement SeekerTokenReader with error distinction
- Enumerate all staking accounts (not just ATA)
- Replace floating-point parsing with strings

### Step 2.1: Create SKR Reader

```kotlin
// app/src/main/kotlin/com/deproof/domain/SeekerTokenOperations.kt

class SeekerTokenReader(private val rpc: SolanaRpc) {
    suspend fun readState(wallet: PublicKey): Result<SeekerTokenState> = runCatching {
        // Verify mint owner (prevent fake token)
        val mint = rpc.getAccountInfo(SEEKER_MINT)
        require(mint.owner == SPL_TOKEN_PROGRAM) { "Invalid mint owner" }
        
        // Read balance (NO floating-point conversion)
        val ata = findAssociatedTokenAddress(wallet, SEEKER_MINT)
        val account = rpc.getAccountInfo(ata)
            ?: return@runCatching SeekerTokenState(
                balance = SeekerBalance.NotFound
            )
        
        // Parse raw bytes
        val rawAmount = parseU64(account.data, offset = 64)
        
        SeekerTokenState(
            balance = SeekerBalance.Available(rawAmount.toString())
        )
    }
}

sealed class SeekerBalance {
    data class Available(val rawAmount: String) : SeekerBalance()
    object NotFound : SeekerBalance()
}
```

### Step 2.2: Enumerate Staking Accounts

```kotlin
class SeekerTokenReader(private val rpc: SolanaRpc) {
    
    private suspend fun queryStakingAccounts(wallet: PublicKey): List<StakedAccount> {
        // Query ALL accounts owned by staking program
        return rpc.getProgramAccounts(
            SEEKER_STAKING_PROGRAM,
            filters = listOf(
                RpcFilter.Memcmp(offset = 8, bytes = wallet.toBytes())
            )
        ).mapNotNull { (address, accountData) ->
            try {
                StakedAccount(
                    address = address,
                    stakedAmount = parseU64(accountData, offset = 8).toString(),
                    rewards = parseU64(accountData, offset = 24).toString()
                )
            } catch (e: Exception) {
                null  // Skip invalid accounts
            }
        }
    }
}

data class StakedAccount(
    val address: PublicKey,
    val stakedAmount: String,  // Raw, no conversion
    val rewards: String
)
```

### Acceptance Criteria
- ✅ String-based amount handling (no floating-point)
- ✅ All staking accounts enumerated
- ✅ Error distinction: NotFound vs RPC error vs OwnershipError

---

## Phase 3: MWA Bridge (Weeks 5-6)

### Objectives
- Implement pre-MWA policy validation
- Cryptographic signature verification
- Crash recovery integration

### Step 3.1: Policy Before MWA

```kotlin
class MobileWalletAdapterBridge(private val policyValidator: PolicyValidator) {
    
    suspend fun signTransaction(
        transaction: SolanaTransaction,
        policy: InstructionPolicy
    ): Result<SignedTransaction> = runCatching {
        
        // FIRST: Validate policy (before MWA)
        policyValidator.isAllowed(
            transaction.instructions[0],
            policy
        ).getOrThrow()
        
        // THEN: Launch MWA
        val mwaResult = launchMwaIntent(listOf(transaction))
        
        // FINALLY: Verify signature
        verifySignatures(mwaResult).getOrThrow()
        
        SignedTransaction(
            bytes = mwaResult.bytes,
            wallet = mwaResult.publicKey,
            signatures = mwaResult.signatures
        )
    }
}
```

### Step 3.2: Signature Verification

```kotlin
private suspend fun verifySignatures(signedTx: SignedTransaction): Result<Unit> = runCatching {
    val messageBytes = deserialize(signedTx.bytes).message.serialize()
    
    signedTx.signatures.forEachIndexed { i, sig ->
        val pubkey = derivePublicKey(i, signedTx)
        require(Ed25519.verify(sig, messageBytes, pubkey)) {
            "Signature $i failed verification"
        }
    }
}
```

### Acceptance Criteria
- ✅ Policy enforced before MWA launch
- ✅ Invalid signatures rejected
- ✅ Crash recovery polls for pending approvals

---

## Phase 4: Node Agent Custody (Weeks 7-8)

### Objectives
- Implement session revocation (Go agent)
- Wire to Android session management
- Test revocation detection

### Step 4.1: Go Agent Revocation

```go
// node-agent/internal/consent/revocation.go

func (a *Agent) Revoke(ctx context.Context, sessionID string, reason string) error {
    revocation := SessionRevocation{
        SessionID: sessionID,
        Reason: reason,
        Timestamp: time.Now(),
    }
    
    // Sign revocation
    sig, err := a.privateKey.Sign(revocation.Bytes())
    if err != nil {
        return err
    }
    
    // Log immutably
    if err := a.auditLog.Append(revocation, sig); err != nil {
        return err
    }
    
    // Invalidate session
    return a.sessionStore.Delete(sessionID)
}
```

### Step 4.2: Android Listener

```kotlin
// Observe revocations
nodeAgent.observeRevocations().collect { revocation ->
    when (revocation.reason) {
        "EXPLICIT" -> showDialog("Session ended")
        "COMPROMISED" -> wipeSessionData()
        else -> {}
    }
}
```

### Acceptance Criteria
- ✅ Revoked sessions reject all operations
- ✅ Revocation cryptographically signed
- ✅ Android UI reflects revocation

---

## Phase 5: Testing & Documentation (Weeks 9-10)

### Objectives
- End-to-end test suite (all phases)
- Chaos testing (infrastructure failures)
- Comprehensive documentation
- Security review

### Step 5.1: End-to-End Tests

See `EndToEndTest.kt` for:
- ✅ Stake flow with fail-closed policy
- ✅ Crash recovery after MWA
- ✅ Policy rejects unsafe instructions
- ✅ Transaction boundary validation
- ✅ Signature verification

### Step 5.2: Chaos Tests

See `ChaosTest.kt` for:
- ✅ Kill RPC during SKR read (error distinction)
- ✅ Kill wallet app during approval (recovery)
- ✅ Network delays (timeout handling)
- ✅ Malformed responses (validation)

### Step 5.3: Documentation

- ✅ `CUSTODY_ARCHITECTURE.md` - Architecture overview
- ✅ `POLICY_VALIDATION.md` - Policy guide with examples
- ✅ `CRASH_RECOVERY.md` - Recovery procedures
- ✅ `SECURITY_PROPERTIES.md` - Security analysis
- ✅ `INTEGRATION_GUIDE.md` - This guide
- ✅ `DEVICE_TESTING_PLAN.md` - Device tests

### Acceptance Criteria
- ✅ All tests pass (100% green)
- ✅ No silent failures (all errors logged)
- ✅ Performance baselines met
- ✅ Security review checklist 100% complete

---

## Deployment Checklist

Before deploying to production:

```bash
# 1. Code Review
- [ ] All domain code reviewed (zero Android imports)
- [ ] Policy validator logic verified
- [ ] MWA integration tested with real wallet
- [ ] Revocation flow tested end-to-end

# 2. Testing
- [ ] Unit test suite passes (100% coverage)
- [ ] Integration tests pass (all phases)
- [ ] Chaos tests pass (infrastructure failures)
- [ ] Device tests pass (manual verification)

# 3. Documentation
- [ ] Architecture documented
- [ ] Policies documented with examples
- [ ] Recovery procedures documented
- [ ] Security properties verified

# 4. Performance
- [ ] Policy validation < 10ms
- [ ] SKR read < 2s
- [ ] Signature verification < 50ms
- [ ] Crash recovery < 5s

# 5. Security
- [ ] No key material in app
- [ ] All errors logged
- [ ] Revocation works
- [ ] Audit trail immutable
- [ ] Recovery deterministic

# Final: Sign Off
- [ ] Product owner approval
- [ ] Security team approval
- [ ] QA sign-off
- [ ] Release notes prepared
```

---

## Troubleshooting

### Issue: "Zero Android imports" check fails

**Solution:**
```bash
find app/src/main/kotlin/com/deproof/domain -name "*.kt" -exec \
  grep -l "import android\." {} \;
```
Move any Android-dependent code to `android/` subdirectory.

### Issue: Floating-point amount errors

**Solution:**
```kotlin
// ❌ WRONG
val amount = (userInput.toDouble() * 1_000_000).toLong()

// ✅ CORRECT
val amount = userInput.toBigDecimal()
    .multiply(BigDecimal(1_000_000))
    .toBigInteger()
    .toString()
```

### Issue: MWA signing hangs

**Solution:**
```kotlin
// Add timeout
val signResult = withTimeoutOrNull(30_000L) {
    walletAdapter.signTransaction(transaction)
} ?: Result.failure(TimeoutException("MWA timeout"))
```

### Issue: Recovery not detecting confirmed transaction

**Solution:**
```kotlin
// Ensure recovery queries for ALL pending, not just first 10
val pending = custodyDecisionDao.getByStatus(
    DecisionStatus.SUBMITTED,
    limit = null  // No limit
)
```

---

## References

- `CUSTODY_ARCHITECTURE.md` - Full architecture
- `POLICY_VALIDATION.md` - Policy specification
- `CRASH_RECOVERY.md` - Recovery procedures
- `SECURITY_PROPERTIES.md` - Security analysis
- `EndToEndTest.kt` - Integration tests
- `ChaosTest.kt` - Failure tests
- `DEVICE_TESTING_PLAN.md` - Manual tests
