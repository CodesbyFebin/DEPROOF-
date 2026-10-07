# Flux Integration Plan
**Status: PLANNED (not yet implemented)**  
**Target Phase: 2B (Oct 15+)**  
**Scope: Read-only node adapter → Runtime controls (staged)**

---

## Overview

Integrate Flux as a read-only node adapter into DEPROOF, enabling operators to contribute computing power via Flux nodes while maintaining separate tracking of FLUX rewards vs. SKR payments.

**Strategy**: Build in stages — discover node health first, then add runtime controls after proving observation accuracy.

---

## Stage 1: Discovery

### Objective
Identify Flux node installation and validate hardware compatibility.

### Acceptance Criteria
- [ ] Flux project card displays installation type
- [ ] Hardware requirements clearly listed
- [ ] HP DL20 Gen9 compatibility check performed
- [ ] Official requirements documented (not assumed)

### Implementation

**1.1 Flux Project Discovery**
- Query Flux official API for node specifications
- Identify required collateral: **1,000 FLUX**
- Document supported node tiers (Cumulus, Stratus, Nimbus)

**1.2 Hardware Validation (HP DL20 Gen9)**

Current hardware specifications:
```
HP DL20 Gen9 (example)
├─ Cores/Threads: [VERIFY]
├─ RAM: [VERIFY]
├─ Storage: [VERIFY]
├─ Network: [VERIFY]
└─ Public IP: [REQUIRED]
```

Flux minimum requirements (Cumulus tier):
- ✓ CPU: 2 cores / 4 threads
- ✓ RAM: 8 GB
- ✓ Storage: 220 GB SSD/NVMe
- ✓ Network: ≥25 Mbit/s
- ✓ Public IP: Yes
- ✓ Collateral: 1,000 FLUX

**Compatibility Status**: [PENDING VERIFICATION]

**Note**: Hardware ownership ≠ qualified node. Benchmark must pass.

**1.3 Official Upstream Review**
- [ ] FluxOS documentation reviewed
- [ ] Flux daemon requirements verified
- [ ] Flux API documentation cached

### Deliverables
- `FluxDiscovery.kt` — Node detection and hardware validation
- `flux-requirements.json` — Official specs snapshot
- Compatibility report for HP DL20 Gen9

---

## Stage 2: Monitoring

### Objective
Track operator-owned node health and extract observations.

### Acceptance Criteria
- [ ] Query operator's specific node (not public gateway)
- [ ] Handle stale, unavailable, malformed responses gracefully
- [ ] Observations timestamped with endpoint source
- [ ] Response digest computed for audit trail

### Implementation

**2.1 Node Health Monitoring**

```kotlin
// Query operator's specific node, not public gateway
interface FluxNodeMonitor {
    suspend fun getNodeHealth(nodeId: String): FluxNodeObservation
    // Targets: <operator-node-ip>:16110 or similar
    // NOT: api.runonflux.io (public gateway)
}

data class FluxNodeObservation(
    val nodeId: String,
    val tier: String,                    // Cumulus, Stratus, Nimbus
    val benchmarkScore: Long,
    val uptime: Long,                    // seconds
    val cpuUsage: Double,                // 0-100%
    val memoryUsage: Double,             // 0-100%
    val storageUsage: Double,            // 0-100%
    val networkBandwidth: Long,          // Mbps
    val collateralStatus: String,        // "LOCKED", "UNLOCKED"
    val rewardHeight: Long,              // Block height
    val timestamp: Long,                 // Observation time (UTC ms)
    val endpoint: String,                // Node endpoint queried
    val responseDigest: String           // SHA256 of raw response
)
```

**2.2 Observation Digest**
- Compute SHA256 of raw node response
- Store endpoint URL for verification
- Timestamp in UTC for audit trail
- Label clearly: "Flux node-specific observation" (not network-wide)

**2.3 Error Handling**

| Response State | Handling | Evidence |
|---|---|---|
| Stale data | Cache previous + mark stale flag | Timestamp diff > threshold |
| Unavailable | Graceful fallback | Error logged with endpoint |
| Malformed JSON | Reject + log | Validation error recorded |
| Invalid signature | Reject (if signed) | Signature verification failed |

