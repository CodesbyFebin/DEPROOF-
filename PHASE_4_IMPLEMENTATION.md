# Phase 4 Implementation: Node Agent Custody Commands - Revocation & Session Management

**Date:** October 7, 2026  
**Phase Duration:** 2 weeks  
**Target Branch:** `phase-2b-p4` (Android changes)

---

## Overview

Phase 4 implements cryptographically-signed session revocation, scoped action delegation, and immutable revocation logging for the DeProof node agent. This enables fine-grained custody control with explicit session invalidation, cross-cluster protection, and comprehensive audit trails.

**Key Deliverables:**
- Go node agent revocation system with cryptographic signatures
- Scoped action delegation with runtime constraints
- Immutable append-only revocation logging
- Android revocation polling and UI integration
- Comprehensive test coverage (Go + Android)

---

## Architecture

### 1. Go Node Agent Components

#### 1.1 Revocation Model (`node-agent/internal/consent/revocation.go`)

**Types:**
- `RevocationReason` enum: EXPLICIT, COMPROMISED, SCOPE_VIOLATION, POLICY_CHANGE, TOKEN_EXPIRED
- `SessionRevocation` struct: Immutable revocation record with Ed25519 signature
- `RevocationLog` struct: In-memory revocation log with signature generation

**Key Methods:**
```go
func (rl *RevocationLog) Revoke(sessionID, reason, revokedBy) (*SessionRevocation, error)
func (rl *RevocationLog) Get(sessionID) *SessionRevocation
func (rl *RevocationLog) IsRevoked(sessionID) bool
func (rev *SessionRevocation) Verify(publicKey) error
```

**Security Properties:**
- Revocations are immutable (append-only)
- Every revocation is signed with node's Ed25519 private key
- Canonical message format: `sessionId|reason|timestamp|revokedBy`
- Timestamp from OS (not input) prevents manipulation
- Signature verification prevents tampering

#### 1.2 Scoped Action Delegation (`node-agent/internal/consent/scope.go`)

**Types:**
- `ActionType` enum: TRANSFER, STAKE, UNSTAKE, CLAIM, SWAP, READ_ONLY, PROOF_SUBMIT, OPERATION_POLL
- `ActionConstraints` struct: Runtime limits for delegated actions
- `DelegatedActionScope` struct: Defines allowed actions and constraints for a session

**Constraints:**
- `MaxTransactionSize`: 1280 bytes (standard Solana limit)
- `MaxInstructionCount`: 4 instructions per transaction
- `TokenTransferMaxAmount`: Raw amount as string (prevent floating-point errors)
- `StakingPoolWhitelist`: Explicitly allowed staking pools
- `ClusterRestriction`: "devnet", "testnet", or "mainnet"
- `OperationTimeoutSec`: Session operation timeout
- `MaxConcurrentOps`: Concurrent operation limit
- `RateLimitPerMin`: Operations per minute

**Cross-Cluster Protection:**
```go
func (s *DelegatedActionScope) ClusterRestrictionViolation(targetCluster) error
// Prevents accidental mainnet operations by enforcing cluster boundaries
```

**Key Methods:**
```go
func (s *DelegatedActionScope) ValidateAction(action, txSize, instructionCount) error
func (s *DelegatedActionScope) TokenTransferValidation(amount, destination) error
func (s *DelegatedActionScope) StakingActionValidation(poolId, action) error
```

#### 1.3 Session Validator (`node-agent/internal/consent/validator.go`)

**Type:**
- `SessionValidator` struct: Validates commands against scope and revocation state

**Key Methods:**
```go
func (sv *SessionValidator) ValidateSessionExists(sessionID) error
func (sv *SessionValidator) ValidateAction(sessionID, action, txSize, instructionCount) error
func (sv *SessionValidator) ValidateClusterAction(sessionID, cluster) error
func (sv *SessionValidator) ValidateTokenTransfer(sessionID, amount, destination) error
func (sv *SessionValidator) RevokeSession(sessionID, reason, revokedBy) (*SessionRevocation, error)
```

**Validation Flow:**
1. Check if session is revoked (explicit reason if yes)
2. Check scope exists
3. Validate action is in allowed set
4. Check constraints (size, instructions, amount, cluster)
5. Enforce rate limiting

#### 1.4 Immutable Revocation Log (`node-agent/internal/log/revocation_log.go`)

**Type:**
- `ImmutableRevocationLog` struct: Append-only file-based revocation store
- `RevocationLogEntry` struct: Single immutable log entry

**Durability Guarantees:**
- File permissions: 0600 (owner read/write only)
- One-entry-per-line JSON format
- Immediate fsync after each append
- In-memory index for fast lookups
- No update/delete operations (append-only)

