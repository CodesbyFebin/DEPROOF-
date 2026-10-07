# Unified Observation Schema Integration Guide

**Status: PLANNED (Phase 2B+)**  
**Impact: Non-breaking, backward-compatible migration**  
**Timeline: After Phase 1 (Oct 8) submission**

---

## Overview

`UnifiedObservation.kt` provides a common interface for all observation sources (AIOZ, Flux, future providers). This enables:

- Consistent proof generation over mixed sources
- Unified DeviceStatsScreen display
- Single observation aggregator
- Extensible reward tracking

---

## Current State (Phase 2A)

### AIZObservation (in AIZStatsParser.kt)
```kotlin
data class AIZObservation(
    val schema: String,
    val provider: String = "AIOZ",
    val source: String,
    val sourceSha256: String,
    val metrics: Metrics,
    val speedUnit: String,
    val signature: String?,
    val providerAcknowledgement: String?,
    val independentVerification: String,
    val rewardAsset: String,
    val skrPayment: String
)
```

### DeviceStatsScreen (Jetpack Compose)
```kotlin
@Composable
fun DeviceStatsScreen(
    observation: AIZObservation,  // Only accepts AIZObservation
    modifier: Modifier = Modifier
)
```

### ProofGeneratorWithObservations
```kotlin
data class ProofInput(
    val observation: AIZObservation  // Only accepts AIZObservation
)
```

---

## Target State (Phase 2B+)

### Unified Observation Interface
```kotlin
interface Observation {
    val schema: String
    val provider: String
    val source: String
    val timestamp: Long
    val assurance: String
    val sourceSha256: String
    val endpoint: String?
    val signature: String?
    val independentVerification: String
    val rewardAsset: String
    val rewardStatus: String
    val skrPaymentStatus: String
}

// AIZObservation + FluxObservation both implement Observation
data class AIZObservation(...) : Observation
data class FluxObservation(...) : Observation
```

### DeviceStatsScreen (Updated)
```kotlin
@Composable
fun DeviceStatsScreen(
    observation: Observation,  // Accepts any Observation
    modifier: Modifier = Modifier
)
```

### ProofGeneratorWithObservations (Updated)
```kotlin
data class ProofInput(
    val observations: List<Observation>  // Multiple sources
)
```

---

## Migration Path (Non-Breaking)

### Step 1: Keep Phase 2A Unchanged
- AIZObservation in AIZStatsParser.kt remains as-is
- Phase 1 device testing unaffected
- PR #4 merges without changes

### Step 2: Introduce UnifiedObservation.kt (Phase 2B, Oct 15)
- New file: `UnifiedObservation.kt`
- Defines Observation interface
- Provides AIZObservation (new) + FluxObservation implementations
- No changes to existing code yet

### Step 3: Update Consumers (Phase 2B, Oct 15-22)
- Refactor DeviceStatsScreen to accept `Observation` interface
- Refactor ProofGeneratorWithObservations to handle multiple observations
- Create migration helpers:
  ```kotlin
  // Helper: Convert old AIZObservation to new Observation interface
  fun AIZObservation.toObservation(): Observation = this.copy(...)
  ```

### Step 4: Deprecate Old AIZObservation (Phase 3A)
- Mark old AIZObservation in AIZStatsParser.kt as @Deprecated
- Point users to new UnifiedObservation schema
- Keep for backward compatibility

### Step 5: Consolidate (Phase 3B)
- Remove old AIZObservation from AIZStatsParser.kt
- Move all observation types to single observations/ package

---

## Code Changes Required

### 1. Update AIZStatsParser.kt (Phase 2B)

**Before:**
```kotlin
object AIZStatsParser : IStatsParser {
    override fun parseStats(raw: ByteArray): AIZObservation {
        // ... parsing logic ...
        return AIZObservation(
            schema = "deproof-aioz-observation-v1",
            provider = "AIOZ",
            source = "operator-supplied-cli-stats",
            sourceSha256 = sourceSha256,
            metrics = Metrics(...),
            // ...
        )
    }
}

data class AIZObservation(
    val schema: String,
    val provider: String,
    // ...
)
```

**After:**
```kotlin
object AIZStatsParser : IStatsParser {
    override fun parseStats(raw: ByteArray): Observation {  // Changed return type
        // ... parsing logic ...
        return com.deproof.data.observations.AIZObservation(
            schema = "deproof-aioz-observation-v1",
            provider = "AIOZ",
            source = "operator-supplied-cli-stats",
            sourceSha256 = sourceSha256,
            metrics = Metrics(...),
            // ...
        )
    }
}

// Old data class kept for backward compat but marked @Deprecated
@Deprecated("Use com.deproof.data.observations.AIZObservation")
data class AIZObservation(...)
```

### 2. Update DeviceStatsScreen.kt (Phase 2B)

**Before:**
```kotlin
@Composable
fun DeviceStatsScreen(
    observation: AIZObservation,
    modifier: Modifier = Modifier
) {
    // Displays AIZObservation.metrics
    StatItem(
        label = "Storage Objects",
        value = formatNumber(observation.metrics.storageObjectCount),
        // ...
    )
}
```

