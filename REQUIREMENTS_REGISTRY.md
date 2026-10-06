# Deproof P1 MVP - Requirements Registry

**Purpose:** Map 281 requirements to specific feature IDs, acceptance criteria, and test coverage.  
**Format:** `F###-LEVEL-CATEGORY: Requirement Title`

---

## Phase 1: Unit Tests (P0 - Critical Path)

### F040-P0-Tests: Transaction Instruction Inspection
**Category:** Crypto & Security → Instruction Decoder  
**Status:** ❌ MISSING TEST  
**Test File:** `app/src/test/kotlin/com/deproof/crypto/InstructionDecoderTest.kt`

**Acceptance Criteria:**
- [ ] Decode TransferChecked instruction (SPL token transfer)
- [ ] Extract program ID correctly
- [ ] Extract mint address from instruction data
- [ ] Extract decimals from mint info
- [ ] Parse source and destination tokens
- [ ] Identify authority/signer
- [ ] Return Verdict.Payable for standard transfers
- [ ] Return Verdict.DoNotSign for suspicious authority
- [ ] Handle malformed instruction data gracefully
- [ ] Test against known-good Solana instruction bytes

**Test Cases:**
```kotlin
class InstructionDecoderTest {
    // F040-TC-01: Decode SPL TransferChecked (6 decimals, SKR)
    fun testDecodeTransferCheckedSKR() { }
    
    // F040-TC-02: Decode regular SOL transfer
    fun testDecodeTransferSOL() { }
    
    // F040-TC-03: Detect suspicious SetAuthority instruction
    fun testDetectSetAuthorityWarning() { }
    
    // F040-TC-04: Handle unknown program
    fun testUnknownProgramVerdictUnknown() { }
    
    // F040-TC-05: Malformed data length
    fun testMalformedDataThrowsException() { }
    
    // F040-TC-06: Missing required accounts
    fun testMissingAccountsThrowsException() { }
}
```

**Evidence Linking:**
- Links to Commit 5f61ea4 (InstructionDecoder.kt type fixes)
- Links to Commit 5f61ea4 (accounts array handling fix)

---

### F041-P0-Tests: Plain-Language Transaction Summary
**Category:** Domain Logic → Transaction Verdict  
**Status:** ❌ MISSING TEST  
**Test File:** `app/src/test/kotlin/com/deproof/domain/model/VerdictTest.kt`

**Acceptance Criteria:**
- [ ] Verdict.Payable → "Safe to approve" summary
- [ ] Verdict.DoNotSign → "Do not sign" + reason
- [ ] Verdict.Unknown → "Cannot determine" + details
- [ ] Summary includes amount, mint, destination
- [ ] Summary is user-readable (no hex values)
- [ ] Localization hooks present (not implemented yet)

**Test Cases:**
```kotlin
class VerdictTest {
    // F041-TC-01: Payable summary
    fun testPayableVerdictSummary() { }
    
    // F041-TC-02: DoNotSign summary with reason
    fun testDoNotSignVerdictSummary() { }
    
    // F041-TC-03: Unknown verdict summary
    fun testUnknownVerdictSummary() { }
    
    // F041-TC-04: Amount formatting in summary
    fun testAmountFormattingInSummary() { }
    
    // F041-TC-05: No hex values in user-facing text
    fun testNoHexInSummary() { }
}
```

**Evidence Linking:**
- Links to Commit 5f61ea4 (Verdict type definitions)
- Links to CreateReceiptUseCase.kt (verdict determination logic)

---

### F042-P0-Tests: Formatter Suite (Locally Actionable)
**Category:** Utilities → Formatters  
**Status:** ❌ MISSING TESTS  
**Test File:** `app/src/test/kotlin/com/deproof/util/FormattersTest.kt`

**Acceptance Criteria:**
- [ ] formatSol(1.0) → "1 SOL"
- [ ] formatSol(0.000000001) → "0.000000001 SOL" (9 decimals)
- [ ] formatSol(1234567.89) → "1,234,567.89 SOL" (with commas)
- [ ] formatSkr(1.0) → "1.000000 SKR" (6 decimals)
- [ ] formatAddress("...abc123") → "...abc123" (last 8 chars)
- [ ] formatTimestamp(ms) → human-readable date/time
- [ ] formatSignature displays first/last 8 chars

