# Phase 3A Implementation Plan: Solana RPC Integration & Mobile Wallet Adapter

**Status:** Planned  
**Timeline:** 3 weeks  
**Target:** Solana devnet with real wallet connection  
**Completion Date:** Week of October 28, 2026

---

## Overview

Phase 3A connects the Phase 2 SKR reader to the actual Solana blockchain and enables wallet-based transaction signing. This phase bridges the gap between UI and on-chain operations.

### Phase 2 → Phase 3A Connection

**Phase 2 Delivered:**
- ✅ SKR token reader (mock RPC)
- ✅ Type-safe amount handling
- ✅ SkrTab UI component
- ✅ Error handling framework

**Phase 3A Will Deliver:**
- 🔗 Real Solana RPC client
- 🔗 Mobile Wallet Adapter integration
- 🔗 Transaction building & signing
- 🔗 On-chain settlement monitoring
- 🔗 Devnet testing & validation

---

## Architecture

### Layer 1: RPC Integration (Week 1)

#### 1.1 Real SolanaRpcClient Implementation

**File:** `app/src/main/kotlin/com/deproof/data/rpc/SolanaRpcClientImpl.kt`

Replace the mock implementation with real JSON-RPC calls:

```kotlin
class SolanaRpcClientImpl(
    private val endpoint: String,
    private val httpClient: OkHttpClient
) : SolanaRpc {
    
    suspend fun getBalance(address: String): Result<Long>
    // Calls: getBalance [address] → lamports
    
    suspend fun getTokenBalance(
        accountAddress: String,
        mint: String
    ): Result<TokenAmount>
    // Calls: getTokenAccountsByOwner → filter → parseAmount
    
    suspend fun getAccountInfo(address: String): Result<AccountInfo>
    // Calls: getAccountInfo → parse account data
    
    suspend fun simulateTransaction(tx: String): Result<SimulationResult>
    // Calls: simulateTransaction → parse logs
    
    suspend fun sendTransaction(tx: String): Result<String>
    // Calls: sendTransaction → returns signature
}
```

**Key Features:**
- OkHttp client with retry logic (exponential backoff)
- JSON-RPC 2.0 protocol compliance
- Proper error handling (network, parsing, RPC errors)
- Transaction timeout handling (default 30s)
- Connection pooling for efficiency

**Tests:**
- `SolanaRpcClientImplTest` (20+ test cases)
  - Successful calls with various data types
  - Error responses (invalid request, server error)
  - Network failures and retries
  - Response parsing edge cases
  - Timeout handling

#### 1.2 Update SeekerTokenReader

**File:** `app/src/main/kotlin/com/deproof/data/SeekerTokenReader.kt` (modify)

Connect Phase 2 reader to real RPC:

```kotlin
class SeekerTokenReader(
    private val rpc: SolanaRpc,  // Now real, not mock
    private val skrMint: String = SKR_MINT
) {
    suspend fun getMintInfo(): Result<MintInfo> {
        // Call: rpc.getAccountInfo(skrMint) → parse mint layout
    }
    
    suspend fun getAllTokenAccounts(owner: String): Result<List<TokenAccount>> {
        // Call: rpc.getTokenAccountsByOwner(owner, programId=TOKEN)
        // Filter: only accounts with mint == skrMint
    }
}
```

**Tests:** Update existing tests to use real RpcClientImpl

#### 1.3 RPC Configuration & Injection

**File:** `app/src/main/kotlin/com/deproof/di/RpcModule.kt` (new)

```kotlin
@Module
object RpcModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
    
    @Provides
    @Singleton
    fun provideRpcClient(httpClient: OkHttpClient): SolanaRpc =
        SolanaRpcClientImpl(
            endpoint = "https://api.devnet.solana.com",
            httpClient = httpClient
        )
    
    @Provides
    @Singleton
    fun provideSeekerTokenReader(rpc: SolanaRpc): SeekerTokenReader =
        SeekerTokenReader(rpc)
}
```

**Environment Configuration:**
- Dev: devnet endpoint
- Staging: testnet endpoint (future)
- Production: mainnet endpoint (future - NOT ENABLED YET)

---

### Layer 2: Mobile Wallet Adapter (Week 1-2)

#### 2.1 Wallet Connection Flow

**File:** `app/src/main/kotlin/com/deproof/data/wallet/MobileWalletAdapter.kt` (enhance)

Current file exists; enhance with real MWA integration:

