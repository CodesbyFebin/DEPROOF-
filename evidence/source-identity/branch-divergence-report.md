# Branch divergence report

Generated 2026-10-06 12:51 UTC. Local record only. Nothing is merged, renamed, reset, pushed or retargeted.

## Source identities

| Line | Ref | Commit | Commits since merge base |
|---|---|---|---:|
| Mac (this checkout, expanded implementation) | feat/deproof-full-build | 158651ac3ee98dfe5d8094456f2172610db71f74 | 33 |
| Cloud (remote) | origin/feat/deproof-full-build | aa94eea0986c1d4b5574f476e1d7b103d0b75730 | 47 |
| Merge base | 797f4e7 | 797f4e7756bcd43572fe1f8d60379369e5822774 | 0 |

Remote commits are external evidence. Their results are attributed to commit aa94eea0986c1d4b5574f476e1d7b103d0b75730 and have not been re-run here.

## Commits on the Mac line (not on remote)

- 158651a Record clean-HEAD qualification run 20261006T123812Z-86707 at 7dcd9e3
- 7dcd9e3 Add F030 out-of-order check and F079 status-contract tests; link criterion tests
- 4b07765 Preserve historical receipts across refresh and qualify Room persistence
- ee988bf Record fresh backup, metadata and proof wiring qualification evidence
- 767cad6 Record clean-HEAD qualification run 20261006T110405Z-83574 at 4e0e81e
- 4e0e81e Link backup and proof tests; document backup and proof wiring; signed-profile test
- 2f09f5e Report node proof capabilities and allow only the owner-local resource profile
- 0818604 Add proof-job dispatch gate and node-side proof command
- 647f3d6 Wire encrypted portable backup and private-file attachment metadata
- bfc50f7 Record qualification run 20261006T103121Z-80605 at 611045e
- 611045e Fix ConnectorTest to find connectors/support.json from any working directory
- 59d232f Add EF019 proof-job matcher (partial) and link its tests
- f236344 Link Room and backup tests to FN051, FN052 and F118; regenerate registries
- c16898e Add encrypted backup container for F118 (partial)
- 75379ba Add Room persistence tests on the JVM (Robolectric, in-memory); FN051/FN052 evidence
- c371282 Corrective: remove unconditional test-link union; reviewed link mappings
- 5e53c00 Add read-only connector dispatcher for FN059 (partial)
- 95797fd Preserve reviewed test links during qualification refresh
- 56092ff Record full qualification run at efcc853 (run 20261006T095457Z-77693)
- a5eb7a8 Bundle browser screenshots cited by the report; record unbundled references
- efcc853 Commit source file recorded by the source manifest; add source-identity records
- bd88aa7 Regenerate uncovered and prioritized backlog reports
- 831c348 Make criterion-specific test links survive registry regeneration
- dc3ee12 Repair build reproducibility and bind qualification to source manifests
- 6c79aa9 Link tests to C047, F049 and FN048; record local evidence
- 143adb7 Add rebuildExpiredTransaction (FN048) and route MainActivity through it
- 4d34fcd Link review-binding and policy tests to F045-F048, C095, FN062
- 582a614 Move memo-rebuild and key-level policies into domain; add policy tests
- 0079cac Link verified local tests to F040, F041 and FN042; correct priority buckets
- 390c0ed Add inspection and lookup-table unit tests; add resolveLookupTables entry point
- 650361a Add prioritized plan for the 281 unresolved requirements
- 4b054f1 Add review-binding unit tests for timestamp, fee, account and stored-hash gaps
- 7136b7b Preserve expanded Deproof implementation with sanitized qualification evidence

## Commits on the cloud line (not on Mac)

