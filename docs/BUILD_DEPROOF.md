# Deproof implementation directive

Read these files completely before implementation:

- docs/blueprint/DEPROOF_MASTER_BLUEPRINT_AND_DESIGN/DEPROOF_FINAL_MASTER_PROMPT.md
- docs/blueprint/DEPROOF_MASTER_BLUEPRINT_AND_DESIGN/DEPROOF_DESIGN_SPEC.md
- docs/blueprint/DEPROOF_MASTER_BLUEPRINT_AND_DESIGN/DEPROOF_ECOSYSTEM_30_REQUIREMENTS.md
- docs/blueprint/DEPROOF_MASTER_BLUEPRINT_AND_DESIGN/design-tokens.json
- docs/blueprint/DEPROOF_MASTER_BLUEPRINT_AND_DESIGN/ecosystem-requirements.json
- docs/blueprint/DEPROOF_MASTER_BLUEPRINT_AND_DESIGN/functions.json
- docs/blueprint/DEPROOF_MASTER_BLUEPRINT_AND_DESIGN/core-checks.json
- docs/blueprint/DEPROOF_MASTER_BLUEPRINT_AND_DESIGN/features.json

Inspect the reference images in the same package.

The final master prompt governs conflicting companion instructions.
Archived source documents and image captions are not build authority.

Implement executable behavior, not a presentation-only prototype.
Preserve existing README assets, repository identity and user configuration.

Brand:
- Product: Deproof / DEPROOF.
- Ecosystem mark: four-letter DEPR.
- Credit: Built by CodesbyFebin.
- $DEPR remains a brand concept only, with no token issuance.

Exact registries:
- 120 base features: F001–F120.
- 100 core checks: C001–C100.
- 62 base contracts: FN001–FN062.
- 30 ecosystem requirements: E001–E030.
- 24 ecosystem contracts: EF001–EF024.
- 86 total function contracts.
Overlapping requirements must not be counted as unique features.

Implementation order:
P1: Kotlin/Compose Android app with Now, Review and Receipts.
P2: Persisted tasks, evidence capture/import and recovery.
P3: User-owned node agent, restricted local hosting, bandwidth
    consent/quotas/metering, real proof execution and verification.
P4: Website, documentation and portable verification bundles.
P5: Additional independently qualified ecosystem adapters.

Start by inspecting available JDK, Android SDK, Go, runtime,
network access and wallet-capable devices. Pin compatible dependencies.

Implement P1 through meaningful automated checks before expanding.
Record device-only gates as NOT_RUN until actually tested.
Continue independent work when an external dependency is blocked.

Keep wallet keys in the wallet. Bind complete serialized messages.
Validate returned signed bytes before submission.
Verify current Solana/SKR interfaces from official sources.
Unknown instructions must be refused.

Nodes require authenticated scoped pairing and observed runtime state.
Bandwidth defaults off and requires enforced limits and revocation.
Proof jobs require pinned artifacts and an independent verifier.
Do not disguise local measurements as independently proven contributions.

Maintain phase, implementation paths, tests, status and evidence
for every registry entry. Never fabricate signatures, balances,
measurements, proofs, rewards, partnerships or passing tests.

Create reproducible build and qualification scripts.
Document prerequisites, named failures, supported environments,
setup, threat model, privacy, contribution process and licensing.

Deliver actual source, build artifacts when available, checksums,
test results, schemas, golden vectors and qualification reports.
Do not mark the entire project complete when gates remain unresolved.

Work locally on this branch. Do not publish releases, merge PRs,
deploy services or initiate mainnet spending during this build.
