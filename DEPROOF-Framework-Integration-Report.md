# DeProof Android Framework Integration Analysis Report

**Date:** 2026-10-07  
**Scope:** AI Framework Integration into DeProof Solana DePIN Android Codebase  
**Author:** Claude Code Analysis  
**Research Materials:** DEPROOF-Combined-Fresh-Source + DEPROOF-Upstream-Research-Pack

---

## Executive Summary

This report analyzes integration patterns for AI agent frameworks and DePIN custody models into the DeProof Android codebase. The research focuses on:

1. **Palinurus-style DePIN Custody Model** - Instruction allowlist, fail-closed security, noncustodial architecture
2. **Solana Agent Kit Patterns** - 60+ on-chain action abstractions, token operations, attestation flows
3. **Existing DeProof Architecture** - Go node agent, Android trust paths, evidence integrity constraints

**Key Finding:** DeProof's existing monorepo architecture already implements custody-agnostic patterns that align with both frameworks. Integration requires adapting specific agent action libraries and command semantics to DeProof's existing Solana domain model and evidence integrity requirements.

---

## Part 1: DeProof's Existing Architecture

### 1.1 Current State Analysis

#### Monorepo Structure
```
deproof/
├── app/                    # Android Kotlin app (no Android imports in domain)
├── node-agent/            # Go agent (ED25519, TLS 1.3, fail-closed)
├── prover-worker/         # Proof computation (separately pinned)
├── contracts/             # Solana onchain (SPL/SKR)
├── web/                   # Static verification website
└── tools/                 # Python verification, node client CLI
```

#### Domain Separation Philosophy
- **Android Domain Code:** No Android imports. Pure Solana parsing (`base58`, `u64`), immutable review hashes, strict instruction policies
- **Adapter Layer:** MWA (Mobile Wallet Adapter), RPC, Room persistence, DataStore, Android Keystore integration
- **UI Layer:** Represents state through Now (current), Review (user approval), Receipts (finalized)

### 1.2 Go Node Agent Architecture

**Identity Management:**
- Ed25519 keypair from OS entropy
- Filesystem storage (0600 private key, 0700 directory) - software protection, not hardware-backed
- Per-session identity, revoked on restart
- No third-party dependencies

**Consent Model:**
- Scoped pairing challenges (24-hour sessions)
- Explicit operation boundaries: session + operation-id + deadline + policy + action + signature
- Fail-closed: unknown JSON fields, duplicates, forged commands, replays all rejected
- No silent failures

**Data Boundaries:**
- READ_NODE scope (observation only)
- SHARE_BANDWIDTH with explicit endpoint, byte cap, rate, deadline
- Measurement: bytes read by HTTP transport (excludes TLS/headers)
- Zero-filled qualification payloads (no private evidence shared)

### 1.3 Android Trust Path

**Message Signing Flow:**
1. Construct exact serialized transaction bytes
2. Hash for user review (immutable hashes)
3. Sign with wallet (via MWA)
4. Verify returned signature contains reviewed message
5. Reject mixed instructions, modified fee payer, stale approval

**Evidence Storage (Room):**
- Immutable event payloads (append-only)
- Separate submission observations vs. chain observations
- Durable WALLET_SIGNED intent precedes RPC send
- Crash recovery/reconciliation (unfinished, blocks release)

**SKR (Seeker Token) Integration:**
- Raw amounts as integers/decimal strings (no floating UI amounts)
- Verify mint owner, decimals, program version independently
- All qualified token accounts checked (not just ATA)
- Missing config remains unavailable (not zero)
- Staking builders BLOCKED until contracts pass authoritative review

---

## Part 2: Palinurus-Style DePIN Custody Model Integration

### 2.1 What Palinurus Contributes