**Test Cases:**
```kotlin
class FormattersTest {
    // F042-TC-01: SOL formatting with decimals
    fun testFormatSolDecimals() { }
    
    // F042-TC-02: SOL thousand separator
    fun testFormatSolThousandSeparator() { }
    
    // F042-TC-03: SKR formatting (6 decimals)
    fun testFormatSkrSixDecimals() { }
    
    // F042-TC-04: Address truncation
    fun testFormatAddressTruncation() { }
    
    // F042-TC-05: Timestamp formatting
    fun testFormatTimestamp() { }
    
    // F042-TC-06: Signature display (first+last)
    fun testFormatSignatureDisplay() { }
    
    // F042-TC-07: Zero amounts
    fun testFormatZeroAmount() { }
    
    // F042-TC-08: Very large amounts
    fun testFormatVeryLargeAmount() { }
}
```

---

### F043-P0-Tests: Validator Suite
**Category:** Utilities → Validators  
**Status:** ❌ MISSING TESTS  
**Test File:** `app/src/test/kotlin/com/deproof/util/ValidatorsTest.kt`

**Acceptance Criteria:**
- [ ] validatePublicKey accepts valid base58 addresses (44 chars)
- [ ] validatePublicKey rejects non-base58 characters
- [ ] validatePublicKey rejects wrong length
- [ ] validateMint accepts token mint addresses
- [ ] validateMint enforces 6 decimals for SKR
- [ ] validateAmount rejects negative amounts
- [ ] validateAmount accepts decimal amounts
- [ ] validateSignature validates 88-char base58 signatures

**Test Cases:**
```kotlin
class ValidatorsTest {
    // F043-TC-01: Valid Solana address
    fun testValidatePublicKeyValid() { }
    
    // F043-TC-02: Invalid characters in address
    fun testValidatePublicKeyInvalidChars() { }
    
    // F043-TC-03: Token mint validation
    fun testValidateMintAddress() { }
    
    // F043-TC-04: SKR mint (6 decimals)
    fun testValidateSKRMint() { }
    
    // F043-TC-05: Amount validation non-negative
    fun testValidateAmountNonNegative() { }
    
    // F043-TC-06: Decimal precision in amounts
    fun testValidateAmountDecimalPrecision() { }
    
    // F043-TC-07: Signature validation
    fun testValidateSignatureFormat() { }
    
    // F043-TC-08: URL validation (https only)
    fun testValidateURLHttpsOnly() { }
}
```

---

### F044-P0-Tests: Base58 Encoding/Decoding
**Category:** Crypto → Base58  
**Status:** ❌ MISSING TESTS  
**Test File:** `app/src/test/kotlin/com/deproof/crypto/Base58Test.kt`

**Acceptance Criteria:**
- [ ] Encode bytes to base58 string
- [ ] Decode base58 string to bytes
- [ ] Round-trip encode/decode preserves data
- [ ] Rejects non-base58 characters on decode
- [ ] Handles 32-byte pubkeys correctly
- [ ] Handles 64-byte signatures correctly
- [ ] No loss of entropy

**Test Cases:**
```kotlin
class Base58Test {
    // F044-TC-01: Encode 32-byte pubkey
    fun testEncode32BytePubkey() { }
    
    // F044-TC-02: Decode valid base58 address
    fun testDecodeValidAddress() { }
    
    // F044-TC-03: Round-trip encode/decode
    fun testRoundTripEncodeDecodePreservesData() { }
    
    // F044-TC-04: Reject invalid character
    fun testDecodeRejectsInvalidCharacter() { }
    
    // F044-TC-05: Signature encoding (64 bytes)
    fun testEncode64ByteSignature() { }
}
```

---

### F045-P0-Tests: SHA-256 Message Binding
**Category:** Crypto → MessageBinding  
**Status:** ❌ MISSING TESTS  
**Test File:** `app/src/test/kotlin/com/deproof/crypto/MessageBindingTest.kt`

**Acceptance Criteria:**
- [ ] createCanonicalMessage formats consistently
- [ ] calculateMessageHash returns 32-byte SHA-256
- [ ] verifyMessageIntegrity detects any modification
- [ ] Hash is deterministic (same input → same hash)
- [ ] Changing 1 bit changes hash completely (avalanche)
- [ ] createTamperDetectionAlert identifies modification point

**Test Cases:**
```kotlin
class MessageBindingTest {
    // F045-TC-01: Canonical message format
    fun testCreateCanonicalMessageFormat() { }
    
    // F045-TC-02: Hash deterministic
    fun testHashDeterministic() { }
    
    // F045-TC-03: Modified message fails verification
    fun testTamperDetectionModifiedMessage() { }
    
    // F045-TC-04: Hash length (32 bytes)
    fun testHashLength32Bytes() { }
    
    // F045-TC-05: Avalanche effect (1-bit change)
    fun testAvalancheEffect() { }
    
    // F045-TC-06: Tamper alert generation
    fun testTamperAlertGeneration() { }
}
```