**After:**
```kotlin
@Composable
fun DeviceStatsScreen(
    observation: Observation,  // Accept interface
    modifier: Modifier = Modifier
) {
    Column(...) {
        // Display observation metadata
        Text("${observation.provider} Observation")
        Text("Endpoint: ${observation.endpoint ?: "local"}")
        
        // Type-safe metric display
        when (observation) {
            is AIZObservation -> {
                StatItem(
                    label = "Storage Objects",
                    value = formatNumber(observation.metrics.storageObjectCount),
                    // ...
                )
            }
            is FluxObservation -> {
                StatItem(
                    label = "CPU Usage",
                    value = "${observation.nodeMetrics.cpuUsage}%",
                    // ...
                )
            }
        }
    }
}
```

### 3. Update ProofGeneratorWithObservations.kt (Phase 2B)

**Before:**
```kotlin
suspend fun generateProof(
    deviceId: String,
    statsJson: ByteArray
): GeneratedProof {
    val observation = parser.parseStats(statsJson)
    
    val proofInput = ProofInput(
        deviceId = deviceId,
        timestamp = System.currentTimeMillis(),
        randomNonce = generateRandomNonce(),
        observation = observation
    )
    // ...
}

data class ProofInput(
    val deviceId: String,
    val timestamp: Long,
    val randomNonce: ByteArray,
    val observation: AIZObservation  // Single observation
)
```

**After:**
```kotlin
suspend fun generateProof(
    deviceId: String,
    observations: List<Observation>  // Multiple sources
): GeneratedProof {
    val proofInput = ProofInput(
        deviceId = deviceId,
        timestamp = System.currentTimeMillis(),
        randomNonce = generateRandomNonce(),
        observations = observations  // Aggregated
    )
    // ...
}

data class ProofInput(
    val deviceId: String,
    val timestamp: Long,
    val randomNonce: ByteArray,
    val observations: List<Observation>  // Multi-source
)
```

---

## Testing Strategy

### Unit Tests (Phase 2B)

```kotlin
@Test
fun testAIZObservationImplementsInterface() {
    val obs: Observation = AIZObservation(...)
    assertEquals("AIOZ", obs.provider)
    assertEquals("deproof-aioz-observation-v1", obs.schema)
}

@Test
fun testFluxObservationImplementsInterface() {
    val obs: Observation = FluxObservation(...)
    assertEquals("Flux", obs.provider)
    assertEquals("deproof-flux-observation-v1", obs.schema)
}

@Test
fun testAggregatedObservations() {
    val aioz: Observation = AIZObservation(...)
    val flux: Observation = FluxObservation(...)
    val aggregated = AggregatedObservations(
        deviceId = "device-1",
        observations = listOf(aioz, flux)
    )
    
    assertEquals(2, aggregated.observationCount)
    assertEquals(setOf("AIOZ", "Flux"), aggregated.providers)
}
```

### Integration Tests (Phase 2B)

```kotlin
@Test
fun testDeviceStatsScreenWithMultipleObservations() {
    val aioz = AIZObservation(...)
    val flux = FluxObservation(...)
    
    // Should render both observation types
    composeTestRule.setContent {
        DeviceStatsScreen(aioz)
        DeviceStatsScreen(flux)
    }
    
    // Verify rendering
    onNodeWithText("Storage Objects").assertExists()  // AIOZ
    onNodeWithText("CPU Usage").assertExists()        // Flux
}
```

---

## Reward Tracking Consistency

### AIOZ Rewards (Phase 2A)
```kotlin
data class AIZObservation(
    val rewardAsset: String = "AIOZ",
    val skrPayment: String = "NOT_SUBMITTED"
)
```

### Flux Rewards (Phase 2B)
```kotlin
data class FluxObservation(
    val rewardAsset: String = "FLUX",
    val rewardStatus: String = "PENDING",
    val skrPaymentStatus: String = "NOT_SUBMITTED"
)
```

### Unified Interface
```kotlin
interface Observation {
    val rewardAsset: String        // Provider-native reward
    val rewardStatus: String       // Provider-specific status
    val skrPaymentStatus: String = "NOT_SUBMITTED"  // Always separate
}
```

**Key Principle**: SKR payment tracking is **always separate** from provider-native rewards.

---

## Implementation Checklist (Phase 2B)

- [ ] UnifiedObservation.kt created (DONE)
- [ ] AIZObservation updated to implement Observation interface
- [ ] FluxObservation implemented (Phase 2B)
- [ ] AIZStatsParser returns Observation interface
- [ ] DeviceStatsScreen accepts Observation interface
- [ ] ProofGeneratorWithObservations handles aggregated observations
- [ ] Unit tests for interface implementations
- [ ] Integration tests for multi-source rendering
- [ ] Backward compatibility maintained
- [ ] Old AIZObservation marked @Deprecated

---

## Timeline

| Phase | Task | Status |
|-------|------|--------|
| Phase 1 (Oct 8) | Device testing (AIOZ only) | Active |
| **Phase 2B (Oct 15)** | **Unified schema integration** | **Planned** |
| Phase 3A (Nov 1) | Multi-device aggregation | Planned |
| Phase 3B (Nov 16) | Consolidation + cleanup | Planned |

---

## Notes

- **Non-breaking**: Old code continues to work during migration
- **Extensible**: New observation sources add implementations without changing consumers
- **Testable**: Each observation type has independent tests
- **Clear**: `when (observation)` patterns make provider-specific logic explicit

No changes to Phase 2A code. Migration begins Phase 2B (Oct 15).
