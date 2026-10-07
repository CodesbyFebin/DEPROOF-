# Phase 2 Implementation: Seeker Token (SKR) Reader with Proper Amount Handling

**Status:** In Progress  
**Timeline:** 2 weeks (Phase 2b)  
**Target:** Solana devnet  
**Completion Date:** Week of October 21, 2026

---

## Overview

Phase 2 implements the **Seeker Token (SKR) Reader** — a complete data layer for reading SKR token balances from the Solana blockchain with proper amount handling, error distinction, and UI integration.

### Key Design Principle

**All amounts are stored and transmitted as raw integer strings (no floating-point).**

Example: `"1500000"` represents 1.5 SKR (with 6 decimals), not the float `1.5`.

---

## Architecture

### 1. Domain Models (Immutable)

#### `TokenAmount` (domain/TokenAmount.kt)
Type-safe representation of token amounts with raw integer storage.

**Key Properties:**
- `rawAmount: String` — Raw integer representation (e.g., `"1000000"`)
- `decimals: Int` — Token decimal places (e.g., `6` for SKR)

**Conversions:**
- `toDecimalString(): String` — Human-readable format (e.g., `"1.000000"`)
- `toBigDecimal(): BigDecimal` — Precise arithmetic
- `parseFromDecimal(s, decimals) → TokenAmount?` — Parse from user input

**Guarantees:**
- No floating-point arithmetic ever occurs
- Decimal precision is preserved through parsing/formatting
- Input validation prevents invalid amounts (negative, too many decimals, etc.)

#### `SkrError` (domain/SkrError.kt)
Sealed error hierarchy distinguishing failure modes.

**Error Types:**
1. **`MintNotFound`** — SKR mint account doesn't exist on-chain
2. **`AccountNotFound`** — User's token account doesn't exist (zero balance, not unavailable)
3. **`ConfigNotFound`** — Staking configuration missing (feature unavailable, **not** zero)
4. **`InvalidDecimals`** — Token decimals invalid (config error)
5. **`RpcFailure`** — Network error, timeout, or invalid response
6. **`ParseError`** — RPC response couldn't be parsed
7. **`InvalidAmount`** — User-provided amount is invalid

**Error Distinction Pattern:**
```kotlin
when (val result = reader.getBalance(owner)) {
    is Result.Failure -> {
        when (result.exceptionOrNull()) {
            is SkrError.ConfigNotFound -> {
                // Show "SKR staking not available"
                // This is NOT zero — it's truly unavailable
            }
            is SkrError.AccountNotFound -> {
                // Show "0 SKR" — account exists but empty
            }
            is SkrError.RpcFailure -> {
                // Show "Network error" with retry button
            }
        }
    }
}
```

#### `MintInfo` & `TokenAccount` (domain/MintInfo.kt)
Data classes representing on-chain state.

```kotlin
data class MintInfo(
    val mint: String,
    val decimals: Int,
    val supply: String,
    val mintAuthority: String? = null
)

data class TokenAccount(
    val address: String,      // Token account public key
    val mint: String,         // Which token it holds
    val owner: String,        // Owner wallet
    val amount: String,       // Raw amount (e.g., "1500000")
    val delegated: Boolean = false,
    val delegate: String? = null
)
```

---

### 2. Data Layer

#### `SeekerTokenReader` (data/SeekerTokenReader.kt)
Main reader for SKR token data from Solana RPC.

**Public API:**

```kotlin
class SeekerTokenReader(
    rpcClient: SolanaRpcClient,
    skrMint: String = SKR_MINT,
    skrDecimals: Int = SKR_DECIMALS
)

suspend fun getMintInfo(): Result<MintInfo>
// Fetch SKR mint metadata from blockchain

suspend fun getAllTokenAccounts(owner: String): Result<List<TokenAccount>>
// Enumerate ALL owner's token accounts for SKR
// Includes ATA + non-ATA accounts
// Returns empty list if owner has zero SKR (valid case)

suspend fun getBalance(owner: String): Result<TokenAmount>
// Total SKR balance across all accounts
// Returns TokenAmount("0", 6) if no accounts exist

suspend fun getBalanceAsDecimal(owner: String): Result<String>
// Convenience: returns "1.500000" format

suspend fun validateAmount(decimalString: String): Result<TokenAmount>
// Validates user input (e.g., "1.5")
// Returns TokenAmount("1500000", 6) on success
```

