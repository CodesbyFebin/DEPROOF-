# Phase 4: Database Persistence - Implementation Guide

**Date:** 2026-10-07  
**Phase:** Phase 4 of 5-Phase Framework Integration Roadmap  
**Duration:** Ongoing  
**Branch:** `phase-3-p4`

---

## Executive Summary

Phase 4 implements durable persistence for Phase 3 components using Android Room database. This enables crash recovery, audit trails, and lifecycle tracking across app restarts.

**Key Addition:** Database layer for CustodyDecision and PendingApproval entities with comprehensive DAOs and repositories.

---

## Architecture Overview

```
┌─────────────────────────────────────┐
│    Application Layer (UI)            │
│  ReviewScreen, MainActivity, etc     │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│   Repository Layer                  │
│  CustodyDecisionRepository           │  High-level API
│  PendingApprovalRepository           │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│   DAO Layer (Data Access Objects)   │
│  CustodyDecisionDao                  │  Query builders
│  PendingApprovalDao                  │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│   Entity + TypeConverters           │
│  CustodyDecisionEntity               │  Database schema
│  PendingApprovalEntity               │
│  InstructionListConverter            │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│   Room Database                      │
│  deproof_phase3.db                   │  SQLite on disk
└─────────────────────────────────────┘
```

---

## Database Schema

### Table: custody_decisions

| Column | Type | Properties |
|--------|------|-----------|
| id | TEXT | PRIMARY KEY |
| transactionId | TEXT | Indexed |
| payer | TEXT | Indexed for queries |
| instructions | TEXT (JSON) | Serialized instruction list |
| fee | INTEGER | |
| status | TEXT | CustodyDecisionStatus enum |
| reviewedMessageHash | TEXT | |
| reviewedAt | INTEGER | Unix timestamp |
| policyValidated | BOOLEAN | |
| policyValidatedAt | INTEGER | Unix timestamp |
| walletApprovalTime | INTEGER | Unix timestamp |
| walletApprovalStatus | TEXT | |
| signatureVerified | BOOLEAN | |
| signatureVerifiedAt | INTEGER | Unix timestamp |
| signatureHash | TEXT | |
| submittedAt | INTEGER | Unix timestamp |
| chainConfirmedAt | INTEGER | Unix timestamp |
| finalSignature | TEXT | |
| lastError | TEXT | |
| createdAt | INTEGER | Unix timestamp (PRIMARY SORT) |

**Indexes:**
- PRIMARY KEY: id
- SORT: createdAt DESC (for list queries)
- FILTER: status, payer (for fast lookups)

### Table: pending_approvals

| Column | Type | Properties |
|--------|------|-----------|
| transactionId | TEXT | PRIMARY KEY |
| createdAt | INTEGER | Unix timestamp |
| reviewedMessageHash | TEXT | |
| expectedSignatureStatus | TEXT | SignatureStatus enum |
| walletApprovedAt | INTEGER | Timestamp or 0 |
| signatureRetrievedAt | INTEGER | Timestamp or 0 |
| isReconciled | BOOLEAN | For recovery queries |
| reconciliationStatus | TEXT | SIGNED, ABANDONED, ERROR, PENDING |

**Indexes:**
- PRIMARY KEY: transactionId
- FILTER: isReconciled (for recovery) |
- SORT: createdAt DESC |

---

## Component Files

### 1. Database Definition

**File:** `app/src/main/kotlin/com/deproof/data/database/DeProofDatabase.kt`

```kotlin
@Database(
    entities = [CustodyDecisionEntity::class, PendingApprovalEntity::class],
    version = 1,
    exportSchema = true
)
abstract class DeProofDatabase : RoomDatabase() {
    abstract fun custodyDecisionDao(): CustodyDecisionDao
    abstract fun pendingApprovalDao(): PendingApprovalDao
}
```

**Initialization:**
```kotlin
val db = DeProofDatabase.getInstance(context)
val custodyDao = db.custodyDecisionDao()
val approvalDao = db.pendingApprovalDao()
```

### 2. Entities