---

### F046-P0-Tests: BinaryParser Data Extraction
**Category:** Crypto → BinaryParser  
**Status:** ❌ MISSING TESTS  
**Test File:** `app/src/test/kotlin/com/deproof/crypto/BinaryParserTest.kt`

**Acceptance Criteria:**
- [ ] Parse u8, u16, u32, u64 correctly
- [ ] Little-endian byte order respected
- [ ] Offset tracking accurate
- [ ] Bounds checking prevents overrun
- [ ] String parsing with length prefix
- [ ] Array parsing with count prefix

**Test Cases:**
```kotlin
class BinaryParserTest {
    // F046-TC-01: Parse u32 little-endian
    fun testParseU32LittleEndian() { }
    
    // F046-TC-02: Parse u64
    fun testParseU64() { }
    
    // F046-TC-03: Offset advances correctly
    fun testOffsetTracking() { }
    
    // F046-TC-04: Bounds checking
    fun testBoundsCheckingPreventsOverrun() { }
    
    // F046-TC-05: String parsing with length
    fun testParseStringWithLength() { }
    
    // F046-TC-06: Array parsing
    fun testParseArray() { }
}
```

---

## Phase 2: Missing Code (P2 - Next Priority, 27 items)

### F047-P2-Code: Error Recovery Patterns
**Category:** Architecture → Error Handling  
**Status:** ❌ MISSING CODE  
**Est. LOC:** 200  

**Acceptance Criteria:**
- [ ] Try/catch blocks cover all repository calls
- [ ] Network errors logged but don't crash app
- [ ] User-facing error messages in UI
- [ ] Retry mechanism for transient errors
- [ ] Timeout handling (no infinite waits)

**Implementation Location:**
- `app/src/main/kotlin/com/deproof/data/repository/*.kt`
- Add error recovery in all use cases

---

### F048-P2-Code: Null Safety Annotations
**Category:** Architecture → Type Safety  
**Status:** ❌ MISSING CODE  
**Est. LOC:** 150  

**Acceptance Criteria:**
- [ ] @Nullable annotations on optional fields
- [ ] @NonNull on required parameters
- [ ] Suppress linter warnings with justification
- [ ] Nullability guards in critical paths

**Implementation Location:**
- Add androidx.annotation:annotation dependency
- Annotate all model classes and functions

---

### F049-P2-Code: Timber Logging Integration
**Category:** Architecture → Logging  
**Status:** ❌ MISSING CODE  
**Est. LOC:** 100  

**Acceptance Criteria:**
- [ ] Timber configured in Application.onCreate()
- [ ] Debug tree for local development
- [ ] Release tree for production
- [ ] Log levels: VERBOSE, DEBUG, INFO, WARN, ERROR
- [ ] No sensitive data logged (no keys, addresses, amounts)

---

### F050-P2-Code: Deep Linking Support
**Category:** Navigation → Deep Links  
**Status:** ❌ MISSING CODE  
**Est. LOC:** 80  

**Acceptance Criteria:**
- [ ] Intent filters in AndroidManifest
- [ ] Navigation graph deep link definitions
- [ ] Passing data through deep links
- [ ] Test deep link from external app

---

### F051-P2-Code: App Shortcuts
**Category:** UI → Shortcuts  
**Status:** ❌ MISSING CODE  
**Est. LOC:** 120  

**Acceptance Criteria:**
- [ ] Shortcuts XML resource file
- [ ] "Review Transaction" quick action
- [ ] "View Receipts" quick action
- [ ] Icon resources for shortcuts
- [ ] Targeting API 30+ (dynamic shortcuts optional)

---

### F052-P2-Code: Proguard Configuration
**Category:** Build → Obfuscation  
**Status:** ❌ MISSING CODE  
**Est. LOC:** 50  

**Acceptance Criteria:**
- [ ] proguard-rules.pro configured
- [ ] Keep rules for API models
- [ ] Keep rules for Compose
- [ ] Keep rules for Room entities
- [ ] Test release build doesn't crash

---

### F053-P2-Code: Release Build Signing
**Category:** Build → Signing  
**Status:** ❌ MISSING CODE  
**Est. LOC:** 60  

**Acceptance Criteria:**
- [ ] Signing config in build.gradle
- [ ] Keystore path configurable
- [ ] Release APK signed and aligned
- [ ] Signature verification possible

---

### F054-P2-Code: Build Variant Configuration
**Category:** Build → Variants  
**Status:** ❌ MISSING CODE  
**Est. LOC:** 100  