Palinurus research indicates a DePIN custody framework with:
- **Instruction Allowlist:** Whitelist of valid on-chain actions
- **Fail-Closed Security:** Unknown instructions rejected, no fallback
- **Noncustodial Architecture:** Private keys remain in external wallet
- **Scoped Authority:** Agents execute only approved instruction types
- **Evidence Trail:** Each decision logged with decision boundary

### 2.2 Alignment with DeProof's Existing Model

DeProof's Go node agent **already implements core Palinurus principles:**

| Palinurus Pattern | DeProof Implementation |
|---|---|
| Instruction allowlist | Go agent parses only supported Solana instruction discriminators |
| Fail-closed security | Unknown JSON fields/operations rejected, no fallback parsing |
| Noncustodial | Android Keystore/MWA holds keys; node agent has no key material |
| Scoped authority | SHARE_BANDWIDTH explicitly scoped by endpoint/bytes/rate/deadline |
| Evidence trail | Signed operation logs with exact JSON payloads and signatures |

### 2.3 Integration Point: Enhanced Instruction Policy

**Proposed Adaptation for Android:**

```kotlin
// deproof/app/src/main/java/com/deproof/domain/SolanaInstructionPolicy.kt

sealed class InstructionPolicy {
    object READONLY : InstructionPolicy()
    data class TokenTransfer(
        val mint: String,
        val maxAmount: Long,
        val destinationWhitelist: Set<String>
    ) : InstructionPolicy()
    
    data class StakingAction(
        val poolId: String,
        val action: StakingAction,  // Stake, Unstake, Claim
        val maxSlippageBps: Int
    ) : InstructionPolicy()
}

sealed class StakingAction {
    data class Stake(val amountRaw: Long) : StakingAction()
    data class Unstake(val stakingAccountPda: String) : StakingAction()
    data class Claim(val stakingAccountPda: String) : StakingAction()
}

interface PolicyValidator {
    fun isAllowed(instruction: SolanaInstruction, policy: InstructionPolicy): Result<Unit>
    
    // Fail-closed: any mismatch throws
    fun validateTransactionBoundary(
        reviewedBytes: ByteArray,
        signedTransaction: SignedTransaction
    ): Result<Unit>
}
```

**Security Properties:**
- Allowlist-only: no fallback to permissive parsing
- Strict discriminator matching on-chain program authority
- Amount checks use raw integers (no floating-point errors)
- Destination whitelist prevents token routing surprises
- Fail-closed: mismatches throw, never proceed silently

### 2.4 Node Agent Enhancement: Palinurus-Style Revocation

**Current State:** Restart revokes all sessions

**Proposed Enhancement:**
```go
// node-agent/internal/consent/revocation.go

type SessionRevocation struct {
    SessionID  string
    Reason     RevocationReason  // EXPLICIT, COMPROMISED, SCOPE_VIOLATION
    Timestamp  time.Time
    Evidence   []byte            // Signed by node authority
}

type RevocationReason string

const (
    EXPLICIT_REVOCATION     RevocationReason = "EXPLICIT"
    COMPROMISED             RevocationReason = "COMPROMISED"
    SCOPE_VIOLATION         RevocationReason = "SCOPE_VIOLATION"
    POLICY_CHANGE           RevocationReason = "POLICY_CHANGE"
)

func (a *Agent) Revoke(ctx context.Context, sessionID string, reason RevocationReason) error {
    // Cryptographically sign revocation
    revocation := SessionRevocation{
        SessionID: sessionID,
        Reason:   reason,
        Timestamp: time.Now(),
    }
    
    sig, err := a.signRevocation(revocation)
    if err != nil {
        return fmt.Errorf("revocation signature failed: %w", err)
    }
    
    revocation.Evidence = sig
    
    // Log immutably
    if err := a.log.AppendRevocation(revocation); err != nil {
        return fmt.Errorf("revocation logging failed: %w", err)
    }
    
    // Invalidate all future operations under this session
    return a.sessionStore.Invalidate(sessionID)
}
```

### 2.5 Evidence Integrity: Custody Decision Boundaries

