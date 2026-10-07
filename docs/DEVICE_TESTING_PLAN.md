# Device Testing Plan

**Document Version:** 1.0  
**Date:** 2026-10-07  
**Status:** Phase 5 - Complete

## Overview

Device testing verifies all phases work on real Android hardware with actual wallets and RPCs.

---

## Phase 1: Foundation (Unit Test Only)

**Device Test:** None required  
**Unit Tests:** 100% pass

---

## Phase 2: SKR Reading (Devnet Device Test)

### Setup
- Real Android device (or emulator)
- DeProof app installed
- Connected to Solana Devnet RPC
- Test SKR tokens on devnet wallet

### Test Case 2.1: Read SKR Balance
```
1. Open DeProof app
2. Navigate to Seeker Token tab
3. Connect wallet via MWA
4. Observe balance displayed (raw amount, no decimals)
5. Expected: Balance shown accurately
6. Verify: Amount matches RPC query
```

### Test Case 2.2: Multiple Staking Accounts
```
1. Create 3+ staking accounts on devnet
2. Open DeProof app
3. Navigate to Seeker Token tab
4. Expected: All staking accounts enumerated
5. Verify: Amounts, rewards, cooldown times all correct
```

### Test Case 2.3: Error Handling
```
1. Disconnect from internet (or proxy to kill RPC)
2. Try to read balance
3. Expected: Error displayed (not zero)
4. Verify: UI shows "Balance unavailable" (not "0 SKR")
```

---

## Phase 3: MWA Signing (Devnet Device Test)

### Setup
- Real Android device
- Solana wallet app installed (Backpack, Saga, etc.)
- Test devnet tokens in wallet
- DeProof connected to wallet via MWA

### Test Case 3.1: Policy Validation Before MWA
```
1. Configure policy to reject transfers > 1 token
2. Attempt transfer of 10 tokens
3. Expected: Policy dialog shows "Exceeds limit"
4. Verify: MWA NOT launched
5. Verify: Wallet app does not show signing prompt
```

### Test Case 3.2: Valid Transaction Signing
```
1. Attempt valid stake transaction (within policy)
2. Expected: MWA launches wallet app
3. User approves in wallet
4. Expected: Signature received and verified
5. Verify: Transaction included in local log
```

### Test Case 3.3: Signature Verification
```
1. (Simulated attack) Intercept and modify transaction
2. Attempt to use modified transaction
3. Expected: Signature verification fails
4. Verify: Error: "Signature does not match reviewed message"
```

---

## Phase 4: Node Agent Revocation (Integration Test)

### Setup
- Node agent running on local machine (or remote server)
- Android device connected to node agent
- Active session with delegated permissions

### Test Case 4.1: Explicit Revocation
```
1. Create active session (24-hour pairing)
2. From node agent: revoke(sessionId, "EXPLICIT")
3. Attempt operation on Android
4. Expected: Error: "Session revoked by administrator"
5. Verify: UI updated to show revoked state
```

### Test Case 4.2: Immediate Effect
```
1. Create active session
2. Verify operation succeeds
3. Revoke session
4. Immediately attempt another operation (same session)
5. Expected: Operation rejected instantly
6. Verify: No retry or grace period
```

---

## Phase 5: End-to-End (Devnet Device Test)

### Full Stake Flow
```
1. Open DeProof app
2. Read SKR balance (SKR Reader)
   ✓ Verify: Correct balance displayed
3. Initiate stake transaction
   ✓ Verify: Policy validation passes
4. Review transaction details
   ✓ Verify: All details correct
5. Approve via MWA
   ✓ Verify: Wallet app launches
   ✓ Verify: User confirms in wallet
   ✓ Verify: Signature received
6. Submit to RPC
   ✓ Verify: Transaction submitted
7. Monitor confirmation
   ✓ Verify: Status updates to CONFIRMED
8. Verify chain state
   ✓ Verify: Staked amount correct on-chain
```

### Crash Recovery Test
```
1. Initiate stake transaction
2. After MWA signature, force-kill app
3. Restart app
   ✓ Verify: "Complete pending transaction?" prompt
   ✓ Verify: Transaction submitted without re-signing
   ✓ Verify: Status updates correctly
4. Verify no double-submit
   ✓ Verify: Chain shows only 1 transaction
```

### Network Failure Test
```
1. Begin stake transaction
2. Kill network (airplane mode)
3. At point of RPC submit, restore network
4. Verify recovery
   ✓ Verify: Transaction eventually submitted
   ✓ Verify: No double-submit on retry
   ✓ Verify: Error logs show clear timeline
```

---

## Test Environment Setup

### Devnet Wallet Setup
```bash
# Create test wallet with devnet tokens
solana-keygen new --outfile ~/devnet-wallet.json
solana airdrop 10 ~/devnet-wallet.json --url devnet

# Import into wallet app via seed phrase
# Fund with devnet SKR from faucet
```

### Node Agent Setup
```bash
# Build and run locally
cd node-agent
make build
./node-agent --bind 0.0.0.0:5000 --config devnet.yaml
```

### App Configuration
```kotlin
// In build.gradle or local.properties for testing
DEVNET_RPC_URL = "https://api.devnet.solana.com"
NODE_AGENT_URL = "http://192.168.1.X:5000"
CLUSTER_RESTRICTION = "devnet"
```

---

## Test Execution Checklist

| Phase | Test Case | Manual | Automated | Status |
|-------|-----------|--------|-----------|--------|
| 2 | Read balance | ✓ | ✓ | Pending |
| 2 | Staking accounts | ✓ | ✓ | Pending |
| 2 | Error handling | ✓ | ✓ | Pending |
| 3 | Policy before MWA | ✓ | ✓ | Pending |
| 3 | Valid signing | ✓ | ✓ | Pending |
| 3 | Signature verification | ✓ | ✓ | Pending |
| 4 | Revocation | ✓ | ✓ | Pending |
| 5 | Full E2E stake flow | ✓ | ✓ | Pending |
| 5 | Crash recovery | ✓ | ✓ | Pending |
| 5 | Network recovery | ✓ | ✓ | Pending |

---

## Success Criteria

All tests pass ✓:
- Balances read correctly
- Staking accounts enumerated
- Policies enforced before wallet
- Signatures verified cryptographically
- Revocation works immediately
- End-to-end flows complete
- Crash recovery is deterministic
- No double submissions
- All errors logged with context

---

## References

- `INTEGRATION_GUIDE.md` - Setup instructions
- `CUSTODY_ARCHITECTURE.md` - Architecture details
- `EndToEndTest.kt` - Automated tests