```kotlin
class MobileWalletAdapter(
    context: Context
) : WalletRepository {
    
    private val walletAdapter: SolanaMobileWalletAdapter
    
    override suspend fun connect(): Result<WalletAccount> {
        // 1. Launch wallet app discovery
        // 2. Request account (public key, name, icon)
        // 3. Establish persistent connection
        // 4. Return WalletAccount
    }
    
    override suspend fun signTransaction(
        txBytes: ByteArray
    ): Result<SignedTransaction> {
        // 1. Call wallet.signTransaction(txBytes)
        // 2. Wallet signs with private key
        // 3. Return signature
    }
    
    override suspend fun signMessage(
        message: String
    ): Result<String> {
        // 1. Call wallet.signMessage(message)
        // 2. Return signature
    }
}
```

**Key Interactions:**
- Wallet discovery (Phantom, Ledger, etc.)
- Account selection
- Transaction signing
- Message signing
- Wallet disconnection

**Tests:**
- `MobileWalletAdapterTest` (15+ test cases)
  - Successful wallet connection
  - Multiple account handling
  - Transaction signing flow
  - Error handling (user declined, wallet unavailable)
  - Timeout & disconnection scenarios

#### 2.2 Wallet State Management

**File:** `app/src/main/kotlin/com/deproof/ui/viewmodel/WalletViewModel.kt` (new)

```kotlin
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val tokenReader: SeekerTokenReader
) : ViewModel() {
    
    private val _walletState = MutableStateFlow<WalletState>(WalletState.Disconnected)
    val walletState: StateFlow<WalletState> = _walletState.asStateFlow()
    
    fun connectWallet() {
        viewModelScope.launch {
            walletRepository.connect()
                .onSuccess { account ->
                    _walletState.value = WalletState.Connected(account)
                }
                .onFailure { error ->
                    _walletState.value = WalletState.Error(error)
                }
        }
    }
    
    suspend fun getConnectedAddress(): String? =
        (_walletState.value as? WalletState.Connected)?.account?.publicKey
}

sealed class WalletState {
    object Disconnected : WalletState()
    data class Connected(val account: WalletAccount) : WalletState()
    data class Error(val exception: Exception) : WalletState()
}
```

---

### Layer 3: Transaction Building (Week 2)

#### 3.1 Instruction Builders

**File:** `app/src/main/kotlin/com/deproof/domain/tx/SolanaInstruction.kt` (new)

```kotlin
sealed class SolanaInstruction {
    data class TransferToken(
        val source: PublicKey,
        val destination: PublicKey,
        val owner: PublicKey,
        val amount: String,  // Raw amount (from Phase 2 TokenAmount)
        val decimals: Int = 6
    ) : SolanaInstruction()
    
    data class Stake(
        val tokenAccount: PublicKey,
        val stakingPool: PublicKey,
        val amount: String,
        val owner: PublicKey
    ) : SolanaInstruction()
    
    data class Unstake(
        val escrowAccount: PublicKey,
        val tokenAccount: PublicKey,
        val stakingPool: PublicKey,
        val owner: PublicKey
    ) : SolanaInstruction()
}
```

#### 3.2 Transaction Builder

**File:** `app/src/main/kotlin/com/deproof/data/tx/TransactionBuilder.kt` (new)

```kotlin
class TransactionBuilder(
    private val rpc: SolanaRpc
) {
    
    suspend fun buildTransferTx(
        instruction: SolanaInstruction.TransferToken,
        payer: PublicKey
    ): Result<UnsignedTransaction> {
        // 1. Get recent blockhash
        // 2. Create transaction message
        // 3. Add instruction
        // 4. Set fee payer
        // 5. Serialize to bytes
        return Result.success(
            UnsignedTransaction(
                message = message,
                recentBlockhash = blockhash
            )
        )
    }
    
    suspend fun buildStakeTx(
        instruction: SolanaInstruction.Stake,
        payer: PublicKey
    ): Result<UnsignedTransaction> {
        // Similar flow for staking
    }
}

data class UnsignedTransaction(
    val message: TransactionMessage,
    val recentBlockhash: String,
    val serialized: ByteArray  // For wallet signing
)
```

**Tests:**
- `TransactionBuilderTest` (15+ test cases)
  - Instruction serialization
  - Message construction
  - Blockhash handling
  - Signature verification
  - Transaction size validation

#### 3.3 SKR Staking Instructions

**File:** `app/src/main/kotlin/com/deproof/data/tx/SkrStakingInstructions.kt` (new)