**Key Pattern:** Every custody decision (agent instruction execution) must have a signed evidence marker.

**Implementation in Room:**

```kotlin
// deproof/app/src/main/java/com/deproof/domain/Evidence.kt

@Entity(tableName = "custody_decisions")
data class CustodyDecision(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val nodeSessionId: String,
    val instructionType: String,  // "STAKE", "UNSTAKE", "TRANSFER"
    val instructionBytes: ByteArray,
    val policyConcurrence: String,  // SHA256 of policy text at approval time
    val nodeSignature: ByteArray,    // Ed25519 signature from Go agent
    val transactionHash: String?,
    val status: DecisionStatus
)

enum class DecisionStatus {
    APPROVED,           // Policy passed, awaiting user signature
    WALLET_SIGNED,      // User signed via MWA
    SUBMITTED,          // RPC submit called
    CONFIRMED,          // Chain observation
    REVOKED             // Decision revoked by node agent
}

@Dao
interface CustodyDecisionDao {
    @Insert(onConflict = OnConflictStrategy.FAIL)
    suspend fun insert(decision: CustodyDecision)
    
    @Query("SELECT * FROM custody_decisions WHERE status = :status ORDER BY timestamp DESC")
    fun getByStatus(status: DecisionStatus): Flow<List<CustodyDecision>>
}
```

---

## Part 3: Solana Agent Kit Pattern Integration

### 3.1 What Solana Agent Kit Contributes

Research indicates solana-agent-kit provides:
- **60+ On-Chain Action Abstractions** (transfer, swap, stake, claim, etc.)
- **Composable Actions** that chain together (detect → approve → execute)
- **Attestation Patterns** for proof-of-execution
- **Error Recovery** with clear failure semantics
- **Token Metadata Handling** (mint verification, decimal precision)

### 3.2 Adapting for Android/Kotlin

**Pattern 1: Action Definition**

Instead of JavaScript agent functions, define Kotlin sealed classes:

```kotlin
// deproof/app/src/main/java/com/deproof/domain/SolanaAction.kt

sealed class SolanaAction {
    abstract suspend fun validate(): Result<Unit>
    abstract suspend fun toInstruction(): Result<SolanaInstruction>
}

data class TransferSplTokenAction(
    val mint: String,
    val from: PublicKey,
    val to: PublicKey,
    val amount: Long,
    val decimals: Int
) : SolanaAction() {
    
    override suspend fun validate(): Result<Unit> = runCatching {
        require(amount > 0) { "Amount must be positive" }
        require(decimals in 0..8) { "Decimals must be 0-8" }
        require(from != to) { "Cannot transfer to self" }
        // Query mint authority to verify it matches expected program
        verifySplTokenProgram(mint)
    }
    
    override suspend fun toInstruction(): Result<SolanaInstruction> = runCatching {
        val tokenProgram = PublicKey("TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX")
        val ata = findAssociatedTokenAddress(to, PublicKey(mint))
        
        SolanaInstruction(
            programId = tokenProgram,
            accounts = listOf(
                AccountMeta(from, isSigner = false, isWritable = true),
                AccountMeta(ata, isSigner = false, isWritable = true),
                AccountMeta(from, isSigner = true, isWritable = false)
            ),
            data = encodeTransferInstruction(amount)
        )
    }
}

data class SolanaInstruction(
    val programId: PublicKey,
    val accounts: List<AccountMeta>,
    val data: ByteArray
)

data class AccountMeta(
    val pubkey: PublicKey,
    val isSigner: Boolean,
    val isWritable: Boolean
)
```

**Pattern 2: Attestation & Verification**

