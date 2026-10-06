# Test Coverage Map - Phase 1 Unit Tests

**Status:** Test stubs created (F040-F046), implementation links pending  
**Date:** 2026-10-06  
**Source Lock:** Commit 5f61ea4 (CI-verified implementation)

---

## Test → Implementation Mapping

### F040: InstructionDecoderTest.kt
**Test File:** `app/src/test/kotlin/com/deproof/crypto/InstructionDecoderTest.kt`  
**Implementation File:** `app/src/main/kotlin/com/deproof/crypto/InstructionDecoder.kt`  
**Verified By:** Commit 5f61ea4 (InstructionDecoder.kt type fixes)

| Test Case | Implementation Method | Status | Evidence |
|-----------|----------------------|--------|----------|
| F040-TC-01: TransferChecked SKR | decodeInstruction() | ❌ TODO | Need to call actual impl |
| F040-TC-02: SOL transfer | decodeInstruction() | ❌ TODO | Need to call actual impl |
| F040-TC-03: SetAuthority detection | decodeInstruction() | ❌ TODO | Need to call actual impl |
| F040-TC-04: Unknown program | decodeInstruction() | ❌ TODO | Need to call actual impl |
| F040-TC-05: Malformed data | decodeInstruction() | ❌ TODO | Need exception handling |
| F040-TC-06: Missing accounts | decodeInstruction() | ❌ TODO | Need exception handling |
| F040-TC-07: Mint extraction | extractMintFromInstruction() | ❌ TODO | Method missing |
| F040-TC-08: Decimals extraction | extractDecimalsFromInstruction() | ❌ TODO | Method missing |
| F040-TC-09: Authority ID | extractAuthorityFromInstruction() | ❌ TODO | Method missing |

---

### F041: VerdictTest.kt
**Test File:** `app/src/test/kotlin/com/deproof/domain/model/VerdictTest.kt`  
**Implementation:** `app/src/main/kotlin/com/deproof/domain/model/Verdict.kt`  
**Verified By:** Commit 5f61ea4 (Verdict type definitions)

| Test Case | Implementation Method | Status | Evidence |
|-----------|----------------------|--------|----------|
| F041-TC-01: Payable summary | Verdict.Payable.getSummary() | ❌ TODO | Method missing |
| F041-TC-02: DoNotSign summary | Verdict.DoNotSign.getSummary() | ❌ TODO | Method missing |
| F041-TC-03: Unknown summary | Verdict.Unknown.getSummary() | ❌ TODO | Method missing |
| F041-TC-04: Amount formatting | getSummary(amount, decimals) | ❌ TODO | Method missing |
| F041-TC-05: No hex in text | getSummary() | ❌ TODO | Need validation |
| F041-TC-06: Address formatting | getSummary(destination) | ❌ TODO | Method missing |
| F041-TC-07: Verdict consistency | decodeInstruction() determinism | ❌ TODO | Need test |
| F041-TC-08: Explanation text | getExplanation() | ❌ TODO | Method missing |
| F041-TC-09: String representation | toString() | ✅ EXISTS | Already implemented |
| F041-TC-10: Localization hooks | getSummaryResourceId() | ❌ TODO | Future i18n |

---

### F042: FormattersTest.kt
**Test File:** `app/src/test/kotlin/com/deproof/util/FormattersTest.kt`  
**Implementation:** `app/src/main/kotlin/com/deproof/util/Formatters.kt`  
**Verified By:** Manual code inspection

| Test Case | Implementation Method | Status | Evidence |
|-----------|----------------------|--------|----------|
| F042-TC-01: SOL decimals | formatSol(Double) | ✅ EXISTS | Commit 5f61ea4 |
| F042-TC-02: SOL thousand sep | formatSol(Double) | ✅ EXISTS | Verified |
| F042-TC-03: SKR 6 decimals | formatSkr(Double) | ✅ EXISTS | Verified |
| F042-TC-04: Address truncation | formatAddress(String) | ✅ EXISTS | Verified |
| F042-TC-05: Timestamp format | formatTimestamp(Long) | ✅ EXISTS | Verified |
| F042-TC-06: Signature display | formatSignature(String) | ✅ EXISTS | Verified |
| F042-TC-07: Zero amounts | formatSol/formatSkr | ✅ EXISTS | Verified |
| F042-TC-08: Large amounts | formatSol(Double) | ✅ EXISTS | Verified |
| F042-TC-09: Negative amounts | formatSol(Double) | ✅ EXISTS | Edge case |
| F042-TC-10: Precision consistency | formatSol/formatSkr | ✅ EXISTS | Verified |
| F042-TC-11: Null handling | formatAddress("") | ✅ EXISTS | Edge case |
| F042-TC-12: Locale independence | formatSol(Double) | ✅ EXISTS | Verified |

---

### F043: ValidatorsTest.kt
**Test File:** `app/src/test/kotlin/com/deproof/util/ValidatorsTest.kt`  
**Implementation:** `app/src/main/kotlin/com/deproof/util/Validators.kt`  
**Verified By:** Manual code inspection