#### CustodyDecisionEntity
**File:** `CustodyDecisionEntity.kt` (90 lines)
- Maps domain CustodyDecision to database row
- `fromDomain()` / `toDomain()` converters
- Full 17-field custody lifecycle tracking

#### PendingApprovalEntity
**File:** `PendingApprovalEntity.kt` (50 lines)
- Maps domain PendingApproval to database row
- Reconciliation status tracking
- Timestamp fields for recovery workflow

### 3. TypeConverters

**File:** `TypeConverters.kt` (90 lines)

#### InstructionListConverter
Serializes `List<Instruction>` to JSON:
```kotlin
instructions = [
  { programId: "...", discriminator: 3, accounts: [...], data: "base64..." },
  ...
]
```

#### CustodyDecisionStatusConverter
Enum serialization: `PENDING_REVIEW` ↔ `"PENDING_REVIEW"`

#### SignatureStatusConverter
Enum serialization: `SIGNED` ↔ `"SIGNED"`

### 4. DAOs (Data Access Objects)

#### CustodyDecisionDao
**File:** `CustodyDecisionDao.kt` (100 lines)

**Query Methods:**
- `insert()` - Insert or replace
- `update()` - Update existing
- `delete()` - Delete by entity
- `getById()` - Single lookup by ID
- `getByTransactionId()` - Lookup by tx ID
- `getByStatus()` - Filter by status
- `getAll(limit, offset)` - Paginated list
- `observeByStatus()` - Flow-based subscription
- `observeAll()` - Flow-based subscription
- `getByPayer()` - Filter by payer address
- `getByDateRange()` - Time-range queries
- `deleteUnconfirmedOlderThan()` - Cleanup

**Batch Updates:**
- `updateStatus()` - Status transitions
- `updatePolicyValidation()` - Phase tracking
- `updateSignatureVerification()` - Phase tracking
- `updateWalletApproval()` - Phase tracking
- `updateError()` - Error logging

#### PendingApprovalDao
**File:** `PendingApprovalDao.kt` (90 lines)

**Query Methods:**
- `insert()` - Record pending approval
- `getByTransactionId()` - Lookup
- `getPending()` - All unreconciled
- `observePending()` - Flow subscription
- `getExpiredPending()` - Timeout detection
- `deleteByTransactionId()` - Clear
- `deleteReconciledOlderThan()` - Cleanup

**Batch Updates:**
- `markReconciled()` - Mark as processed
- `updateWalletApproval()` - Timestamp
- `updateSignatureRetrieval()` - Timestamp

### 5. Repositories

#### CustodyDecisionRepository
**File:** `CustodyDecisionRepository.kt` (200 lines)

High-level API wrapping DAO:
```kotlin
repository.save(decision)      // Insert or update
repository.update(decision)    // Update existing
repository.getById(id)         // Single lookup
repository.getByStatus(status) // Filter
repository.updateStatus(id, status)     // Phase transition
repository.updatePolicyValidation(id, validated)
repository.updateSignatureVerification(id, verified, hash)
repository.updateWalletApproval(id, status)
repository.updateError(id, error)
repository.deleteUnconfirmedOlderThan(ageMs)
```

**Error Handling:** All methods return `Result<T>`

#### PendingApprovalRepository
**File:** `PendingApprovalRepository.kt` (180 lines)

High-level API wrapping DAO:
```kotlin
repository.recordPendingApproval(txId, hash)
repository.getPendingApprovals()
repository.getByTransactionId(txId)
repository.clearPendingApproval(txId)
repository.reconcilePendingApprovals()    // Load from DB
repository.getExpiredPendingApprovals()   // Timeout detection
repository.cleanupReconciledOlderThan(ageMs)
```

**Error Handling:** All methods return `Result<T>`

### 6. Persistent Recovery Manager

**File:** `PersistentMwaRecoveryManager.kt` (150 lines)

Wraps MwaRecoveryManager with database persistence:
```kotlin
val manager = PersistentMwaRecoveryManager(repository)

// Record approval (memory + database)
manager.recordPendingApproval(txId, hash)

// Clear approval (memory + database)
manager.clearPendingApproval(txId)

// Reconcile (loads from database, query-first)
manager.reconcilePendingApprovals()

// Load from DB after crash
manager.loadFromDatabase()

// Cleanup old entries
manager.cleanup(ageMs)
```