```kotlin
// deproof/app/src/main/java/com/deproof/domain/ActionAttestation.kt

sealed class ActionAttestation {
    data class OnChainConfirmation(
        val transactionSignature: String,
        val slot: Long,
        val blockTime: Long,
        val confirmed: Boolean,
        val instructionIndex: Int
    ) : ActionAttestation()
    
    data class FailureProof(
        val error: String,
        val timestamp: Long,
        val rpcEndpoint: String,
        val commitment: String
    ) : ActionAttestation()
}

interface ActionAttestationValidator {
    suspend fun verifyTransactionInclusion(
        signature: String,
        expectedInstruction: SolanaInstruction,
        commitment: Commitment
    ): Result<ActionAttestation.OnChainConfirmation>
    
    suspend fun verifyFailure(
        action: SolanaAction,
        error: String
    ): Result<ActionAttestation.FailureProof>
}
```

### 3.3 Token Operations: Direct SKR Integration

**Current SKR Challenges:**
- Community libraries use `Number(account.amount)` (floating-point error risk)
- Only check ATA, not all qualified token accounts
- Cache by wallet without cluster/token-program context
- Missing config becomes zero instead of unavailable

**Proposed Kotlin Adaptation:**

```kotlin
// deproof/app/src/main/java/com/deproof/domain/SeekerTokenOperations.kt

object SeekerTokenConstants {
    const val MINT = "SeekerMint111111111111111111111111111111"
    val MINT_PUBKEY = PublicKey(MINT)
    const val STAKING_PROGRAM = "SeekerStaking1111111111111111111111111111"
    val STAKING_PROGRAM_KEY = PublicKey(STAKING_PROGRAM)
}

data class SeekerTokenState(
    val balance: SeekerBalance,
    val stakedAccounts: List<StakedAccount>,
    val clusterContext: ClusterContext,
    val observationTime: Long
)

sealed class SeekerBalance {
    data class Available(val rawAmount: String) : SeekerBalance()  // Not floating-point
    object NotFound : SeekerBalance()
    object OwnershipError : SeekerBalance()
    object InvalidDecimals : SeekerBalance()
}

data class StakedAccount(
    val address: PublicKey,
    val stakedAmount: String,  // Raw decimals
    val cooldownRemaining: Long,
    val harvestedRewards: String
)

data class ClusterContext(
    val cluster: String,  // "mainnet-beta", "devnet"
    val rpcEndpoint: String,
    val genesisHash: String,
    val tokenProgram: PublicKey
)

class SeekerTokenReader(
    private val rpc: SolanaRpc,
    private val logger: Logger
) {
    
    suspend fun readState(
        wallet: PublicKey,
        cluster: ClusterContext
    ): Result<SeekerTokenState> = runCatching {
        
        // Verify mint first (prevent fake token swaps)
        val mint = rpc.getAccountInfo(SeekerTokenConstants.MINT_PUBKEY)
        require(mint.owner == cluster.tokenProgram) { "Invalid mint owner" }
        require(mint.data.size == 82) { "Invalid mint data size" }
        
        // Read ATA
        val ata = findAssociatedTokenAddress(wallet, SeekerTokenConstants.MINT_PUBKEY)
        val ataAccount = rpc.getAccountInfo(ata)
        
        val balance = when {
            ataAccount == null -> SeekerBalance.NotFound
            ataAccount.owner != cluster.tokenProgram -> SeekerBalance.OwnershipError
            else -> {
                val amount = parseTokenAmount(ataAccount.data)
                SeekerBalance.Available(amount)  // Keep as string to avoid float rounding
            }
        }
        
        // Read staking accounts (query for all PDAs with this wallet as authority)
        val stakedAccounts = queryStakingAccounts(wallet, cluster)
        
        SeekerTokenState(
            balance = balance,
            stakedAccounts = stakedAccounts,
            clusterContext = cluster,
            observationTime = System.currentTimeMillis()
        )
    }
    
    private suspend fun queryStakingAccounts(
        wallet: PublicKey,
        cluster: ClusterContext
    ): List<StakedAccount> {
        // Query all accounts owned by staking program
        val accounts = rpc.getProgramAccounts(
            SeekerTokenConstants.STAKING_PROGRAM_KEY,
            filters = listOf(
                RpcFilter.Memcmp(offset = 32, bytes = wallet.toBase58())
            )
        )
        
        return accounts
            .mapNotNull { (address, account) ->
                try {
                    val data = parseStakingAccount(account.data)
                    StakedAccount(
                        address = address,
                        stakedAmount = data.amount,
                        cooldownRemaining = data.cooldownEnds - System.currentTimeMillis() / 1000,
                        harvestedRewards = data.rewards
                    )
                } catch (e: Exception) {
                    logger.warn("Failed to parse staking account $address", e)
                    null
                }
            }
    }
    
    private fun parseTokenAmount(data: ByteArray): String {
        // Read raw u64 at offset 64
        val amount = data.sliceArray(64..71).toLong()
        return amount.toString()  // Return as string, no conversion
    }
    
    private fun parseStakingAccount(data: ByteArray): StakingAccountData {
        // Parse discriminator, authority, amount, cooldown, rewards
        return StakingAccountData(
            amount = data.slice(8..15).toByteArray().toLong().toString(),
            cooldownEnds = data.slice(16..23).toByteArray().toLong(),
            rewards = data.slice(24..31).toByteArray().toLong().toString()
        )
    }
}

data class StakingAccountData(
    val amount: String,
    val cooldownEnds: Long,
    val rewards: String
)
```