**Acceptance Criteria:**
- [ ] Debug variant (fast build, unoptimized)
- [ ] Release variant (optimized, obfuscated)
- [ ] Testnet variant (different RPC endpoint)
- [ ] BuildConfig.DEBUG flag available
- [ ] Variant-specific resources (optional)

---

## Phase 3: Device Validation (P3 - Device/Emulator Only, 45 items)

### F055-P3-Device: Screen Navigation E2E
**Category:** Device Testing → Navigation  
**Status:** ❌ NOT_RUN - Requires Device  

**Acceptance Criteria:**
- [ ] App launches to Now screen
- [ ] Tab navigation works (5 screens accessible)
- [ ] Back button navigates correctly
- [ ] State preserved across navigation

**Evidence Needed:**
- Video recording of full navigation flow
- Device: Android 8.0+ emulator or physical device

---

### F056-P3-Device: Now Screen UI Rendering
**Category:** Device Testing → Now Screen  
**Status:** ❌ NOT_RUN - Requires Device  

**Acceptance Criteria:**
- [ ] Wallet card displays
- [ ] Balance card displays
- [ ] Transaction list renders
- [ ] Touch interactions work
- [ ] Responsive layout (portrait/landscape)

---

### F057-P3-Device: Review Screen Transaction Flow
**Category:** Device Testing → Review Screen  
**Status:** ❌ NOT_RUN - Requires Device  

**Acceptance Criteria:**
- [ ] Input field accepts instruction data
- [ ] Decode button processes instruction
- [ ] Verdict displays correctly
- [ ] Message binding hash shows
- [ ] Approve/Reject buttons work

---

## Phase 4: RPC Integration (P4 - External Services, 70 items)

### F058-P4-RPC: Balance Query Integration
**Category:** Integration → Solana RPC  
**Status:** 🚫 BLOCKED - Requires RPC  

**Acceptance Criteria:**
- [ ] Query devnet SOL balance
- [ ] Query testnet SPL token balance
- [ ] Parse RPC response correctly
- [ ] Handle network errors
- [ ] Timeout after 10 seconds

---

### F059-P4-RPC: Wallet Connection (MWA)
**Category:** Integration → Mobile Wallet Adapter  
**Status:** 🚫 BLOCKED - Requires MWA  

**Acceptance Criteria:**
- [ ] Phantom wallet connection
- [ ] Backpack wallet connection
- [ ] Sign transaction request
- [ ] Handle wallet rejection
- [ ] Session management

---

## Test Coverage Tracking

### Current Status
- **Unit Tests Written:** 0/48
- **Code Coverage:** 0%
- **Device Tests Run:** 0/45
- **Integration Tests Run:** 0/70

### Target Coverage
| Category | Target | Current |
|----------|--------|---------|
| Formatters | 90% | 0% |
| Validators | 85% | 0% |
| Crypto Utils | 95% | 0% |
| Models | 80% | 0% |
| ViewModels | 70% | 0% |
| **Overall** | **70%** | **0%** |

---

## Test File Structure

```
app/src/test/
  kotlin/
    com/deproof/
      util/
        FormattersTest.kt          (F042)
        ValidatorsTest.kt          (F043)
      crypto/
        InstructionDecoderTest.kt  (F040)
        Base58Test.kt              (F044)
        MessageBindingTest.kt      (F045)
        BinaryParserTest.kt        (F046)
      domain/
        model/
          VerdictTest.kt           (F041)
        usecase/
          GetBalanceUseCaseTest.kt
          CreateReceiptUseCaseTest.kt
      data/
        repository/
          ReceiptRepositoryTest.kt
          RpcRepositoryTest.kt (mock)
      presentation/
        viewmodel/
          NowViewModelTest.kt
          ReviewViewModelTest.kt
          ReceiptsViewModelTest.kt
```

---

## Next Actions

### Immediate (This Session)
1. ✅ Create requirements registry (F040-F054)
2. ⏳ Begin F040 (InstructionDecoderTest.kt)
3. ⏳ Begin F041 (VerdictTest.kt)
4. ⏳ Complete F042-F046 (Formatter, Validator, Crypto)

### Before Device Testing
1. Run all Phase 1 tests locally
2. Achieve ≥70% overall code coverage
3. Fix any failing tests
4. Document test results in TEST_RESULTS.md

### Before Production
1. Complete Phase 2 (27 missing code items)
2. Run Phase 3 (45 device tests on emulator)
3. Run Phase 4 (70 integration tests with testnet)
4. Security audit (Phase 4)

---

**Registry Version:** 1.0  
**Last Updated:** 2026-10-06  
**Source Lock:** Commit 5f61ea4
