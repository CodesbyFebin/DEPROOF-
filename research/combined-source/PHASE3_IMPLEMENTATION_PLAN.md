# Phase 3 Implementation Plan - RPC Integration Tests

**Status:** In Progress  
**Priority:** High (backend integration)  
**Requirements:** 40+ test items  
**Environment:** Android Device/Emulator with network access  
**Network:** Solana RPC (Mainnet-beta, Testnet, Devnet)  
**Target SDK:** API 34  
**Min SDK:** API 28

---

## Overview

Phase 3 validates all Solana RPC integration points and real network interactions. The 40+ items are organized into 6 areas:

1. **RPC Connection & Configuration** (8 items) - Endpoint validation, network switching, connection retry
2. **Wallet Integration** (10 items) - MWA (Mobile Wallet Adapter) connection, signing, session management
3. **Balance & Token Queries** (8 items) - SOL balance, token balance (SKR), balance updates, formatting
4. **Transaction Operations** (8 items) - Transfer, token transfer, instruction decoding, fee estimation
5. **Error Handling & Resilience** (4 items) - Network failures, timeout handling, retry logic, fallback behavior
6. **Performance & Optimization** (2 items) - Response time validation, batch query optimization

---

## Testing Strategy

### Network Environments

**Mainnet-beta (Production)**
- Real SOL and SKR tokens
- Live transaction costs
- Production node stability
- Use: Only for read-only operations and validation testing

**Testnet (Staging)**
- Test SOL (free via faucet)
- Test SKR tokens
- Suitable for transaction testing
- Use: Primary environment for write operations

**Devnet (Development)**
- Immediate token availability
- Fast block times
- Recommended for rapid iteration
- Use: Local development and CI/CD pipeline

### Test Infrastructure

**RPC Client Setup**
```kotlin
// Solana RPC client configuration
class RpcClientFactory {
    fun createClient(network: SolanaNetwork): SolanaRpcClient {
        val endpoint = when(network) {
            SolanaNetwork.MAINNET -> "https://api.mainnet-beta.solana.com"
            SolanaNetwork.TESTNET -> "https://api.testnet.solana.com"
            SolanaNetwork.DEVNET -> "https://api.devnet.solana.com"
        }
        return SolanaRpcClient(endpoint)
    }
}
```

**Mock vs Real Testing**
- Unit tests: Mock RPC responses
- Integration tests: Real RPC calls to testnet/devnet
- E2E tests: Real wallet interaction + real transactions

---

## Test Organization

### 1. RPC Connection & Configuration (8 tests)

**Tests:**
- Endpoint validation (mainnet-beta, testnet, devnet)
- Network switching during runtime
- Connection timeout handling
- Fallback endpoint retry logic
- RPC version compatibility check
- Health check before transaction
- Connection pooling efficiency
- DNS resolution validation

**Example Test:**
```kotlin
@Test
fun validateRpcEndpointConnectivity() {
    val rpcClient = RpcClientFactory.createClient(SolanaNetwork.TESTNET)
    
    val result = rpcClient.getHealth()
    
    assert(result.isSuccess)
    assert(result.health == "ok")
}

@Test
fun handleRpcTimeoutWithFallback() {
    val rpcClient = RpcClientFactory.createClient(SolanaNetwork.TESTNET)
    rpcClient.setTimeout(Duration.ofMillis(100)) // Unreasonably short
    
    val result = rpcClient.getBalance(testWallet)
    
    // Should retry with fallback endpoint
    assert(result.isSuccess)
}
```

### 2. Wallet Integration (10 tests)

**Tests:**
- MWA (Mobile Wallet Adapter) connection
- Wallet authorization request handling
- Sign transaction with wallet
- Sign message with wallet
- Multiple wallet support
- Session token management
- Wallet disconnect and cleanup
- Handle missing wallet app
- Wallet permission validation
- Transaction approval UI

**Example Test:**
```kotlin
@Test
fun walletAdapterConnectsSuccessfully() {
    val mwaClient = MobileWalletAdapterClient()
    
    val result = mwaClient.connect()
    
    assert(result.isConnected)
    assert(result.selectedAccount != null)
}

@Test
fun signTransactionWithWallet() {
    val mwaClient = MobileWalletAdapterClient()
    mwaClient.connect()
    
    val transaction = createTestTransfer()
    val signResult = mwaClient.signTransaction(transaction)
    
    assert(signResult.isSuccess)
    assert(signResult.signature != null)
}
```