### 3.4 Composable Action Pipeline

```kotlin
// deproof/app/src/main/java/com/deproof/domain/ActionPipeline.kt

class SeekerActionPipeline(
    private val tokenReader: SeekerTokenReader,
    private val policyValidator: PolicyValidator,
    private val actionAttestationValidator: ActionAttestationValidator
) {
    
    suspend fun stake(
        wallet: PublicKey,
        amount: String,
        cluster: ClusterContext
    ): Result<ActionAttestation> = runCatching {
        
        // Step 1: Read current state
        val state = tokenReader.readState(wallet, cluster).getOrThrow()
        
        // Step 2: Verify sufficient balance (use raw strings for comparison)
        val (balance, _) = state.balance as? SeekerBalance.Available
            ?: throw IllegalStateException("No balance available")
        require(balance.toLongOrNull()!! >= amount.toLongOrNull()!!) {
            "Insufficient balance"
        }
        
        // Step 3: Create staking action
        val action = StakeAction(
            wallet = wallet,
            amount = amount,
            clusterContext = cluster
        )
        
        // Step 4: Validate against policy
        policyValidator.isAllowed(
            action.toInstruction().getOrThrow(),
            InstructionPolicy.READONLY  // or appropriate policy
        ).getOrThrow()
        
        // Step 5: Create transaction and sign (via MWA)
        val transaction = createStakingTransaction(action, cluster)
        // ... MWA signing flow ...
        
        // Step 6: Submit and attestate
        val signature = rpc.sendTransaction(transaction)
        actionAttestationValidator.verifyTransactionInclusion(
            signature,
            action.toInstruction().getOrThrow(),
            Commitment.CONFIRMED
        ).getOrThrow()
    }
}
```

---

## Part 4: Integration Architecture for Android

### 4.1 Enhanced Domain Model

```
deproof/app/src/main/java/com/deproof/domain/
├── SolanaAction.kt              # Sealed classes for all action types
├── SolanaInstructionPolicy.kt   # Allowlist-based validation
├── SeekerTokenOperations.kt     # SKR reading & staking
├── CustodyDecision.kt           # Evidence room entities
├── ActionAttestation.kt         # On-chain verification
└── Transaction*.kt              # Message signing, broadcast
```

**No Android imports:** All domain code remains testable without framework.

### 4.2 Adapter Layer: MWA Integration