**Error Handling:**
- Missing mint → `SkrError.MintNotFound`
- No accounts for owner → Returns `TokenAmount("0", 6)` (success, not error)
- Missing config → `SkrError.ConfigNotFound` (unavailable, not zero)
- RPC failure → `SkrError.RpcFailure` (transient)

**Amount Arithmetic:**
```kotlin
companion object {
    fun addRawAmounts(a: String, b: String): String
    // "1000000" + "500000" → "1500000"
    // Uses BigInteger internally (no float precision loss)
}
```

---

### 3. UI Layer

#### `SkrTab` (ui/screen/SkrTab.kt)
Jetpack Compose component for NowScreen integration.

**Features:**
- Display current SKR balance with proper formatting
- Refresh button with loading state
- Error display (distinct UI for config unavailable vs network error)
- Staking pool options (Quick stake buttons)
- Unstake all action

**State Management:**
```kotlin
data class SkrTabState(
    val balance: String,      // "1.500000"
    val isLoading: Boolean,
    val error: SkrError? = null,
    val walletAddress: String? = null
)

sealed class SkrTabEvent {
    object RefreshBalance : SkrTabEvent()
    data class StakeRequested(val amount: String) : SkrTabEvent()
    data class UnstakeRequested(val amount: String) : SkrTabEvent()
}
```

**Integration Example:**
```kotlin
@Composable
fun NowScreen() {
    val reader = remember { SeekerTokenReader(rpcClient) }
    val walletAddress = remember { connectedWallet?.publicKey }

    SkrTab(
        reader = reader,
        walletAddress = walletAddress,
        onEvent = { event ->
            when (event) {
                is SkrTabEvent.StakeRequested -> 
                    navigateToStaking(event.amount)
                is SkrTabEvent.RefreshBalance -> 
                    // Already handled in SkrTab
                    Unit
            }
        }
    )
}
```

---

## Amount Handling Details

### Raw Integer Storage

**Why Not Floats?**
Floating-point arithmetic accumulates rounding errors. With cryptocurrencies, even 1 satoshi matters.

**Example Problem:**
```kotlin
// ❌ Float (wrong)
val amount: Float = 1.5f
val result = amount * 1_000_000  // 1500000.0 (but might be 1499999.9999...)

// ✅ String/BigDecimal (correct)
val rawAmount = "1500000"
val result = rawAmount.toBigInteger() * 1_000_000.toBigInteger()
```

### Conversion Flow

**Decimal String → Raw Integer:**
```
User Input: "1.5" (6 decimals)
                ↓
Parse integer part: "1"
Parse fractional part: "5"
Pad to decimals: "5" → "500000"
Combine: "1" + "500000" = "1500000"
```

**Raw Integer → Decimal String:**
```
Raw Amount: "1500000" (6 decimals)
                ↓
Pad to 7 chars: "01500000"
Split at decimals: "1" | "500000"
Format: "1.500000"
Trim trailing zeros: "1.5"
```

### Validation

**Input Validation** (`validateAmount`):
- Must be non-negative
- Decimal places ≤ token decimals (max 6 for SKR)
- Must be valid number format
- Must fit in u64 (on-chain constraint)

**Example:**
```kotlin
reader.validateAmount("1.5")          // ✅ Success
reader.validateAmount("1.0000001")    // ❌ Too many decimals
reader.validateAmount("-1")           // ❌ Negative
reader.validateAmount("abc")          // ❌ Invalid format
```

---

## Error Handling Patterns

### Distinction: Unavailable vs Zero

**Critical Pattern:**
```kotlin
when (val error = result.exceptionOrNull()) {
    is SkrError.ConfigNotFound -> {
        // The feature (staking config) doesn't exist
        // Show: "SKR Staking not available"
        // DON'T show: "0 SKR" — this is different
        showUnavailableUI()
    }
    is SkrError.AccountNotFound -> {
        // Account exists but is empty
        // Show: "0 SKR" with staking options
        showZeroBalance()
    }
    null -> {
        // Success case — might be zero or non-zero
        showBalance(result.getOrNull())
    }
}
```

### Transient vs Permanent Errors