```kotlin
object SkrStakingInstructions {
    
    /**
     * Creates a staking instruction for SKR token.
     * Amount must be validated via SeekerTokenReader.validateAmount()
     */
    fun createStakeInstruction(
        userTokenAccount: PublicKey,
        stakingPool: PublicKey,
        amount: String,  // Raw amount from TokenAmount
        owner: PublicKey
    ): SolanaInstruction.Stake {
        return SolanaInstruction.Stake(
            tokenAccount = userTokenAccount,
            stakingPool = stakingPool,
            amount = amount,
            owner = owner
        )
    }
    
    fun createUnstakeInstruction(
        escrow: PublicKey,
        tokenAccount: PublicKey,
        pool: PublicKey,
        owner: PublicKey
    ): SolanaInstruction.Unstake {
        return SolanaInstruction.Unstake(
            escrowAccount = escrow,
            tokenAccount = tokenAccount,
            stakingPool = pool,
            owner = owner
        )
    }
}
```

---

### Layer 4: Settlement Monitoring (Week 2-3)

#### 4.1 Transaction Settlement Tracker

**File:** `app/src/main/kotlin/com/deproof/data/settlement/TransactionSettlementTracker.kt` (new)

```kotlin
class TransactionSettlementTracker(
    private val rpc: SolanaRpc
) {
    
    /**
     * Monitor transaction until confirmation or timeout.
     * Returns final state: success, failed, or timeout.
     */
    suspend fun monitorTransaction(
        signature: String,
        maxWaitMs: Long = 60000
    ): Result<TransactionResult> {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            val status = rpc.getTransactionStatus(signature)
            
            when {
                status.isConfirmed() -> return Result.success(
                    TransactionResult.Confirmed(signature, status.slot)
                )
                status.isFailure() -> return Result.failure(
                    SkrError.TransactionFailed(signature, status.error)
                )
                else -> delay(1000)  // Retry after 1 second
            }
        }
        
        return Result.failure(SkrError.TransactionTimeout(signature))
    }
}

sealed class TransactionResult {
    data class Confirmed(val signature: String, val slot: Long) : TransactionResult()
    data class Failed(val signature: String, val error: String) : TransactionResult()
}
```

**Tests:**
- `TransactionSettlementTrackerTest` (12+ test cases)
  - Successful confirmation
  - Failure detection
  - Timeout handling
  - Retry logic
  - Concurrent transaction monitoring

#### 4.2 Receipt Generation

**File:** `app/src/main/kotlin/com/deproof/data/receipt/ReceiptGenerator.kt` (enhance)

Connect to Phase 2's SKR payment job and settlement:

```kotlin
class ReceiptGenerator(
    private val tracker: TransactionSettlementTracker
) {
    
    suspend fun generateReceipt(
        job: SkrPaymentJob,
        signature: String
    ): Result<Receipt> {
        // 1. Monitor transaction settlement
        // 2. Extract on-chain amount transferred
        // 3. Verify SKR mint and decimals
        // 4. Create Receipt with all metadata
        return Result.success(
            Receipt(
                jobId = job.jobId,
                signature = signature,
                amount = job.priceRawLamports.toString(),
                decimals = SKR_DECIMALS,
                timestamp = System.currentTimeMillis(),
                status = "CONFIRMED"
            )
        )
    }
}
```

---

### Layer 5: UI Integration (Week 3)

#### 5.1 Update ReviewScreen

**File:** `app/src/main/kotlin/com/deproof/presentation/ui/screen/SkrPaymentReviewScreen.kt` (enhance)

Connect Phase 2 UI to transaction signing:

```kotlin
@Composable
fun SkrPaymentReviewScreen(
    job: SkrPaymentJob,
    wallet: MobileWalletAdapter,
    onSettled: (Receipt) -> Unit
) {
    var isSigningTx by remember { mutableStateOf(false) }
    var txSignature by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(txSignature) {
        if (txSignature != null) {
            isSigningTx = true
            // Monitor settlement and generate receipt
        }
    }
    
    Button(
        onClick = {
            coroutineScope.launch {
                // 1. Build transaction from job
                val tx = buildStakeTx(job)
                // 2. Request wallet signature
                val signature = wallet.signTransaction(tx)
                txSignature = signature
                // 3. Send transaction
                val sent = rpc.sendTransaction(signature)
            }
        },
        enabled = !isSigningTx
    ) {
        Text(if (isSigningTx) "Signing..." else "Approve & Stake")
    }
}
```

#### 5.2 Settlement Status UI

**File:** `app/src/main/kotlin/com/deproof/ui/screen/SettlementStatusScreen.kt` (new)