### Deliverables
- `FluxNodeMonitor.kt` — Health observation interface
- `FluxObservation.kt` — Data structure with digest tracking
- Error handling tests (stale, unavailable, malformed)

---

## Stage 3: Evidence Export

### Objective
Export timestamped observations with clear attribution.

### Acceptance Criteria
- [ ] Observations labeled "Flux node observation" (not claim of independent proof)
- [ ] Endpoint clearly documented
- [ ] Response digest included
- [ ] Timestamp in UTC
- [ ] SKR payment status kept separate from FLUX rewards

### Implementation

**3.1 Observation Structure**

```kotlin
data class FluxEvidence(
    // Observation metadata
    val sourceType: String = "flux-node-observation",  // NOT "proof"
    val nodeId: String,
    val endpoint: String,
    val timestamp: Long,
    
    // Metrics from node
    val nodeObservation: FluxNodeObservation,
    
    // Audit trail
    val responseDigest: String,                // SHA256 of raw response
    val digestAlgorithm: String = "SHA-256",
    
    // Asset tracking (separate)
    val fluxRewardStatus: String,              // "PENDING", "CLAIMED", etc.
    val skrPaymentStatus: String = "NOT_SUBMITTED",  // Always separate
    
    // Clarity on proof status
    val proofContribution: String = "node-health-observation-only"
)
```

**3.2 Export Formats**

Export as:
1. **JSON** — Full observation with digest
2. **CSV** — Time-series for analytics
3. **On-chain log** — Solana SPL token metadata (when applicable)

**3.3 Clear Labeling**

Each export includes:
```
"This is a node health observation from Flux.
It does not constitute independent proof of contribution.
It provides operator-owned metrics from their node endpoint.
Response digest: <SHA256> enables audit trail verification."
```

### Deliverables
- `FluxEvidenceExporter.kt` — JSON/CSV/chain export
- Evidence schema (JSON) with clear disclaimers
- Example observation export

---

## Stage 4: Runtime Controls (Phase 3A+)

### Objective
Enable scoped start/stop of node and prove operator ownership/revocation.

### Acceptance Criteria
- [ ] Start/stop commands scoped to operator's node only
- [ ] Ownership verification required before control
- [ ] Revocation reliable and immediate
- [ ] All control actions logged with timestamp

### Implementation (Deferred to Phase 3A)

**Note**: Runtime control implementation deferred until:
1. Node monitoring (Stage 2) proven accurate
2. Observation evidence validated
3. Ownership verification mechanism designed

```kotlin
// Planned interface (not implemented yet)
interface FluxRuntimeControl {
    suspend fun startNode(nodeId: String, ownershipProof: String): Result
    suspend fun stopNode(nodeId: String, ownershipProof: String): Result
    suspend fun revokeAccess(nodeId: String): Result
}
```

---

## Stage 5: Rewards Display

### Objective
Show FLUX rewards separately from SKR payments.

### Acceptance Criteria
- [ ] FLUX rewards displayed with actual chain data
- [ ] SKR payment status kept separate (never conflated)
- [ ] Reward source clearly labeled
- [ ] No claim of unified rewards ledger

### Implementation

**5.1 Reward Display Structure**

```kotlin
data class FluxRewardStatus(
    // FLUX asset (Flux blockchain)
    val fluxRewards: FluxRewardInfo,
    
    // SKR asset (Solana)
    val skrPaymentStatus: String = "NOT_SUBMITTED"
)

data class FluxRewardInfo(
    val amount: BigDecimal,
    val assetName: String = "FLUX",
    val blockchain: String = "Flux",
    val blockHeight: Long,
    val lastClaimHeight: Long,
    val claimable: Boolean
)
```

**5.2 UI Display (Compose)**

```
┌─────────────────────────────┐
│ Flux Node Rewards           │
├─────────────────────────────┤
│ 📊 FLUX Reward              │
│    Amount: 125.45 FLUX      │
│    Blockchain: Flux         │
│    Status: Claimable        │
├─────────────────────────────┤
│ 💰 SKR Payment              │
│    Status: NOT SUBMITTED    │
│    (separate ledger)        │
└─────────────────────────────┘
```