```kotlin
// deproof/app/src/main/java/com/deproof/android/wallet/MwaAdapter.kt

class MobileWalletAdapterBridge(
    private val context: Context,
    private val domainPolicy: PolicyValidator
) {
    
    suspend fun signTransaction(
        transaction: SolanaTransaction,
        policy: InstructionPolicy
    ): Result<SignedTransaction> = runCatching {
        
        // Domain validation before MWA
        domainPolicy.validateTransactionBoundary(
            transaction.serializeToBytes(),
            null  // No signature yet
        ).getOrThrow()
        
        // Launch MWA flow
        val mwaResult = launchMwaSignIntent(
            context,
            listOf(transaction)
        )
        
        // Verify returned signature matches reviewed bytes
        require(mwaResult.signedTransactions.size == 1) { "Unexpected MWA response count" }
        
        val signed = mwaResult.signedTransactions[0]
        val deserializedTx = VersionedTransaction.deserialize(signed)
        
        // Verify signatures are valid (cryptographic check)
        verifySignatures(deserializedTx).getOrThrow()
        
        SignedTransaction(
            bytes = signed,
            wallet = mwaResult.publicKey,
            signatures = deserializedTx.transaction.signatures
        )
    }
    
    private fun verifySignatures(tx: VersionedTransaction): Result<Unit> = runCatching {
        // Reconstruct message bytes
        val messageBytes = tx.message.serialize()
        
        // Verify each signature
        tx.transaction.signatures.forEachIndexed { i, sig ->
            val pubkey = tx.message.staticAccountKeys[i]
            require(Ed25519.verify(sig, messageBytes, pubkey)) {
                "Signature $i failed verification for account $i"
            }
        }
    }
}
```

### 4.3 Room Persistence with Evidence

```kotlin
// deproof/app/src/main/java/com/deproof/android/persistence/EvidenceDatabase.kt

@Database(
    entities = [
        CustodyDecision::class,
        EventPayload::class,
        ChainObservation::class
    ],
    version = 1
)
abstract class EvidenceDatabase : RoomDatabase() {
    abstract fun custodyDecisionDao(): CustodyDecisionDao
    abstract fun eventPayloadDao(): EventPayloadDao
    abstract fun chainObservationDao(): ChainObservationDao
    
    companion object {
        @Volatile
        private var instance: EvidenceDatabase? = null
        
        fun getInstance(context: Context): EvidenceDatabase =
            instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    EvidenceDatabase::class.java,
                    "deproof_evidence.db"
                )
                    .fallbackToDestructiveMigration()  // TODO: implement proper migrations
                    .build()
                    .also { instance = it }
            }
    }
}

@Entity(tableName = "event_payloads", indices = [Index("transactionHash")])
data class EventPayload(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionHash: String,
    val eventType: String,  // "INSTRUCTION_APPROVED", "WALLET_SIGNED", etc.
    val payload: ByteArray,  // Immutable serialized event
    val timestamp: Long,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val signature: ByteArray  // Signed by creating entity (app/node/chain)
)

@Entity(tableName = "chain_observations")
data class ChainObservation(
    @PrimaryKey val transactionSignature: String,
    val slot: Long,
    val blockTime: Long,
    val confirmed: Boolean,
    val status: String,  // "SUBMITTED", "CONFIRMED", "FAILED"
    val observationTime: Long,
    val rpcEndpoint: String
)
```

### 4.4 Crash Recovery Protocol

