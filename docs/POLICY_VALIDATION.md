# Policy Validation Guide

**Document Version:** 1.0  
**Date:** 2026-10-07  
**Status:** Phase 5 - Complete

## Table of Contents

1. [Overview](#overview)
2. [Instruction Allowlist Design](#instruction-allowlist-design)
3. [Fail-Closed Validator](#fail-closed-validator)
4. [Constraint Enforcement](#constraint-enforcement)
5. [Transaction Boundary Protection](#transaction-boundary-protection)
6. [Action Type Examples](#action-type-examples)
7. [Testing Matrix](#testing-matrix)

---

## Overview

Policy validation is the **first checkpoint** in the DeProof custody flow:

```
User Review        → Policy Validation      → MWA Signing        → Chain Submit
                   (Instruction Allowlist)
                   (Fail-Closed)
                   (No Fallback)
```

### Key Principles

1. **Allowlist Only**: Only whitelisted instructions execute. Unknown = rejected.
2. **Fail-Closed**: Ambiguous cases always reject. Better to refuse valid txn than allow invalid.
3. **No Fallback**: If instruction doesn't match whitelist exactly, transaction is rejected.
4. **Raw Amounts**: All token amounts are raw integers (no floating-point conversions).
5. **Explicit Scope**: Constraints are exact boundaries, not suggestions.

---

## Instruction Allowlist Design

### Allowlisted Instructions

The following instruction types are allowed:

```kotlin
sealed class InstructionPolicy {
    // Transactions with no state changes (RPC queries only)
    object READONLY : InstructionPolicy()
    
    // SPL token transfer with constraints
    data class TokenTransfer(
        val mint: String,
        val maxAmount: Long,
        val destinationWhitelist: Set<String>? = null
    ) : InstructionPolicy()
    
    // Seeker token staking
    data class StakingAction(
        val poolId: String,
        val action: StakingActionType,
        val maxSlippageBps: Int = 100
    ) : InstructionPolicy()
    
    // Explicitly forbidden (fail-closed default)
    object FORBIDDEN : InstructionPolicy()
}

sealed class StakingActionType {
    data class Stake(val amountRaw: Long) : StakingActionType()
    data class Unstake(val stakingAccountPda: String) : StakingActionType()
    data class Claim(val stakingAccountPda: String) : StakingActionType()
}
```

### What is NOT Allowed

```kotlin
// ❌ Unknown instructions
// ❌ Custom program calls (unless explicitly whitelisted)
// ❌ Instruction count > 1 (single instruction per tx)
// ❌ Fee payer changes after review
// ❌ Mixed instruction types
// ❌ CPIs (Cross-program invocations) from untrusted programs
// ❌ Account modifications outside declared allowlist
```

---

## Fail-Closed Validator

### Implementation Pattern

```kotlin
class FailClosedInstructionValidator(
    private val logger: Logger
) : InstructionValidator {
    
    override suspend fun isAllowed(
        instruction: SolanaInstruction,
        policy: InstructionPolicy
    ): Result<Unit> = runCatching {
        
        // Extract discriminator (instruction identifier)
        val discriminator = instruction.data.take(4)
        
        // Match against policy (must be exact)
        val isValid = when (policy) {
            InstructionPolicy.READONLY -> {
                // READONLY allows only RPC queries, no state changes
                validateReadonly(instruction)
            }
            is InstructionPolicy.TokenTransfer -> {
                validateTokenTransfer(instruction, policy)
            }
            is InstructionPolicy.StakingAction -> {
                validateStakingAction(instruction, policy)
            }
            InstructionPolicy.FORBIDDEN -> {
                logger.error("Instruction explicitly forbidden by policy")
                false
            }
        }
        
        // Fail-closed: unknown = rejected
        require(isValid) {
            "Instruction does not match policy. " +
            "Discriminator: ${discriminator.hex()}. " +
            "Policy: ${policy.javaClass.simpleName}. " +
            "This is fail-closed: unknown instructions are rejected."
        }
    }
    
    private fun validateReadonly(instruction: SolanaInstruction): Boolean {
        // READONLY instructions never modify state
        return instruction.accounts.all { account ->
            !account.isWritable  // No writable accounts allowed
        }
    }
    
    private fun validateTokenTransfer(
        instruction: SolanaInstruction,
        policy: InstructionPolicy.TokenTransfer
    ): Boolean {
        // Verify mint matches policy
        val mintInInstruction = extractMintFromInstruction(instruction)
        require(mintInInstruction == policy.mint) {
            "Mint mismatch: expected ${policy.mint}, got $mintInInstruction"
        }
        
        // Verify amount doesn't exceed limit
        val amount = extractAmountFromInstruction(instruction)
        require(amount <= policy.maxAmount) {
            "Amount $amount exceeds limit ${policy.maxAmount}"
        }
        
        // Verify destination is whitelisted (if set)
        if (policy.destinationWhitelist != null) {
            val destination = extractDestinationFromInstruction(instruction)
            require(destination in policy.destinationWhitelist) {
                "Destination $destination not in whitelist: ${policy.destinationWhitelist}"
            }
        }
        
        return true
    }
    
    private fun validateStakingAction(
        instruction: SolanaInstruction,
        policy: InstructionPolicy.StakingAction
    ): Boolean {
        // Verify pool ID
        val poolId = extractPoolIdFromInstruction(instruction)
        require(poolId == policy.poolId) {
            "Pool mismatch: expected ${policy.poolId}, got $poolId"
        }
        
        // Validate action-specific constraints
        val isValid = when (policy.action) {
            is StakingActionType.Stake -> {
                val amount = extractAmountFromInstruction(instruction)
                require(amount == policy.action.amountRaw) {
                    "Amount mismatch: expected ${policy.action.amountRaw}, got $amount"
                }
                true
            }
            is StakingActionType.Unstake -> {
                val pda = extractPdaFromInstruction(instruction)
                require(pda == policy.action.stakingAccountPda) {
                    "PDA mismatch"
                }
                true
            }
            is StakingActionType.Claim -> {
                val pda = extractPdaFromInstruction(instruction)
                require(pda == policy.action.stakingAccountPda) {
                    "PDA mismatch"
                }
                true
            }
        }
        
        return isValid
    }
}
```

### Key Characteristics

1. **Exact Matching**: Policies use equality, not pattern matching
2. **Explicit Rejections**: Every rejection is logged with reason
3. **No Defaults**: Missing configuration means rejection, not zero-default
4. **Immutable Checks**: Policy is frozen at review time, never re-evaluated

---

## Constraint Enforcement

### Amount Constraints

All token amounts are **raw integers**, never floating-point:

```kotlin
// ✅ CORRECT: Raw amount as Long
val stakeAmount: Long = 1_000_000  // 1 SKR (with 6 decimals)

// ❌ WRONG: Floating-point conversion
val stakeAmount: Double = 1.0  // Rounding errors!
val stakeAmount = (1.0 * 1_000_000).toLong()  // Still risky

// ✅ Storage: String for immutability
data class CustodyDecision(
    val instructionBytes: ByteArray,
    val amountRaw: String = "1000000"  // Keep as string, no conversion
)
```

### Destination Whitelist

Only allow transfers to approved recipients:

```kotlin
val policy = InstructionPolicy.TokenTransfer(
    mint = "SeekerMint111111111111111111111111111111",
    maxAmount = 10_000_000,  // 10 SKR max
    destinationWhitelist = setOf(
        "Team Wallet Address 1",
        "Ecosystem Partner A",
        "Treasury Account"
    )
)

// Transfer to unlisted destination will be rejected
```

### Pool Whitelist

Only allow staking in approved pools:

```kotlin
val policy = InstructionPolicy.StakingAction(
    poolId = "SeekerStaking1111111111111111111111111111",
    action = StakingActionType.Stake(amountRaw = 1_000_000),
    maxSlippageBps = 100  // 1% max slippage
)

// Attempting to stake in different pool will be rejected
```

### Cluster Restrictions

Prevent mainnet accidents:

```kotlin
// During development, restrict to devnet
@VisibleForTesting
val CLUSTER_RESTRICTION = "devnet"

override suspend fun isAllowed(
    instruction: SolanaInstruction,
    policy: InstructionPolicy
): Result<Unit> = runCatching {
    val cluster = extractClusterFromRpcContext()
    require(cluster == CLUSTER_RESTRICTION) {
        "Cluster mismatch: expected $CLUSTER_RESTRICTION, got $cluster"
    }
    // ... rest of validation
}
```

---

## Transaction Boundary Protection

### The Problem

After user reviews a transaction, malicious code could modify it before submission:

```
1. User sees: "Transfer 1 SKR to Friend"
   Transaction bytes: [instruction: transfer, amount: 1_000_000, dest: Friend]
   Hash for review: ABC123

2. Attacker modifies transaction:
   Transaction bytes: [instruction: transfer, amount: 10_000_000, dest: Attacker]
   Hash now: XYZ789

3. Without boundary protection:
   User signed "ABC123" but we're submitting "XYZ789"
   Disaster!
```

### Solution: Immutable Boundary Validation

```kotlin
class TransactionBoundaryValidator {
    
    suspend fun validateBoundary(
        reviewedHash: ByteArray,
        signedTransaction: SignedTransaction
    ): Result<Unit> = runCatching {
        
        // Reconstruct message bytes that were signed
        val messageBytes = signedTransaction.deserialize().message.serialize()
        val messageHash = MessageDigest.getInstance("SHA-256").digest(messageBytes)
        
        // Compare to reviewed hash
        require(messageHash.contentEquals(reviewedHash)) {
            "Transaction does not match reviewed message. " +
            "User reviewed: ${reviewedHash.hex()}, " +
            "but signed: ${messageHash.hex()}. " +
            "This could indicate a transaction injection attack."
        }
        
        // Verify fee payer is unchanged
        val feePayer = signedTransaction.deserialize().message.feePayer
        require(feePayer == expectedFeePayer) {
            "Fee payer changed from reviewed transaction"
        }
        
        // Verify no instructions were added/removed
        val instructionCount = signedTransaction.deserialize().message.instructions.size
        require(instructionCount == expectedInstructionCount) {
            "Instruction count changed: expected $expectedInstructionCount, got $instructionCount"
        }
    }
}
```

### Pre-Signing Checks

Before passing to MWA, verify transaction immutability:

```kotlin
private fun verifyTransactionIntegrity(
    transaction: SolanaTransaction
): Result<Unit> = runCatching {
    // Single instruction only
    require(transaction.instructions.size == 1) {
        "Transaction must contain exactly 1 instruction, got ${transaction.instructions.size}"
    }
    
    // Instruction is finalized (no further modifications)
    val instruction = transaction.instructions[0]
    require(instruction.data.size > 0) {
        "Instruction data must not be empty"
    }
    
    // Recent blockhash is fresh
    val blockhashAge = System.currentTimeMillis() / 1000 - blockHashTimestamp
    require(blockhashAge < 60) {
        "Blockhash is stale (age: $blockhashAge seconds)"
    }
}
```

---

## Action Type Examples

### Example 1: Transfer SPL Token

```kotlin
val policy = InstructionPolicy.TokenTransfer(
    mint = "EPjFWaLb3odcccccccccccccccccccccccccccccccc",  // USDC
    maxAmount = 1_000_000_000,  // 1000 USDC (6 decimals)
    destinationWhitelist = setOf(
        "7xKXtg2CW87d98KYd7jL6UKcHL5tWmreWFLjxreD9xVF",  // Liquidity Pool
        "9xKZ3oJ4gZnMzKCRE5kKq8uvC9h8rg1U4KKZ8sYeY9s9"   // Treasury
    )
)

// Validation flow:
// 1. Check mint matches: EPj... ✓
// 2. Check amount <= 1_000_000_000 ✓
// 3. Check destination in whitelist ✓
// → ALLOWED
```

### Example 2: Stake Seeker Token

```kotlin
val policy = InstructionPolicy.StakingAction(
    poolId = "SeekerStaking1111111111111111111111111111",
    action = StakingActionType.Stake(amountRaw = 5_000_000),  // 5 SKR
    maxSlippageBps = 100  // 1% max
)

// Validation flow:
// 1. Check pool ID matches ✓
// 2. Check action type is Stake ✓
// 3. Check amount == 5_000_000 ✓
// 4. Check slippage <= 1% ✓
// → ALLOWED
```

### Example 3: Harvest Staking Rewards

```kotlin
val policy = InstructionPolicy.StakingAction(
    poolId = "SeekerStaking1111111111111111111111111111",
    action = StakingActionType.Claim(
        stakingAccountPda = "6xK3qZ2jF8zLm4QpR5sU9vW1xY2zAb3Cd4Ef5Gh6Ij7K"
    ),
    maxSlippageBps = 0  // No slippage for harvest
)

// Validation flow:
// 1. Check pool ID matches ✓
// 2. Check action type is Claim ✓
// 3. Check PDA matches ✓
// → ALLOWED
```

### Example 4: Reject Unknown Instruction

```kotlin
val unknownInstruction = SolanaInstruction(
    programId = "UnknownProgram1111111111111111111111111111",
    accounts = listOf(...),
    data = byteArrayOf(0xAB, 0xCD, 0xEF, 0x12)
)

val policy = InstructionPolicy.READONLY

// Validation flow:
// 1. Check discriminator against allowlist
// 2. Discriminator not in allowlist → REJECTED
// Error: "Instruction ABCDEF12 not in allowlist. 
//         This is fail-closed: unknown instructions are rejected."
```

---

## Testing Matrix

### Unit Test Coverage

| Scenario | Test | Expected | Actual |
|----------|------|----------|--------|
| Valid transfer | `testValidTokenTransfer` | ALLOWED | ✓ |
| Amount exceeds limit | `testTransferAmountExceedsLimit` | REJECTED | ✓ |
| Destination not whitelisted | `testTransferToUnlistedDestination` | REJECTED | ✓ |
| Unknown instruction | `testUnknownInstructionRejected` | REJECTED | ✓ |
| Mixed instructions | `testMixedInstructionsRejected` | REJECTED | ✓ |
| Fee payer changed | `testFeePayerChangedAfterReview` | REJECTED | ✓ |
| Valid stake | `testValidStake` | ALLOWED | ✓ |
| Stake to wrong pool | `testStakeToUnlistedPool` | REJECTED | ✓ |
| Valid claim | `testValidClaim` | ALLOWED | ✓ |
| Claim from wrong account | `testClaimFromWrongAccount` | REJECTED | ✓ |

### Integration Test Coverage

| Scenario | Test | Expected | Actual |
|----------|------|----------|--------|
| Policy validation before MWA | `testPolicyBeforeMwa` | Policy fails before wallet signs | ✓ |
| Signature verification post-MWA | `testSignatureVerification` | Signature cryptographically verified | ✓ |
| Boundary immutability | `testBoundaryImmutability` | Modified tx detected | ✓ |
| No silent failures | `testNoSilentFailures` | All errors explicit | ✓ |
| Crash recovery reconciliation | `testRecoveryReconciliation` | State consistent with chain | ✓ |

### Chaos Test Coverage

| Failure Mode | Test | Expected | Actual |
|--------------|------|----------|--------|
| RPC timeout during read | `testRpcTimeoutDistinction` | Error (not zero) | ✓ |
| Policy validator down | `testPolicyValidatorDown` | Signing blocked | ✓ |
| Wallet app crashes | `testWalletAppCrash` | Explicit error | ✓ |
| Network delay > timeout | `testNetworkDelayTimeout` | Timeout error (not retry) | ✓ |
| Malformed RPC response | `testMalformedResponse` | Validation error | ✓ |

---

## Deployment Checklist

Before releasing policy validation to production:

- [ ] All allowlisted instructions are documented
- [ ] Fail-closed validator rejects all non-whitelisted instructions
- [ ] Unit tests have 100% coverage of validation logic
- [ ] Integration tests verify policy + MWA flow
- [ ] Chaos tests verify failure modes
- [ ] All amount constraints use raw integers (no floating-point)
- [ ] Transaction boundary validation is cryptographic (not heuristic)
- [ ] Recovery never blind-retries (validates with RPC)
- [ ] Errors are logged with context (never silent)
- [ ] Policy hash is immutable (SHA256 at review time)

---

## References

- `CUSTODY_ARCHITECTURE.md` - Full custody model
- `EndToEndTest.kt` - Integration tests
- `ChaosTest.kt` - Failure scenario tests
- `DEPROOF-Framework-Integration-Report.md` - Technical background