---

## Usage Patterns

### Initialize Database

```kotlin
// In Application.onCreate() or dependency injection setup
val db = DeProofDatabase.getInstance(context)
val custodyRepo = CustodyDecisionRepository(db.custodyDecisionDao())
val approvalRepo = PendingApprovalRepository(db.pendingApprovalDao())
val recoveryManager = PersistentMwaRecoveryManager(approvalRepo)
```

### Save Transaction Decision

```kotlin
lifecycleScope.launch {
    val decision = CustodyDecision(
        transactionId = "tx_123",
        payer = userAddress,
        instructions = listOf(...),
        fee = 5000
    )
    
    custodyRepo.save(decision).onSuccess {
        // Decision persisted
    }
}
```

### Track Policy Validation

```kotlin
lifecycleScope.launch {
    // After policy validation passes:
    custodyRepo.updatePolicyValidation(
        id = decision.id,
        validated = true
    )
    
    // Status automatically set to POLICY_VALIDATED
    // policyValidatedAt set to current time
}
```

### Record Pending Approval

```kotlin
lifecycleScope.launch {
    recoveryManager.recordPendingApproval(
        transactionId = decision.id,
        reviewedMessageHash = messageHash
    ).onSuccess {
        // Approval recorded in memory + database
        launchWallet()
    }
}
```

### Verify Signature & Update

```kotlin
lifecycleScope.launch {
    val verified = signatureVerifier.verify(reviewedBytes, signedTx)
    if (verified.isSuccess && verified.getOrNull()?.isValid == true) {
        custodyRepo.updateSignatureVerification(
            id = decision.id,
            verified = true,
            hash = signatureHash
        )
        
        recoveryManager.clearPendingApproval(decision.id)
    }
}
```

### Crash Recovery on App Start

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    lifecycleScope.launch {
        // Recover from crash
        recoveryManager.loadFromDatabase()
        
        val results = recoveryManager.reconcilePendingApprovals()
        results.getOrNull()?.forEach { reconciliation ->
            when (reconciliation.status) {
                SIGNED -> {
                    // Resubmit transaction
                    submitTransaction(reconciliation.finalSignature)
                }
                ABANDONED -> {
                    // Show user: approval expired or rejected
                    showNotification("Transaction abandoned")
                }
                ERROR -> {
                    // Show error dialog
                    showErrorDialog(reconciliation.errorMessage)
                }
                PENDING -> {
                    // Show waiting dialog
                    showWaitingDialog()
                }
            }
        }
    }
    
    setContent { App() }
}
```

### Query Transaction History

```kotlin
lifecycleScope.launch {
    // Get all confirmed transactions
    val confirmed = custodyRepo.getByStatus(CustodyDecisionStatus.CONFIRMED)
    
    // Get pending transactions
    val pending = custodyRepo.getByStatus(CustodyDecisionStatus.PENDING_REVIEW)
    
    // Get recent transactions by date
    val recent = custodyRepo.getAll(limit = 50, offset = 0)
    
    // Observe changes (real-time)
    custodyRepo.observeAll().collect { decisions ->
        updateUI(decisions)
    }
}
```

### Cleanup Old Data

```kotlin
// In scheduled task or on app start
lifecycleScope.launch {
    // Delete unconfirmed transactions older than 7 days
    custodyRepo.deleteUnconfirmedOlderThan(ageMs = 7 * 24 * 60 * 60 * 1000)
    
    // Delete reconciled approvals older than 24 hours
    recoveryManager.cleanup(ageMs = 24 * 60 * 60 * 1000)
}
```

---

## Migration Strategy

### Version 1 → Version 2 (Future)

Room provides `Migration` API for schema updates:

```kotlin
val migration1To2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new column
        database.execSQL(
            "ALTER TABLE custody_decisions ADD COLUMN newField TEXT DEFAULT ''"
        )
    }
}

// Register migration
Room.databaseBuilder(...)
    .addMigrations(migration1To2)
    .build()