```kotlin
// deproof/app/src/main/java/com/deproof/android/recovery/CrashRecovery.kt

class EvidenceRecoveryManager(
    private val db: EvidenceDatabase,
    private val rpc: SolanaRpc
) {
    
    suspend fun reconcile(): Result<Unit> = runCatching {
        val dao = db.custodyDecisionDao()
        
        // Find all SUBMITTED but not CONFIRMED
        val pendingDecisions = dao.getByStatus(DecisionStatus.SUBMITTED)
        
        for (decision in pendingDecisions) {
            try {
                val txSignature = decision.transactionHash ?: continue
                
                // Query chain for status
                val status = rpc.getTransactionStatus(txSignature)
                
                when {
                    status.isConfirmed -> {
                        dao.updateStatus(decision.id, DecisionStatus.CONFIRMED)
                        // Log chain observation
                        db.chainObservationDao().insert(
                            ChainObservation(
                                transactionSignature = txSignature,
                                slot = status.slot,
                                blockTime = status.blockTime,
                                confirmed = true,
                                status = "CONFIRMED",
                                observationTime = System.currentTimeMillis(),
                                rpcEndpoint = rpc.endpoint
                            )
                        )
                    }
                    status.isFailed -> {
                        dao.updateStatus(decision.id, DecisionStatus.REVOKED)
                    }
                    // UNKNOWN: remains SUBMISSION_UNKNOWN, don't retry blind
                }
            } catch (e: Exception) {
                logger.warn("Recovery for ${decision.id} failed", e)
                // Continue with next decision, don't crash
            }
        }
    }
}
```

---

## Part 5: Security Considerations from Palinurus Model

### 5.1 Fail-Closed Architecture

**Principle:** Unknown is rejected, not accepted.

**Android Implementation:**
```kotlin
class FailClosedValidator {
    fun validateInstruction(instruction: SolanaInstruction): Result<Unit> {
        // Allowlist only - no wildcards
        val allowed = setOf(
            "transfer",           // SPL token transfer
            "initializeMint",      // Mint creation (admin only)
            "stake",              // Seeker stake
            "unstake",            // Seeker unstake
            "claim"               // Harvest rewards
        )
        
        val discriminator = extractDiscriminator(instruction.data)
        require(discriminator in allowed) {
            "Instruction $discriminator not in allowlist. This is fail-closed: unknown instructions are rejected."
        }
        
        return Result.success(Unit)
    }
}
```

### 5.2 Noncustodial Evidence

**Principle:** Node agent never holds keys; evidence is non-custodial.

**Pattern:**
- Private keys: Android Keystore + External wallet (MWA)
- Node agent: Observes, signs decisions, revokes permissions
- Chain: Source of truth for on-chain state
- Local storage: Immutable evidence, never keys

### 5.3 Scoped Delegation

**Current:** Node agent SHARE_BANDWIDTH has explicit bounds.

**Extended for Actions:**
```kotlin
data class DelegatedActionScope(
    val sessionId: String,
    val allowedActions: Set<ActionType>,
    val constraints: ActionConstraints,
    val expiresAt: Long
)

data class ActionConstraints(
    val maxTransactionSize: Int = 1280,
    val maxInstructionCount: Int = 2,
    val tokenTransferMaxAmount: String = "0",
    val stakingPoolWhitelist: Set<String> = emptySet(),
    val clusterRestriction: String = "devnet"  // Prevent mainnet mistakes
)
```

### 5.4 Immutable Decision Logs

