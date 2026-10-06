# Deproof full implementation directive

Repository: /Users/cyberteck/DEPROOF-/DEPROOF-build
Authoritative master blueprint: docs/blueprint/DEPROOF_MASTER_BLUEPRINT_AND_DESIGN/DEPROOF_FINAL_MASTER_PROMPT.md

Read that file completely, all referenced specifications,
design tokens, registries, and docs/BUILD_DEPROOF.md when present.
Inspect existing source and reference images before changing files.

The master blueprint governs conflicts.
Preserve existing work, README assets, package identity and configuration.
Do not overwrite user changes or delete files to conceal failures.

Implement executable application behavior and verification.
Do not stop at a scaffold, plan, mock dashboard or documentation.

Brand:
- Product: Deproof / DEPROOF.
- Ecosystem mark: DEPR.
- Credit: Built by CodesbyFebin.
- $DEPR remains a brand concept; no token issuance.

Maintain the exact authoritative registries:
- 120 base features F001–F120.
- 100 core checks C001–C100.
- 62 base contracts FN001–FN062.
- 30 ecosystem requirements E001–E030.
- 24 ecosystem contracts EF001–EF024.
- 86 total function contracts.
Do not count overlapping requirements as unique features.

Inspect installed JDK, Android SDK, Gradle, Go, Node,
network access and device availability.
Resolve compatible versions from official documentation.
Pin dependencies and record decisions.

Implement in dependency order:

P1:
Native Kotlin/Compose Android application.
Now, Review and Receipts routes.
Noncustodial wallet integration and actual RPC observations.
Strict supported transaction decoding.
Exact serialized-message binding and returned-byte validation.
Persistent receipts, file hashing, schemas and golden vectors.
Meaningful unit tests and an assembled debug APK.

P2:
Persisted tasks, evidence capture/import, manifests,
task/evidence linkage, recovery and portable receipts.

P3:
User-owned node agent with scoped authenticated pairing.
Restricted local hosting and observed runtime state.
Bandwidth defaults off, with explicit consent,
enforced quotas, revocation and accurate metering.
Pinned proof jobs, real execution and independent verification.
Contribution receipts distinguish local measurements,
signatures, proof verification and external observations.

P4:
Responsive website, documentation, setup guides,
portable verification bundles and open-source delivery files.
Show actual implementation status throughout.

P5:
Additional ecosystem adapters independently qualified
against current official Solana/SKR/provider interfaces.
Unavailable protocols remain explicitly blocked.

Parallelize only independent components after shared schemas
and prerequisites exist. Use isolated worktrees or exclusive
file ownership. Integrate changes and rerun cross-component checks.
Never concurrently mutate shared Android or registry files.

Security:
Keep wallet transaction keys in the wallet.
cardHash is a display hash.
messageSha256 hashes exact serialized Solana message bytes.
Any changed message invalidates approval.
Refuse unknown or unsupported instructions.
Verify mint, decimals, instruction layout and account roles.
Keep native SOL staking outside the supported signing path.
Respect devnet qualification gates before mainnet drafts.
Do not initiate mainnet spending.

Delivery:
Generate actual source, Gradle wrapper, build configuration,
test fixtures, schemas, verifier tools and setup scripts.
Provide LICENSE, README, CONTRIBUTING, SECURITY,
threat model, privacy documentation and dependency decisions.
Preserve an existing license; document unresolved licensing choices.

Create scripts/qualify.sh and component qualification scripts.
Maintain machine-readable coverage for every registry entry:
phase, implementation paths, checks, status and evidence.
Use PASS, FAIL, BLOCKED and NOT_RUN accurately.
Never fabricate signatures, balances, measurements, proofs,
rewards, integrations, audits or passing checks.

Run appropriate automated tests and builds.
Fix failures before expanding dependent features.
Continue independent work when a device or provider is unavailable.
Record concrete manual gates instead of claiming production readiness.

Write:
- evidence/qualification/implementation-status.json
- evidence/qualification/qualification-report.md
- evidence/qualification/artifact-checksums.txt

Final report:
List implemented components and exact verification commands.
Identify APK and other artifact paths when actually produced.
Describe failed, blocked and unrun gates.
Do not claim 100/100 or full production qualification without evidence.

Work locally.
Do not push, publish releases, deploy services or issue tokens.