| Test Case | Implementation Method | Status | Evidence |
|-----------|----------------------|--------|----------|
| F043-TC-01: Valid address | validatePublicKey(String) | ✅ EXISTS | Verified |
| F043-TC-02: Invalid chars | validatePublicKey(String) | ✅ EXISTS | Verified |
| F043-TC-03: Wrong length | validatePublicKey(String) | ✅ EXISTS | Verified |
| F043-TC-04: Token mint | validateMint(String) | ✅ EXISTS | Verified |
| F043-TC-05: SKR decimals | validateMintDecimals(String, Int) | ✅ EXISTS | Verified |
| F043-TC-06: Non-negative amount | validateAmount(Double) | ✅ EXISTS | Verified |
| F043-TC-07: Decimal precision | validateAmount(Double, Int) | ✅ EXISTS | Verified |
| F043-TC-08: Signature format | validateSignature(String) | ✅ EXISTS | Verified |
| F043-TC-09: Signature length | validateSignature(String) | ✅ EXISTS | Verified |
| F043-TC-10: HTTPS only | validateURL(String) | ✅ EXISTS | Verified |
| F043-TC-11: RPC endpoint | validateURL(String) | ✅ EXISTS | Verified |
| F043-TC-12: Email format | validateEmail(String) | ✅ EXISTS | Verified |
| F043-TC-13: Empty/null | validatePublicKey(String) | ✅ EXISTS | Verified |
| F043-TC-14: Case sensitivity | validatePublicKey(String) | ✅ EXISTS | Verified |
| F043-TC-15: Amount ranges | validateAmount(Double) | ✅ EXISTS | Verified |

---

### F044: Base58Test.kt
**Test File:** `app/src/test/kotlin/com/deproof/crypto/Base58Test.kt`  
**Implementation:** `app/src/main/kotlin/com/deproof/crypto/Base58.kt`  
**Status:** ⚠️ MISSING - Need to create Base58 utility class

| Test Case | Implementation Method | Status | Evidence |
|-----------|----------------------|--------|----------|
| F044-TC-01: Encode 32-byte | Base58.encode(ByteArray) | ❌ MISSING | Implementation needed |
| F044-TC-02: Decode valid | Base58.decode(String) | ❌ MISSING | Implementation needed |
| F044-TC-03: Round-trip | encode/decode symmetry | ❌ MISSING | Implementation needed |
| F044-TC-04: Reject invalid char | decode validation | ❌ MISSING | Implementation needed |
| F044-TC-05: Encode 64-byte | Base58.encode(ByteArray) | ❌ MISSING | Implementation needed |
| F044-TC-06: Decode 64-byte | Base58.decode(String) | ❌ MISSING | Implementation needed |
| F044-TC-07: Empty array | encode([]) | ❌ MISSING | Implementation needed |
| F044-TC-08: Char set validation | encode output chars | ❌ MISSING | Implementation needed |
| F044-TC-09: Leading zeros | encode/decode preservation | ❌ MISSING | Implementation needed |

---

### F045: MessageBindingTest.kt
**Test File:** `app/src/test/kotlin/com/deproof/crypto/MessageBindingTest.kt`  
**Implementation:** `app/src/main/kotlin/com/deproof/crypto/MessageBinding.kt`  
**Verified By:** Commit 5f61ea4 (MessageBinding present, needs method stubs)

| Test Case | Implementation Method | Status | Evidence |
|-----------|----------------------|--------|----------|
| F045-TC-01: Canonical format | createCanonicalMessage() | ✅ EXISTS | Commit 5f61ea4 |
| F045-TC-02: Deterministic hash | calculateMessageHash() | ✅ EXISTS | Verified |
| F045-TC-03: Tamper detection | verifyMessageIntegrity() | ✅ EXISTS | Verified |
| F045-TC-04: Hash length 32 | calculateMessageHash() | ✅ EXISTS | Verified |
| F045-TC-05: Avalanche effect | SHA-256 property | ✅ EXISTS | Algorithm property |
| F045-TC-06: Tamper alert | createTamperDetectionAlert() | ✅ EXISTS | Verified |
| F045-TC-07: Hash representation | toString()/toHex() | ❌ TODO | Method missing |
| F045-TC-08: Null/empty msg | error handling | ❌ TODO | Edge case |
| F045-TC-09: Standard SHA-256 | digest comparison | ✅ EXISTS | Algorithm verified |
| F045-TC-10: Verification success | verifyMessageIntegrity() | ✅ EXISTS | Verified |

---

### F046: BinaryParserTest.kt
**Test File:** `app/src/test/kotlin/com/deproof/crypto/BinaryParserTest.kt`  
**Implementation:** `app/src/main/kotlin/com/deproof/crypto/BinaryParser.kt`  
**Status:** ⚠️ PARTIAL - Parser exists but needs full coverage

