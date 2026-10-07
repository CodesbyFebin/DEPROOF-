# Current source review — 2026-10-07 UTC

Pinned repository: CodesbyFebin/DeProof--Solana-Depin-Dapp
Pinned revision: 59b5785d780387725ff34288024e2b534cbc182d

## First repairs

- app/build.gradle.kts comments out the real MWA dependency. app/src/main/java/com/solana/mobilewalletadapter/clientlib/MwaStubs.kt throws unsupported operations. The active data/solana/MobileWalletAdapter.kt opens custom links and returns null authorization/signature. data/wallet/MobileWalletAdapterClient.kt rejects wallet callback operations as unimplemented. Choose and qualify one real SDK path instead of keeping overlapping wrappers.
- Active com.deproof.ui.MainActivity constructs fixed observation metrics and a fixed digest. Replace with attributable user/node input or label/isolate as an illustrative demo. Never promote this record to independent verification.
- app/build.gradle.kts uses abortOnError=false and checkReleaseBuilds=false. Historical build/CI pass does not establish strict lint acceptance.
- Manifest references com.example.data.ReminderReceiver, while the legacy com.example source is in .build-excluded/. Reconcile active manifest/source references and prove receiver functionality; do not move legacy source without checking overlapping classes.
- Preserve exact-byte review and signed-return validation in the real transaction path. No display text, open deep link or detached message signature establishes transaction settlement.

## Evidence provenance

Bundled historical qualification reports are preserved as upstream documents, not endorsed as fresh acceptance. They reference earlier source paths including com/example and a Mac/Docker environment. This package has not been compiled or installed. Recalculate all exact 336 registry records only after source-bound commands execute. FAIL/BLOCKED/NOT_RUN remain unresolved; no record is closed by upstream reference inclusion.

## Integration order

Baseline and source reconciliation → integer amounts/typed observations → supported decoder/serialized message binding → real MWA authorization and devnet return verification → migrations/evidence signing/export → paired host read-only inspection and restricted execution → one provider adapter → device/release qualification.

Official templates and documentation are starting references. Community SKR code requires precision/error/cluster handling repairs and authoritative layout verification. DAppNode GPL code is external reference, not silently incorporated into DEPROOF's license. No arbitrary remote shell, automatic mainnet spending, nested agents or orchestrator has been run.