### Deliverables
- `FluxRewardDisplay.kt` — Compose UI component
- Reward status tracker
- Separation validation tests

---

## Hardware Compatibility Matrix

| Component | Requirement | HP DL20 Gen9 | Status |
|-----------|-------------|--------------|--------|
| CPU | 2c/4t | [VERIFY] | [PENDING] |
| RAM | 8 GB | [VERIFY] | [PENDING] |
| Storage | 220 GB SSD | [VERIFY] | [PENDING] |
| Network | ≥25 Mbit/s | [VERIFY] | [PENDING] |
| Public IP | Required | [VERIFY] | [PENDING] |
| Collateral | 1,000 FLUX | N/A (operator choice) | [PLANNED] |
| Benchmark | Must pass | [RUN] | [PENDING] |

**Action Required**: Verify actual HP DL20 Gen9 specs against official Flux requirements before proceeding.

---

## Official Upstreams (Reference)

### FluxOS
- **Source**: https://github.com/RunOnFlux/flux
- **Docs**: https://docs.runonflux.io
- **Hardware List**: https://runonflux.io/compatibility
- **Cumulus Tier**: Documented here

### Flux Daemon
- **API Port**: 16110 (default)
- **Endpoints**: 
  - Node health: `/api/daemon/getzinfo`
  - Benchmark: `/api/daemon/benchmark`
  - Control: `/api/daemon/{action}`

### Flux API Documentation
- **Public Gateway**: api.runonflux.io (network-wide info only)
- **Node-specific**: `http://<node-ip>:16110` (operator-owned)
- **Authentication**: Operator signature required for control

---

## Timeline & Dependencies

| Phase | Dates | Stage | Dependencies |
|-------|-------|-------|--------------|
| Phase 1 | Oct 6-8 | — | [INDEPENDENT] Device testing (APK submission) |
| Phase 2A | Oct 6-15 | — | [INDEPENDENT] AIOZ observations (complete) |
| **Phase 2B** | **Oct 15-22** | **1-3** | Flux Discovery, Monitoring, Evidence |
| Phase 2C | Oct 22-31 | — | [OPTIONAL] Web companion (React) |
| **Phase 3A** | **Nov 1-15** | **4** | Runtime Controls (start/stop/revoke) |
| Phase 3B | Nov 16+ | 5 | Rewards Display + Play Store |

---

## Key Principles

1. **Read-only First** — Observation accuracy proven before runtime control
2. **Operator Ownership** — Never query public gateway for node-specific health
3. **Asset Separation** — FLUX rewards ≠ SKR payments (distinct ledgers)
4. **Clear Attribution** — "Node observation" ≠ "independent proof claim"
5. **Audit Trail** — Endpoint + digest + timestamp for every observation
6. **Hardware Validation** — Collateral + benchmark both required (not just collateral)

---

## Implementation Status

| Stage | Status | PR | Notes |
|-------|--------|----|----|
| 1: Discovery | PLANNED | — | Requires HP DL20 Gen9 specs verification |
| 2: Monitoring | PLANNED | — | Node-specific endpoint queries |
| 3: Evidence | PLANNED | — | Digest tracking + clear labeling |
| 4: Runtime Controls | DEFERRED | — | Phase 3A (Nov 1+) |
| 5: Rewards Display | PLANNED | — | Asset separation |

---

## Notes

- **Phase 1 (Oct 6-8)**: Device testing with AIOZ observations. Flux integration separate.
- **Phase 2B (Oct 15)**: Flux research + architecture. Stage 1-3 implementation.
- **Phase 3A (Nov 1)**: Runtime controls (Stage 4).
- **Hardware**: HP DL20 Gen9 compatibility must be verified with official Flux checklist before installation attempt.
- **Rewards**: Keep Flux blockchain asset separate from SKR/Solana payment tracking.

No Deproof source changes or Flux installation performed yet. This plan defines scope only.