**Key Methods:**
```go
func (irl *ImmutableRevocationLog) AppendRevocation(...) error
func (irl *ImmutableRevocationLog) GetBySessionID(sessionID) *RevocationLogEntry
func (irl *ImmutableRevocationLog) IsRevoked(sessionID) bool
func (irl *ImmutableRevocationLog) Verify() error // Permissions check
```

### 2. Android Components

#### 2.1 Revocation Poller (`app/src/main/kotlin/com/deproof/data/RevocationPoller.kt`)

**Type:**
- `RevocationPoller` class: Polls node agent's revocation log and syncs to app state
- `SessionRevocation` data class: Represents a revocation event

**Features:**
- Background polling thread with configurable interval (default: 5 seconds)
- Reads from node agent's immutable log file
- Syncs new revocations to DataStore
- JSON parsing (no external dependencies)
- Clears wallet sessions on revocation
- Flows for subscription-based updates

**Integration Points:**
1. **DataStore Sync:** Updates DataStore when revocations detected
2. **Wallet Session Clearing:** Invalidates local session if revoked
3. **UI Notification:** Flows to composables for immediate display
4. **Rate Limiting:** Efficient batch processing

#### 2.2 ReviewScreen Revocation Support (`app/src/main/kotlin/com/deproof/presentation/ui/screens/ReviewScreenRevocationSupport.kt`)

**Types:**
- `RevocationState` sealed class: None or Revoked with reason/timestamp
- UI components for displaying revocation warnings

**Components:**
- `RevocationBanner()`: Warning card with revocation details
- `RevocationWarningCard()`: Styled warning with icon and reason
- `ReviewScreenWithRevocationHandling()`: Composite screen with revocation state
- `formatRevocationReason()`: User-friendly reason formatting
- `observeRevocationChanges()`: Hook for lifecycle-aware polling

**UI Behavior:**
- Revocation reasons displayed in user-friendly format
- Warning icon and red styling
- "No further operations" message
- Suggests restart/new connection
- Content disabled when revoked

#### 2.3 Android Tests (`app/src/test/java/com/deproof/data/RevocationPollerTest.kt`)

**Test Coverage:**
- JSON parsing (single/multiple entries, invalid data)
- Log file reading and filtering
- Revocation state tracking
- Session revocation flows
- Multiple concurrent revocations
- Polling with missing file
- Reason mapping

---

## Security Properties

### Revocation Signature Verification
```
Message: sessionId|reason|timestamp|revokedBy
Signature: Ed25519(Message, nodePrivateKey)
Verification: ed25519.Verify(Message, Signature, nodePublicKey)
```

### Immutable Log Guarantees
1. **Append-Only:** Never update/delete entries
2. **Durable:** fsync() after each write
3. **Atomic:** Single entry per line (no partial writes)
4. **Authenticated:** Every entry signed
5. **Protected:** File permissions 0600

### Cross-Cluster Protection
```go
// Prevent accidental mainnet operations
if scope.ClusterRestriction != targetCluster {
    return fmt.Errorf("cluster violation")
}
```

### Rate Limiting
```go
// Enforce operations-per-minute
if currentRequestCount > rateLimitPerMin {
    return fmt.Errorf("rate limit exceeded")
}
```

---

## Implementation Details

### Revocation Reasons

| Reason | Cause | Recovery |
|--------|-------|----------|
| EXPLICIT | User/admin revoked session | New session required |
| COMPROMISED | Security incident detected | Security review needed |
| SCOPE_VIOLATION | Attempted operation outside scope | Restart with new scope |
| POLICY_CHANGE | Node policy updated | New session matches new policy |
| TOKEN_EXPIRED | Session authorization expired | Reauthentication required |

### Constraint Validation Order

1. **Existence:** Does session exist?
2. **Revocation:** Is session revoked?
3. **Expiration:** Has scope expired?
4. **Action Permission:** Is action in allowed set?
5. **Size Limits:** Transaction size within bounds?
6. **Instruction Count:** Instruction count within bounds?
7. **Amount Limits:** Transfer amount within bounds?
8. **Whitelist:** Destination/pool in whitelist?
9. **Cluster:** Correct cluster for operation?
10. **Rate Limit:** Operations within rate limit?

### JSON Format

**Revocation Log Entry:**
```json
{
  "sessionId": "sess-123",
  "reason": "EXPLICIT",
  "timestamp": 1696714200000,
  "evidence": "deadbeef...",
  "revokedBy": "admin@example.com",
  "recordedAt": 1696714200500
}
```

**Android DataStore:**
```
latest_revocation_<sessionId> = "EXPLICIT@1696714200000"
known_revocations = "sess-1,sess-2,sess-3"
last_sync_time = "1696714200500"
```

---

## Testing Strategy