**Transient** (`SkrError.RpcFailure`):
- Show: "Network error, retrying..."
- Action: Automatic retry with exponential backoff
- User action: Manual refresh button

**Permanent** (`SkrError.InvalidAmount`):
- Show: "Invalid amount"
- Action: User must correct input
- No retry needed

---

## Testing Strategy

### Unit Tests

#### `TokenAmountTest` (test/domain/TokenAmountTest.kt)
- ✅ Parsing decimal strings (various decimals, edge cases)
- ✅ Converting to decimal strings
- ✅ Round-trip conversion accuracy
- ✅ Error cases (negative, too many decimals, non-numeric)

#### `SeekerTokenReaderTest` (test/data/SeekerTokenReaderTest.kt)
- ✅ getMintInfo success and failure
- ✅ getAllTokenAccounts empty list vs error
- ✅ getBalance calculation (sum of multiple accounts)
- ✅ validateAmount with various inputs
- ✅ Amount arithmetic (addRawAmounts)

### Test Fixtures

**FakeSolanaRpcClient:**
- Configurable failures (RPC errors)
- Mock responses for successful calls
- No network I/O

**Mock Data:**
```kotlin
val mockMint = MintInfo(
    mint = SKR_MINT,
    decimals = 6,
    supply = "1000000000000"
)

val mockAccount = TokenAccount(
    address = "Ata...",
    mint = SKR_MINT,
    owner = "9B5X...",
    amount = "1500000"
)
```

---

## Integration with ReviewScreen

### Staking Flow

```
NowScreen (SkrTab)
      ↓ (user clicks "Stake 1 SKR")
      ↓
ReviewScreen (stake instruction)
      ↓ (approve/reject)
      ↓
SolanaRpcClient (on-chain settlement)
      ↓
Update balance
```

### Amount Flow

```
User enters "1.5" in ReviewScreen
           ↓
validateAmount("1.5") → TokenAmount("1500000", 6)
           ↓
Create instruction with rawAmount=1500000 (u64)
           ↓
Blockchain processes integer amount
           ↓
Update UI with new balance "0.500000"
```

---

## Success Criteria

✅ **All amounts stored as raw integer strings**
- TokenAmount.rawAmount is always "123456789" not 123.456789

✅ **Error distinction working**
- ConfigNotFound → "Feature unavailable" UI
- RpcFailure → "Network error" with retry
- InvalidAmount → "Invalid input" on form

✅ **ATA + non-ATA enumeration**
- getAllTokenAccounts finds all qualifying accounts
- getBalance sums correctly across multiple accounts

✅ **Decimal precision maintained**
- No floating-point intermediate values
- Parsing and formatting round-trip correctly
- validateAmount prevents invalid input

✅ **All tests pass**
- TokenAmountTest: 25+ test cases
- SeekerTokenReaderTest: 15+ test cases
- UI tests for SkrTab integration

✅ **NowScreen SKR tab displays correctly**
- Shows balance with proper formatting
- Refresh button works
- Error states handled gracefully
- Staking options clickable (navigates to ReviewScreen)

---

## Code Organization

```
app/src/main/kotlin/com/deproof/
├── domain/
│   ├── TokenAmount.kt          (raw amount handling)
│   ├── SkrError.kt             (error types)
│   └── MintInfo.kt             (data models)
├── data/
│   └── SeekerTokenReader.kt    (RPC reader)
└── ui/screen/
    └── SkrTab.kt              (Compose UI)

app/src/test/kotlin/com/deproof/
├── domain/
│   └── TokenAmountTest.kt      (parsing/formatting)
└── data/
    └── SeekerTokenReaderTest.kt (reader logic)
```

---

## Next Steps (Phase 3A)

After Phase 2 completion:

1. **Solana RPC Integration** — Replace mock RPC with real client
2. **Mobile Wallet Adapter** — Connect wallet for transactions
3. **On-Chain Proof Execution** — Settlement and verification
4. **Transaction Monitoring** — Track SKR transfers

---

## References

- **SKR Mint:** `SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3`
- **Decimals:** 6 (1 SKR = 1,000,000 raw units)
- **SPL Token Program:** `TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA`

---

**Owner:** Phase 2b Implementation  
**Branch:** `phase-2b-p2`  
**Target Completion:** October 21, 2026