### 3. Balance & Token Queries (8 tests)

**Tests:**
- Get SOL balance (lamports conversion)
- Get token balance (SKR)
- Balance formatting (decimals)
- Balance refresh/cache invalidation
- Balance polling for changes
- Handle zero balance
- Handle large balances (BigDecimal)
- Associated token account discovery

**Example Test:**
```kotlin
@Test
fun getSolBalanceAndFormat() {
    val rpcClient = RpcClientFactory.createClient(SolanaNetwork.TESTNET)
    
    val balanceLamports = rpcClient.getBalance(testWallet)
    val balanceSol = Formatters.lamportsToSol(balanceLamports)
    
    assert(balanceSol.scale() <= 9) // SOL has 9 decimals
}

@Test
fun getTokenBalanceWithDecimals() {
    val rpcClient = RpcClientFactory.createClient(SolanaNetwork.TESTNET)
    
    val tokenBalance = rpcClient.getTokenBalance(testWallet, skrMintAddress)
    val formatted = Formatters.formatSkr(tokenBalance)
    
    assert(formatted.contains("."))
}
```

### 4. Transaction Operations (8 tests)

**Tests:**
- Create SOL transfer instruction
- Create token transfer instruction
- Estimate transaction fee
- Submit transaction to network
- Wait for transaction confirmation
- Handle transaction failure/rejection
- Decode instruction from Base64
- Multi-signature transaction handling

**Example Test:**
```kotlin
@Test
fun createAndSubmitSolTransfer() {
    val rpcClient = RpcClientFactory.createClient(SolanaNetwork.TESTNET)
    val mwaClient = MobileWalletAdapterClient()
    
    val instruction = SystemProgram.transfer(
        fromPubkey = myWallet,
        toPubkey = recipientWallet,
        lamports = 1000000 // 0.001 SOL
    )
    
    val transaction = Transaction(instructions = listOf(instruction))
    val signature = mwaClient.signAndSendTransaction(transaction)
    
    val confirmed = rpcClient.waitForConfirmation(signature, timeout = Duration.ofSeconds(30))
    assert(confirmed)
}

@Test
fun estimateTransactionFee() {
    val rpcClient = RpcClientFactory.createClient(SolanaNetwork.TESTNET)
    
    val instruction = SystemProgram.transfer(...)
    val transaction = Transaction(instructions = listOf(instruction))
    
    val fee = rpcClient.estimateFee(transaction)
    
    assert(fee > 0)
    assert(fee == 5000) // Current standard fee in lamports
}
```

### 5. Error Handling & Resilience (4 tests)

**Tests:**
- Network timeout with retry
- RPC rate limiting (429 response)
- Invalid transaction rejection
- Insufficient funds handling

**Example Test:**
```kotlin
@Test
fun handleNetworkTimeoutWithRetry() {
    val rpcClient = RpcClientFactory.createClient(SolanaNetwork.TESTNET)
    
    // Simulate network failure
    val result = rpcClient.getBalance(testWallet) // May timeout
    
    // Should retry up to 3 times
    assert(result.isSuccess || result.error is TimeoutException)
}

@Test
fun handleInsufficientFundsError() {
    val rpcClient = RpcClientFactory.createClient(SolanaNetwork.TESTNET)
    val mwaClient = MobileWalletAdapterClient()
    
    val instruction = SystemProgram.transfer(
        fromPubkey = poorWallet,
        toPubkey = richWallet,
        lamports = Long.MAX_VALUE // More than available
    )
    
    val result = rpcClient.simulateTransaction(Transaction(instructions = listOf(instruction)))
    
    assert(!result.isSuccess)
    assert(result.error?.contains("insufficient funds") == true)
}
```

### 6. Performance & Optimization (2 tests)

**Tests:**
- Balance query response time < 500ms
- Batch query optimization

**Example Test:**
```kotlin
@Test
fun balanceQueryResponseTimeUnder500ms() {
    val rpcClient = RpcClientFactory.createClient(SolanaNetwork.TESTNET)
    
    val start = System.currentTimeMillis()
    val balance = rpcClient.getBalance(testWallet)
    val elapsed = System.currentTimeMillis() - start
    
    assert(elapsed < 500)
    assert(balance > 0)
}
```