```kotlin
@Composable
fun SettlementStatusScreen(
    signature: String,
    onCompleted: (Receipt) -> Unit
) {
    val settlementTracker = LocalSettlementTracker.current
    var status by remember { mutableStateOf<SettlementStatus>(
        SettlementStatus.Monitoring
    ) }
    
    LaunchedEffect(signature) {
        status = when (val result = settlementTracker.monitorTransaction(signature)) {
            is Result.Success -> SettlementStatus.Confirmed(result.value)
            is Result.Failure -> SettlementStatus.Failed(result.exception)
        }
    }
    
    when (status) {
        SettlementStatus.Monitoring -> {
            SettlementLoadingUI()
        }
        is SettlementStatus.Confirmed -> {
            SettlementSuccessUI(status.receipt) { onCompleted(status.receipt) }
        }
        is SettlementStatus.Failed -> {
            SettlementErrorUI(status.error)
        }
    }
}
```

---

## Testing Strategy

### Unit Tests (Week 1-2)

| Component | Test Count | Coverage |
|-----------|-----------|----------|
| SolanaRpcClientImpl | 20+ | Network, parsing, errors |
| TransactionBuilder | 15+ | Serialization, validation |
| MobileWalletAdapter | 15+ | Connection, signing |
| SkrStakingInstructions | 10+ | Instruction creation |
| TransactionSettlementTracker | 12+ | Monitoring, confirmation |
| **Total** | **72+** | Core functionality |

### Integration Tests (Week 3)

- E2E devnet transaction (connect → sign → settle)
- Multiple concurrent transactions
- Network error recovery
- Wallet timeout handling

### Manual Testing (Week 3)

- Connect Phantom wallet (devnet)
- Execute test stake transaction
- Monitor settlement
- Verify receipt generation

---

## Devnet Setup

### Prerequisites

```bash
# Install Solana CLI
sh -c "$(curl -sSfL https://release.solana.com/v1.18.11/install)"

# Configure for devnet
solana config set --url devnet

# Get devnet SOL (for transaction fees)
solana airdrop 2

# Verify
solana balance
```

### Test Accounts

```bash
# Generate keypair for testing
solana-keygen new --force --no-bip39-passphrase -o test-key.json

# Export public key
solana-keygen pubkey test-key.json
```

### Devnet Faucet

- Solana devnet faucet: https://faucet.solana.com
- Daily limit: 2 SOL per account
- Rate limit: Request once per 24 hours

---

## Success Criteria

✅ **Real RPC Integration**
- All RPC calls use actual Solana devnet endpoint
- Proper error handling for network failures
- Transaction simulation working

✅ **Wallet Connection**
- Phantom wallet discoverable and connectable
- Account selection working
- Transaction signing functional

✅ **Transaction Building**
- Instructions serialize correctly
- Blockhash and recent state handling
- Fee estimation working

✅ **Settlement Monitoring**
- Transactions confirmed on-chain
- Receipt generation complete
- Timeout handling robust

✅ **All Tests Pass**
- 72+ unit tests passing
- Integration tests on devnet passing
- No compiler warnings

✅ **UI Integration**
- ReviewScreen connects to wallet
- SettlementStatusScreen monitors transaction
- Error states handled gracefully

---

## Dependencies to Add

```gradle
// Solana
implementation("com.solana:solanaj:1.3.5")

// Mobile Wallet Adapter
implementation("com.solanomobile:walletadapterkit:2.0.7")

// HTTP Client (already have OkHttp)
implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

// Hilt DI (likely already present)
implementation("com.google.dagger:hilt-android:2.48")
```

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Mainnet accidental spending | Never enable mainnet in Phase 3A; require explicit flag |
| User loses SOL to fees | Estimate fees before signing; show cost preview |
| Wallet unavailable (no Phantom) | Fall back to mock for development; document wallet setup |
| Network unreliability | Retry logic with exponential backoff; timeout handling |
| Transaction confirmation delays | Monitor for 60s; show "still confirming..." UI |

---

## Timeline & Milestones

| Week | Tasks | Deliverables |
|------|-------|--------------|
| **Week 1** | RPC client impl + tests; MWA setup | Real RPC working; wallet discoverable |
| **Week 2** | Transaction building; settlement tracking | Instructions serialize; monitor working |
| **Week 3** | UI integration; devnet testing; receipts | Full flow working; 72+ tests passing |

---

## Branch & PR Strategy

**Branch:** `phase-3a-solana-integration`

**PRs:**
1. RPC implementation (Week 1)
2. Wallet integration (Week 1-2)
3. Transaction building (Week 2)
4. Settlement & UI (Week 3)

Each PR includes:
- Implementation code
- Comprehensive tests
- Documentation updates
- Integration examples

---

## Next Phase Preview (Phase 3B)

After Phase 3A completes:

- **Proof Verification** — On-chain proof validation
- **Automated Rewards** — Distribute SKR based on verified proofs
- **Mainnet Preparation** — Security audit, testnet hardening

---

**Owner:** Phase 3A Implementation  
**Status:** Planned → Ready to Start  
**Target Completion:** October 28, 2026