**Every custody decision** must have:
1. **Immutable event** in Room (never updated, only inserted)
2. **Signature** from decision-maker (app user or node agent)
3. **Chain observation** (separate from submission acknowledgment)
4. **Recovery procedure** for crashes (reconcile, don't replay)

---

## Part 6: Implementation Roadmap

### Phase 1: Foundation (Weeks 1-2)
- [ ] Extract `SolanaAction` sealed classes from existing Solana parsing
- [ ] Add `InstructionPolicy` validation layer
- [ ] Extend `CustodyDecision` entity with node signatures
- [ ] Write fail-closed validator

**Acceptance Criteria:**
- All new domain code has zero Android imports
- Unit tests for policy validation pass
- Existing Android app continues to build/run

### Phase 2: Seeker Token Integration (Weeks 3-4)
- [ ] Implement `SeekerTokenReader` with raw amount handling
- [ ] Add staking account enumeration (not just ATA)
- [ ] Connect to existing `skr-tab` UI
- [ ] Replace community SDK's `Number(amount)` with string-based reads

**Acceptance Criteria:**
- `SeekerTokenState` correctly parses mint owner, decimals, ATAs
- Staking accounts without config don't report zero balance
- Cluster context preserved through observation chain

### Phase 3: MWA Bridge Enhancement (Weeks 5-6)
- [ ] Add pre-MWA policy validation
- [ ] Implement signature verification
- [ ] Wire to crash recovery protocol
- [ ] Test with devnet wallet

**Acceptance Criteria:**
- Policy failures prevent MWA launch
- Invalid signatures rejected
- App recovers from killed RPC sends

### Phase 4: Node Agent Custody Commands (Weeks 7-8)
- [ ] Add `Revoke(sessionId, reason)` to Go agent
- [ ] Implement revocation signing
- [ ] Add revocation to immutable log
- [ ] Wire to Android session management

**Acceptance Criteria:**
- Revoked sessions reject all new operations
- Revocation reason logged cryptographically
- Android UI reflects revocation state

### Phase 5: Testing & Documentation (Weeks 9-10)
- [ ] End-to-end test: stake flow with fail-closed policy
- [ ] Chaos test: kill RPC, verify recovery
- [ ] Integration test: node agent revocation -> app update
- [ ] Security review: custody boundary validation
- [ ] Document: architecture, policies, attestation flow

**Acceptance Criteria:**
- All phases pass security review
- No Silent failures; all errors logged
- Release blocking issues identified

---

## Part 7: Key Differences from Community SDKs

| Aspect | Community SDK | DeProof + Palinurus/Agent-Kit |
|---|---|---|
| Token Amount | Floating-point `Number()` | Raw integer strings |
| Policy Validation | Permissive (all instructions allowed) | Allowlist-only (fail-closed) |
| Balance Reading | ATA only | All qualified accounts, with error distinction |
| Evidence Trail | UI events only | Immutable Room + signatures |
| Key Custody | Various (risky) | Noncustodial (keys in wallet) |
| Crash Recovery | Silent, no replay | Explicit reconciliation |
| Node Integration | None | Scoped, revocable sessions |

---

## Part 8: Research Pack Integration Notes

### 8.1 Upstream Repositories Used

**Priority 1 (Canonical):**
- `solana-mobile/templates` - Kotlin MWA, message signing examples
- `solana-mobile/solana-mobile-docs` - Protocol spec, SKR documentation

**Reference (Patterns):**
- `saicharanpogul/seeker-sdk` - Community SKR implementation (review required)
- `Saganize/Solwave-kt` - Alternative Kotlin transaction patterns
- `sepivip/SeekerClaw` - SKR interaction examples

**GPL References (Inspect, don't copy):**
- `dappnode/DAppNode` - Host orchestration patterns (Ethereum, not Solana)
- `GokiProtocol/walletkit` - React wallet UI (web, not mobile)

### 8.2 What NOT to Do

1. **Don't paste examples directly** - Adapt for fail-closed semantics
2. **Don't run upstream scripts** - All marked NOT_RUN
3. **Don't use Number(amount)** - Use raw integer strings
4. **Don't assume wallet keys** - Verify external custody
5. **Don't retry blind** - Reconcile submissions, track status separately
6. **Don't infer chain state from UI** - Query directly with error distinction

---

## Conclusion

DeProof's existing architecture already aligns with Palinurus-style custody models and solana-agent-kit's composable action patterns. Integration focuses on:

1. **Enhancing instruction validation** with allowlist-only policies
2. **Fixing SKR reading** with raw amounts and error distinction
3. **Adding custody decision signatures** from node agent
4. **Implementing robust crash recovery** with explicit reconciliation
5. **Testing noncustodial architecture** end-to-end

The 839 files in the research materials provide canonical Solana/Kotlin references. Use official Solana documentation and DEPROOF's existing test suite as the ground truth for implementation.

---

**Report prepared by:** Claude Code Analysis  
**Date:** 2026-10-07  
**Status:** Ready for architectural review and Phase 1 planning