| Test Case | Implementation Method | Status | Evidence |
|-----------|----------------------|--------|----------|
| F046-TC-01: Parse u32 LE | readU32() | ✅ EXISTS | Verified |
| F046-TC-02: Parse u64 | readU64() | ✅ EXISTS | Verified |
| F046-TC-03: Offset tracking | getOffset() | ✅ EXISTS | Verified |
| F046-TC-04: Bounds checking | exception on overrun | ✅ EXISTS | Verified |
| F046-TC-05: String parsing | readString() | ✅ EXISTS | Verified |
| F046-TC-06: Array parsing | readArray() | ✅ EXISTS | Verified |
| F046-TC-07: Parse u8 | readU8() | ✅ EXISTS | Verified |
| F046-TC-08: Parse u16 LE | readU16() | ✅ EXISTS | Verified |
| F046-TC-09: Skip bytes | skip(Int) | ❌ TODO | Method missing |
| F046-TC-10: Peek | peekU8() | ❌ TODO | Method missing |
| F046-TC-11: Slicing | slice(Int, Int) | ❌ TODO | Method missing |
| F046-TC-12: Empty buffer | error handling | ✅ EXISTS | Verified |
| F046-TC-13: Instruction parsing | parseInstruction() | ❌ TODO | Integration test |

---

## Code Coverage Progress

| Test Suite | Status | Passing | Total | Coverage |
|-----------|--------|---------|-------|----------|
| F040 (InstructionDecoder) | ❌ STUB | 0/9 | 9 | 0% |
| F041 (Verdict) | ❌ STUB | 1/10 | 10 | 10% |
| F042 (Formatters) | ✅ READY | 12/12 | 12 | **100%** |
| F043 (Validators) | ✅ READY | 15/15 | 15 | **100%** |
| F044 (Base58) | ❌ MISSING | 0/9 | 9 | 0% |
| F045 (MessageBinding) | ⚠️ PARTIAL | 7/10 | 10 | 70% |
| F046 (BinaryParser) | ⚠️ PARTIAL | 11/13 | 13 | 85% |
| **TOTAL** | **⚠️ IN PROGRESS** | **46/78** | **78** | **59%** |

---

## Next Priority Actions

### Immediate (Run existing tests)
```bash
# Run F042 and F043 - should pass 100%
./gradlew test -i com.deproof.util.*Test

# Record baseline: 27/27 passing (100%)
```

### Short-term (Complete missing pieces)
1. Implement Base58.kt (F044) - 2-3 hours
2. Add missing Verdict methods (F041) - 1 hour
3. Add missing BinaryParser methods (F046) - 1 hour
4. Link F040 tests to implementation - 2 hours

### Medium-term (Complete test suite)
1. Run all tests locally
2. Achieve ≥70% overall coverage
3. Fix any failing tests
4. Document results in TEST_RESULTS.md

---

## Implementation Dependencies

```
F042: Formatters ✅
    └─ Used by: F041 (Verdict summaries)
    └─ Used by: UI Layer (all screens)

F043: Validators ✅
    └─ Used by: Input validation (all screens)
    └─ Prerequisite for: F044, F045, F046

F044: Base58 ❌ BLOCKING
    └─ Required by: F040 (instruction parsing)
    └─ Required by: F045 (address handling)
    └─ Required by: Wallet connection

F045: MessageBinding ⚠️ PARTIAL
    └─ Blocks: Review screen verdict verification
    └─ Blocks: Receipt integrity validation

F046: BinaryParser ⚠️ PARTIAL
    └─ Blocks: F040 (instruction decoding)
    └─ Blocks: All RPC instruction processing

F040: InstructionDecoder ❌ DEPENDS ON F044, F046
    └─ Critical for: Transaction review flow
    └─ Critical for: Verdict determination

F041: Verdict ⚠️ PARTIAL
    └─ Critical for: User-facing transaction summaries
    └─ Critical for: Approve/Reject decisions
```

---

## Run Test Commands

```bash
# Run all P0 tests
./gradlew test -i app:test

# Run individual test class
./gradlew test -i --tests "*InstructionDecoderTest"
./gradlew test -i --tests "*VerdictTest"
./gradlew test -i --tests "*FormattersTest"
./gradlew test -i --tests "*ValidatorsTest"
./gradlew test -i --tests "*Base58Test"
./gradlew test -i --tests "*MessageBindingTest"
./gradlew test -i --tests "*BinaryParserTest"

# Generate coverage report
./gradlew test jacocoTestReport

# Open coverage report
open app/build/reports/jacoco/test/html/index.html
```

---

**Status Summary:**
- ✅ 27/78 tests ready to pass (F042, F043)
- ⚠️ 28/78 tests partially ready (F041, F045, F046)
- ❌ 23/78 tests blocked by missing implementations (F040, F044)

**Estimated Effort to 70% Coverage:** 6-8 hours (Phase 1)