- aa94eea docs: Update Phase 1 status - all P2 items (F049-F054) complete
- 9936d6f F054-P2-Code: Build Variant Configuration with environment-specific flavors
- b1cced0 F053-P2-Code: Release Build Signing with keystore and environment configuration
- e1684cd F052-P2-Code: ProGuard Configuration with comprehensive app-specific rules and testing/maintenance documentation
- ccb349f F051-P2-Code: App Shortcuts Support
- 7d5757c F050-P2-Code: Deep Linking Support
- 0556e58 F049-P2-Code: Timber Logging Integration
- b667ea1 FN059-P3-Code: Read-Only Connector Dispatcher (Decision A)
- 63cd66b docs: Add Phase 1 implementation status and handoff guide
- e480420 F118-P2-Code: Android Encrypted Backup Infrastructure
- 5b9f4b4 F048-P2-Code: Null Safety Annotations
- a93e7a9 F047-P2-Code: Error Recovery Patterns
- a1bcde8 FN051/FN052: Add Android instrumented-test infrastructure for Room persistence
- 2523455 docs: Correct registry - remove overclaimed Phase 1 completion
- ee78f97 docs: Update registry to reflect parallel test completion (45/45 tests passing)
- 2b77ecd chore: Add Phase 1 evidence, registry recount, and baseline test documentation
- 5d4aca7 Phase 1 qualification framework: Add test infrastructure and requirement registry
- e21e396 test: Add P0 unit test stubs for Phase 1 critical path
- 5772bf6 docs: Add qualification snapshot and requirements matrix for feat/deproof-full-build
- 5f61ea4 fix: Resolve compilation errors in Kotlin code
- d7b46c3 fix: Add repository content filters to resolve Solana-specific artifacts only from Solana Maven repo
- 30b0077 fix: Replace Circle icon (doesn't exist) with text symbols in TasksScreen
- 688fc9f fix: Replace non-existent Material Design icons with available alternatives
- e236c73 fix: Fix material color scheme and missing imports in UI screens
- 967e636 fix: Add missing ErrorCard composable and fix icon imports in ReviewScreen
- 1453a5c fix: Remove unnecessary @TypeConverters annotation from DepRoofDatabase
- 3380cbf fix: Comment out unavailable dependencies
- f88074e fix: Add Solana Maven repository for walletadapterkit dependency
- aef4eeb fix: Add JitPack repository for GitHub-hosted dependencies
- 349df6f fix: Clean up ReceiptEntity.kt and define DepRoofDatabase properly
- 203fecd build: Add retry logic for network resilience in Gradle builds
- 4e9a941 fix: Pass clipboardManager to WalletConnectionCard
- c43c09f chore: Add missing Android resource files
- b18a20a fix: Use consistent database name from constant
- 9163690 build: Fix Kotlin serialization plugin configuration
- ed9a19a build: Make P3 dependencies optional for P1-only builds
- 6f3a47b Enable AndroidX support in Gradle configuration
- 6f0a2c3 build: Update Gradle wrapper to 8.5
- c33f495 build: Add Gradle wrapper scripts
- 21c9232 ci: Use manual sdkmanager for Android SDK setup
- 088e76b ci: Configure Android SDK packages properly
- 123486b ci: Update actions/upload-artifact to v4
- d9c81fc fix: Resolve build compilation errors
- bc4072c fix: Resolve build compilation errors
- 492e52c feat: Complete Deproof P1 MVP implementation with full Kotlin codebase
- f714188 Initialize Deproof P1 MVP project with complete build system
- 23e4d9e Add comprehensive implementation and build verification report

## Conflicting implementations

| Item | Mac implementation | Cloud implementation |
|---|---|---|
| FN059 `runConnector` | `app/src/main/java/com/example/domain/Connectors.kt` (read-only dispatcher over `connectors/support.json`; every outcome BLOCKED with reason; freshness NOT_OBSERVED) | `app/src/main/kotlin/com/deproof/domain/usecase/FN059ReadOnlyDispatcher.kt` |
| F118 encrypted backup | `app/src/main/java/com/example/domain/Backup.kt` (AES-256-GCM, PBKDF2, authenticated header), `data/BackupRepository.kt`, `BackupWorkspace.kt` | `app/src/main/kotlin/com/deproof/data/security/EncryptedBackup.kt` |

## Read-only review of the cloud implementations (against authoritative contracts)

FN059 (contract: attributable adapter result with freshness; no fixed success values; unverified adapters BLOCKED):
- Useful: unverified and unknown adapters return BLOCKED with a reason; network adapters blocked by default. The Mac dispatcher has the same behaviour.
- Defect: the LOCAL_STORAGE branch returns a fixed `data` string (`{"source": "local_storage", ...}`) with no timestamp, provenance or freshness. The contract forbids fixed success values.
- Defect: `verifyAdapter(adapterId)` sets `isVerified = true` with no evidence attached. Verification is asserted, not earned.
- Not imported. Not a basis for a correction to the Mac FN059.

F118 (contract: encrypted backup and restore):
- Defect: `createBackup` writes `gson.toJson(...)` with `writeText`. The backup file is plaintext JSON. Only the internal preferences use EncryptedSharedPreferences.
- Defect: the backup content includes `Settings.Secure.ANDROID_ID` as `deviceId`, a device identifier, in plaintext.
- Restore reads the same plaintext file with `readText()`.
- Not imported. The Mac F118 encrypts the file. No correction to the Mac F118 is needed.

## Registry and completion claims

The cloud commit `aa94eea` message says "all P2 items (F049-F054) complete". Its registry and completion claims are not imported. Under the authoritative registry, F049–F054 statuses on this line are recalculated from local evidence only.

## Keep and reconcile

- Keep the Mac FN059 and F118 implementations.
- Reconcile later: the cloud Base58, binary parser, instruction decoder and message-binding tests use a different package (`com.deproof`). They can be compared against the Mac contracts without import.
- No registry from the cloud line is used.

## Secrets check

Scanned the cloud line for private-key blocks, GitHub tokens, Anthropic keys and AWS key IDs: none found. Signing passwords are read from environment variables. The tracked `local.properties` contains only `sdk.dir`.
