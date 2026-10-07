# Local backup, attachment and proof dispatch wiring

Deproof / DEPR — Built by CodesbyFebin. Device and external provider acceptance remain unqualified.

## F118 portable backup

Settings offers explicit encrypted export and additive restore through Android document pickers. DPBK version 1 uses AES-256-GCM, a random 16-byte salt and 12-byte nonce, and PBKDF2-HMAC-SHA256 with 210,000 iterations. The header is authenticated. Android UI support requires API 26 or later for the pinned PBKDF2 provider; older Android versions explicitly show unavailable. A passphrase of at least 12 characters is required and must be retained separately; no server or hardware key recovery is implied. No backup, passphrase or evidence is transmitted to RPC or a provider. Passphrase fields are masked and not saved across recreation; derived password buffers are cleared. Java/Kotlin runtime copies cannot be guaranteed erased.

The authenticated payload is deproof-portable-records-v1: Room tasks, receipts/events, observations, evidence metadata and exact file bytes, mapping/reminder drafts and independently verified signed contribution records. It excludes wallet/evidence private keys, node pairing seeds and sessions, active operation journals, sharing permissions and preference stores. Restoring reminders sets them CANCELLED; no alarm, node command, wallet operation or transaction is replayed. Restore clears current signing approval. Old public signatures remain verifiable without recovering their original private keys. Imported records remain historical producer/provider observations, not new chain or physical-truth evidence.

Supported bounds: 16 MiB authenticated plaintext and 8 MiB aggregate raw evidence during export, at most 10,000 rows per collection. This is an explicit bounded portable-record format, not a SQLite database image or full-device backup. Unsupported draft types, schemas, malformed metadata/relationships, incorrect file digests and invalid local/contribution signatures are refused before insertion. Evidence IDs are UUIDs, never paths. Wrong keys and ciphertext/header tampering refuse authentication. Unknown container/payload versions are refused.

Restore never overwrites existing IDs or files. Every Room insert occurs in one transaction; later constraint failure rolls back new rows and removes newly staged files. Existing records and bytes remain intact in the tested failure paths. A process crash between publishing files and committing Room can leave unreferenced private files; they confer no evidence/receipt acceptance. Crash recovery and document-provider interruption behavior still require physical-device qualification. No complete F118 acceptance is inferred from the Robolectric tests.

## FN052 file metadata and association

EvidenceRepository reads the actual private bytes, validates ID, timestamp, MIME and provenance, hashes raw files and rechecks expected metadata before association. Room enforces task foreign keys. A completed import is not deleted merely because subsequent UI delivery is cancelled. The operation journal recovers local file association only; it never replays wallet signing, transaction submission or node commands.

## EF019 observed proof capability matching

Android compares discovered owner-pinned jobs with fresh attributed node observations immediately before dispatch. The supported local cubic job declares backend, circuit digest/version and minimum CPU/RAM. Mismatched resources/backends/circuits, stale/malformed observations and missing profiles refuse dispatch. Unqualified external providers remain BLOCKED. A match still requires explicit input consent.

The node observes Linux MemAvailable, clamps it against readable cgroup-v2 remaining memory, and reports CPU limits. These observations are not reservations, attestation, GPU qualification, independent-provider acceptance or production proof isolation. Unsupported hosts without a RAM observation return UNAVAILABLE rather than an invented value. Older jobs without the signed resource profile are explicitly unavailable for Android matching; re-create a new owner-approved local job, never silently rewrite an existing signature.

Room tests execute the actual database/DAO under Robolectric. Domain tests execute the dispatch matcher. Neither establishes real Android document-provider behavior, wallet handoff, hardware-backed keys, external-provider qualification or production proof-system acceptance.