```

**Current Policy:** `fallbackToDestructiveMigration()` for development.
**Production:** Must use proper migrations.

---

## Performance Considerations

### Query Optimization

- **Index on createdAt** for sorted list queries
- **Index on status** for filter queries
- **Index on isReconciled** for recovery queries
- **Pagination** for large result sets (limit + offset)

### TypeConverter Performance

- **InstructionListConverter:** JSON serialization (~100μs per instruction)
- Acceptable for transaction counts (typically 1-5 instructions)
- Cache if needed for bulk queries

### Database Size

- **Typical custody decision:** ~500 bytes (with instruction data)
- **Typical pending approval:** ~100 bytes
- **1000 decisions:** ~1 MB
- **10,000 decisions:** ~10 MB (acceptable for Android storage)

### Concurrent Access

- Room handles multi-threaded access safely
- Use `Flow` for reactive updates without polling
- Mutex protection in PersistentMwaRecoveryManager

---

## Testing

### Unit Tests for DAOs

```kotlin
@RunWith(AndroidTestRunner::class)
class CustodyDecisionDaoTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var db: TestDatabase
    private lateinit var dao: CustodyDecisionDao
    
    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            context,
            TestDatabase::class.java
        ).build()
        dao = db.custodyDecisionDao()
    }
    
    @Test
    fun testInsertAndRetrieve() = runBlocking {
        val entity = CustodyDecisionEntity(...)
        dao.insert(entity)
        
        val retrieved = dao.getById(entity.id)
        assertEquals(entity, retrieved)
    }
}
```

### Integration Tests

```kotlin
@RunWith(AndroidTestRunner::class)
class CustodyDecisionRepositoryTest {
    
    @Test
    fun testSaveAndUpdate() = runBlocking {
        val repo = CustodyDecisionRepository(dao)
        val decision = CustodyDecision(...)
        
        repo.save(decision)
        repo.updateStatus(decision.id, POLICY_VALIDATED)
        
        val retrieved = repo.getById(decision.id).getOrNull()
        assertEquals(POLICY_VALIDATED, retrieved?.status)
    }
}
```

---

## Troubleshooting

### Database Locked

**Symptom:** `SQLITE_BUSY` during writes

**Solution:** Ensure all database operations are on background threads:
```kotlin
lifecycleScope.launch(Dispatchers.IO) {
    repository.save(decision)
}
```

### TypeConverter Serialization Errors

**Symptom:** `SerializationException` when reading instructions

**Solution:** Check JSON format; use try-catch in converter:
```kotlin
@TypeConverter
fun toInstructionList(json: String): List<Instruction> {
    return try {
        Json.decodeFromString(json)
    } catch (e: Exception) {
        emptyList() // Fallback
    }
}
```

### Schema Mismatch

**Symptom:** `Room(2): Schema validation failed` on migration

**Solution:** Either:
1. Use `fallbackToDestructiveMigration()` (dev only)
2. Create proper `Migration` for production
3. Bump database version and recreate schema

---

## Next Steps (Future Phases)

- [ ] Add encryption at rest (EncryptedSharedPreferences for sensitive fields)
- [ ] Implement network-level recovery (relay nodes)
- [ ] Add audit log export (JSON, signed)
- [ ] Observability metrics (policy violations, recovery success rate)
- [ ] External security audit

---

## References

- Android Room Documentation: https://developer.android.com/training/data-storage/room
- SQLite Migrations: https://developer.android.com/training/data-storage/room/migrating-db-versions
- Kotlin Serialization: https://github.com/Kotlin/kotlinx.serialization

---

**Phase 4 Status:** ✅ COMPLETE

**Components Implemented:**
- ✅ DeProofDatabase (Room definition)
- ✅ CustodyDecisionEntity + DAO
- ✅ PendingApprovalEntity + DAO
- ✅ TypeConverters (3 types)
- ✅ CustodyDecisionRepository
- ✅ PendingApprovalRepository
- ✅ PersistentMwaRecoveryManager
- ✅ Documentation & examples

---

**Author:** Claude Code  
**Date:** 2026-10-07  
**Branch:** `phase-3-p4`