---

## Test Infrastructure

### RpcRepository Mock Implementation

```kotlin
// Mock for testing without network
class MockRpcRepository : RpcRepository {
    var simulateNetworkFailure = false
    var simulateTimeout = false
    var mockBalance = BigDecimal("1.5")
    var mockTokenBalance = BigDecimal("5000.00")
    
    override suspend fun getBalance(address: String): Result<BigDecimal> {
        return when {
            simulateNetworkFailure -> Result.failure(IOException("Network error"))
            simulateTimeout -> Result.failure(TimeoutException("Request timeout"))
            else -> Result.success(mockBalance)
        }
    }
    
    override suspend fun getTokenBalance(address: String, mint: String): Result<BigDecimal> {
        return when {
            simulateNetworkFailure -> Result.failure(IOException("Network error"))
            else -> Result.success(mockTokenBalance)
        }
    }
}
```

### Real RpcRepository Implementation

```kotlin
// Real implementation using Solana Web3j or native RPC
class SolanaRpcRepository(private val rpcClient: SolanaRpcClient) : RpcRepository {
    override suspend fun getBalance(address: String): Result<BigDecimal> {
        return try {
            val lamports = rpcClient.getBalance(PublicKey(address)).await()
            val sol = lamports.toBigDecimal().divide(BigDecimal(1_000_000_000))
            Result.success(sol)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## CI/CD Integration

### GitHub Actions Workflow

```yaml
name: Phase 3 - RPC Integration Tests

on: [push, pull_request]

jobs:
  integration-tests:
    runs-on: ubuntu-latest
    
    strategy:
      matrix:
        network: [devnet, testnet]
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup JDK
        uses: actions/setup-java@v3
        with:
          java-version: 11
      
      - name: Run RPC Integration Tests
        run: |
          export SOLANA_NETWORK=${{ matrix.network }}
          ./gradlew connectedAndroidTest -Pnetwork=${{ matrix.network }}
      
      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: integration-test-results-${{ matrix.network }}
          path: app/build/reports/androidTests/
```

---

## Success Criteria

- ✅ All 40+ RPC integration tests pass on testnet
- ✅ Transaction operations complete within 30 seconds
- ✅ Balance queries respond in < 500ms
- ✅ Error handling covers all failure scenarios
- ✅ CI/CD pipeline executes tests automatically
- ✅ Network switching works without app restart
- ✅ Wallet integration handles missing wallet app gracefully

---

## Known Blockers & Solutions

**Blocker 1: MWA (Mobile Wallet Adapter) Availability**
- Issue: Requires compatible wallet app installed
- Solution: Mock wallet for CI/CD, test with real wallet locally
- Timeline: Resolved when wallet app available

**Blocker 2: Network Faucet Tokens**
- Issue: Testnet SOL faucets may rate-limit
- Solution: Pre-fund test account, use devnet for rapid iteration
- Timeline: Use existing test account balance

**Blocker 3: Transaction Confirmation Times**
- Issue: Devnet may have slow confirmation times
- Solution: Set reasonable timeout (30-60 seconds), test with cached signatures
- Timeline: Adjust timeouts based on network performance

---

## 4-Week Roadmap

### Week 1: RPC Connection & Configuration
- Set up RPC client factory
- Test endpoint validation
- Implement connection retry logic
- 8 tests for RPC connectivity

### Week 2: Wallet Integration & Balance Queries
- Implement MWA integration
- Test wallet connection flow
- Implement balance querying
- 18 tests for wallet and balance operations

### Week 3: Transactions & Error Handling
- Implement transaction creation
- Test transaction submission
- Implement error handling
- 12 tests for transactions and errors

### Week 4: Performance & CI/CD
- Performance benchmarking
- CI/CD pipeline setup
- Documentation and handoff
- 2 performance tests + pipeline automation

---

## Next Phases

- **Phase 4** (Pending): Security & hardening review (penetration testing, key management audit)
- **Phase 5** (Pending): Documentation completion (API docs, user guides, deployment guide)
- **Phase 6** (Pending): CI/CD pipeline automation (GitHub Actions matrix, Firebase Test Lab integration)

---

**Phase 3 Status:** Ready to Begin  
**Target Completion:** 2 weeks  
**Test Count Target:** 40+ integration tests  
**Coverage Target:** 90%+ of RPC operations