### Go Unit Tests (`node-agent/pkg/consent/consent_test.go`)

**Coverage:**
- Revocation creation and verification
- Signature generation and verification
- Scope validation
- Cross-cluster protection
- Token transfer constraints
- Staking operation validation
- Session validator workflows
- Rate limiting enforcement

**Test Functions:**
- `Test_RevocationLog_Revoke` - Revocation creation
- `Test_RevocationLog_IsRevoked` - Status checking
- `Test_RevocationSignatureVerification` - Signature validation
- `Test_DelegatedActionScope_IsExpired` - Expiration checking
- `Test_DelegatedActionScope_ValidateAction` - Constraint validation
- `Test_SessionValidator_CrossClusterProtection` - Mainnet safety
- `Test_RateLimiting` - Rate limit enforcement

### Android Tests (`app/src/test/java/com/deproof/data/RevocationPollerTest.kt`)

**Coverage:**
- JSON parsing (valid/invalid entries)
- Log file reading
- Revocation state tracking
- Session revocation flows
- Multiple concurrent revocations
- Polling without file
- Reason mapping

**Test Functions:**
- `testParseRevocationLine` - JSON parsing
- `testParseMultipleRevocations` - Batch parsing
- `testSessionRevocationFlow` - End-to-end flow
- `testMultipleSessionRevocations` - Concurrent revocations
- `testNonRevockedSession` - Negative cases

---

## Integration Points

### With Existing Phase 3A (Solana RPC)
- Revocation validator integrated before transaction submission
- Cluster restriction enforced on RPC endpoint selection
- Proof submission scoped to allowed sessions

### With Android Wallet Adapter
- Session scope tied to wallet connection
- Wallet selection restricted to allowed scopes
- Transaction signing blocked on revocation

### With Node Agent
- Agent checks revocation before operation execution
- Failed validation returns explicit error reason
- Revocations logged to immutable file

---

## Success Criteria

✅ Node agent can revoke individual sessions with reason  
✅ Revocations are immutable and cryptographically signed  
✅ Android app detects and responds to revocations  
✅ Session scope constraints are enforced  
✅ Cross-cluster protection prevents mainnet mistakes  
✅ All Go tests pass  
✅ All Android tests pass  
✅ Revocation reason displayed in ReviewScreen  
✅ Rate limiting enforced  
✅ Immutable log verified on startup  

---

## Deployment

### Go Node Agent
- Copy `node-agent/internal/consent/*.go` files
- Copy `node-agent/internal/log/*.go` files
- Run tests: `go test ./pkg/consent -v`
- Integrate validator into existing agent command handler

### Android App
- Merge Android files into existing project
- Add RevocationPoller to app initialization
- Wire ReviewScreen with revocation handling
- Update MainActivity to start polling on app launch
- Run tests: `./gradlew test`
- Build and test APK: `./gradlew build`

### Branch Management
- Develop on `phase-2b-p4`
- Test integration with existing Phase 3A code
- Create PR when complete
- Coordinate node-agent changes with main team

---

## Known Limitations

1. **File System Dependence:** Revocation log requires access to shared node-agent directory
2. **Polling Latency:** Revocation detection depends on polling interval (default 5s)
3. **No Network Push:** Android doesn't receive immediate push notifications
4. **Manual Parsing:** Simple JSON parser to avoid external dependencies
5. **No Signature Verification on Android:** Trust node agent's signatures (could add verification)

---

## Future Enhancements

1. **Push Notifications:** Real-time revocation alerts via FCM
2. **Signature Verification:** Android verifies node signatures
3. **Persistent Revocation Log:** Android stores seen revocations for audit
4. **Scheduled Re-Auth:** Periodic re-authentication to refresh scope
5. **Delegation Chains:** Multi-level delegation with cascading revocation
6. **Hardware Signing:** Use Android Keystore for revocation signatures

---

## Testing Checklist

- [ ] Go revocation tests pass
- [ ] Go scope validation tests pass
- [ ] Go validator tests pass
- [ ] Android poller tests pass
- [ ] Android builds without errors
- [ ] ReviewScreen displays revocation reason
- [ ] Manual testing: Revoke session via node agent
- [ ] Manual testing: Verify Android detects revocation
- [ ] Manual testing: Verify wallet session cleared on revocation
- [ ] Manual testing: Verify cross-cluster protection
- [ ] Integration testing with Phase 3A Solana flow
- [ ] APK built and signed successfully

---

**Status:** ✅ **Phase 4 READY FOR IMPLEMENTATION**

**Next Phase (Phase 5):** Advanced consent patterns, delegation chains, and cross-chain custody models.

---

*Document: PHASE_4_IMPLEMENTATION.md*  
*Author: Claude Code*  
*Version: 1.0*  
*Last Updated: 2026-10-07*
