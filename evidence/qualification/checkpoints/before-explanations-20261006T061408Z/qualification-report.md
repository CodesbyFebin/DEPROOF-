# Local qualification report

Deproof / DEPR — Built by CodesbyFebin. $DEPR remains a brand concept only.

BUILD_QUALIFIED: **True**. CORE_DEVICE_QUALIFIED: **False**. FULL_SCOPE_QUALIFIED: **False**. Automated passes do not establish production readiness.

## Implemented components

- Kotlin/Compose Now, Review and Receipts with nested Nodes, Bandwidth and Proof jobs. Real pinned-TLS node client, exact-byte Ed25519 pairing/commands, independently pinned node identity, encrypted node-control seed and durable command records; no wallet secrets.
- Room v1→v2 additive migration; existing event/task/evidence bytes preserved. Submission/command recovery never automatically replays uncertain effects. File-ready import journal verifies bytes before recovery. Device lifecycle remains unrun.
- Go agent with scoped pairing, self/owner revocation, observed host state, fixed-endpoint consent, persistent reservations, actual transfers, stop/cancellation and signed metering.
- Qualified cached BusyBox isolated HTTP service profile on the local Linux VM: real start/health/stop and restart reconciliation; no public ports/mounts/network, nonroot, read-only root, dropped capabilities, CPU/memory/PID/tmpfs limits. Rootful owner-trusted engine disclosed.
- Owner-signed bounded local proof discovery, pinned circuit/key, actual producer and separate verifier; tampered proof/key/input/job/receipt rejection; signed contribution receipts with explicit same-owner/educational assurance.
- Read-only live app RPC observations for both public clusters. Full genesis constants corrected; failures remain unavailable rather than zero. Public System program account only; no user wallet or spending.
- Seven website routes, registry filter and local receipt inspector. Browser screenshots and automated accessibility results recorded separately from manual/device qualification.

## Exact executed commands

| Gate | Command | Exit | Result | Log |
|---|---|---:|---|---|
| core-tests | `python3 scripts/qualify-core.py` | 0 | PASS | [evidence/qualification/core-tests.log](../../evidence/qualification/core-tests.log) |
| android-build | `scripts/qualify-android.sh` | 0 | PASS | [evidence/qualification/android-build.log](../../evidence/qualification/android-build.log) |
| node-tests | `scripts/qualify-node.sh` | 0 | PASS | [evidence/qualification/node-tests.log](../../evidence/qualification/node-tests.log) |
| prover-tests | `scripts/qualify-prover.sh` | 0 | PASS | [evidence/qualification/prover-tests.log](../../evidence/qualification/prover-tests.log) |
| verifier-tests | `python3 -m unittest discover -s tools -p test_*.py` | 0 | PASS | [evidence/qualification/verifier-tests.log](../../evidence/qualification/verifier-tests.log) |
| hosting-isolation | `python3 scripts/qualify-hosting.py` | 0 | PASS | [evidence/qualification/hosting-isolation.log](../../evidence/qualification/hosting-isolation.log) |
| node-integration | `env DEPROOF_TEST_HOSTING=1 python3 scripts/qualify-integration.py` | 0 | PASS | [evidence/qualification/node-integration.log](../../evidence/qualification/node-integration.log) |
| rpc-read-only | `python3 scripts/qualify-rpc.py` | 0 | PASS | [evidence/qualification/rpc-read-only.log](../../evidence/qualification/rpc-read-only.log) |
| coverage-reconciliation | `python3 scripts/reconcile-coverage.py` | 0 | PASS | [evidence/qualification/coverage-reconciliation.log](../../evidence/qualification/coverage-reconciliation.log) |
| backlog-priorities | `python3 scripts/prioritize-uncovered.py` | 0 | PASS | [evidence/qualification/backlog-priorities.log](../../evidence/qualification/backlog-priorities.log) |
| registry-checks | `python3 scripts/check-registries.py` | 0 | PASS | [evidence/qualification/registry-checks.log](../../evidence/qualification/registry-checks.log) |
| web-build | `python3 scripts/build-web.py` | 0 | PASS | [evidence/qualification/web-build.log](../../evidence/qualification/web-build.log) |
| web-checks | `python3 scripts/qualify-web.py` | 0 | PASS | [evidence/qualification/web-checks.log](../../evidence/qualification/web-checks.log) |
| browser-checks | `node scripts/browser/qualify.mjs` | 0 | PASS | [evidence/qualification/browser-checks.log](../../evidence/qualification/browser-checks.log) |
| coverage-reconciliation | `python3 scripts/reconcile-coverage.py` | 0 | PASS | [evidence/qualification/coverage-reconciliation.log](../../evidence/qualification/coverage-reconciliation.log) |
| backlog-priorities | `python3 scripts/prioritize-uncovered.py` | 0 | PASS | [evidence/qualification/backlog-priorities.log](../../evidence/qualification/backlog-priorities.log) |

Android script executes `bash ./gradlew testDebugUnitTest --stacktrace`, `bash ./gradlew assembleDebug --stacktrace`, and `bash ./gradlew lintDebug --stacktrace`. It hashes the APK only after these tasks succeed.

Historical failures: sandbox cache/native-library/socket and DNS restrictions were cleared through approved execution. Source failures were the MWA shared namespace and unguarded StrongBox exception. The live RPC probe additionally exposed shortened genesis constants; full-hash checks now have regression tests. Historical failures are preserved in prior run logs.

## Registry coverage

- features: {"BLOCKED": 19, "IMPLEMENTED_UNVERIFIED": 89, "NOT_STARTED": 5, "VERIFIED": 7}
- core-checks: {"NOT_RUN": 80, "PASS": 20}
- functions: {"BLOCKED": 8, "IMPLEMENTED_UNVERIFIED": 49, "NOT_STARTED": 1, "VERIFIED": 28}
- ecosystem-requirements: {"BLOCKED": 6, "IMPLEMENTED_UNVERIFIED": 24}

Exact counts remain 120 F, 100 C, 62 FN + 24 EF = 86 contracts and 30 E. `uncovered-requirements.json` enumerates each unresolved acceptance with paths/tests/evidence; local-profile verification never implies external/device qualification.

## Unresolved qualification gates

- **DEVICE_WALLET_HARDWARE: NOT_RUN** — ADB lists no connected physical device. MWA signing, Keystore protection/security level, Room-on-device migration and process-death/camera tests remain unrun.
- **HOST_SUPPORT_MATRIX: BLOCKED** — Only the documented constrained HTTP profile on this local Linux Docker VM is qualified. The owner-trusted Docker engine is rootful; rootless engines, native Linux/ARM64 targets and arbitrary services are unqualified.
- **EXTERNAL_PEER_PROVIDER: NOT_RUN** — Real loopback TLS pairing and controlled known-byte receiver passed locally; remote peer tunnels, NAT/mobile network transitions, provider corroboration and payouts remain untested.
- **PROOF_PRODUCTION_SETUP: NOT_RUN** — Owner-signed local cubic jobs and separate gnark verifier pass. Educational trusted setup and same-owner verifier domain; isolated production proof execution, external jobs/provider membership/rewards remain unqualified.
- **P5_EXTERNAL_ADAPTERS: BLOCKED** — Verified SKR staking layouts and additional provider authorization/protocol interfaces unavailable. No mainnet spending.
- **SOURCE_COMPLETENESS: NOT_RUN** — Full lookup resolution, legacy receipt import, unsupported media previews/capture lifecycle, optional location/mapping planner, background network policy, translations/reminders and complete Android backup integration remain incomplete. Bounded text/image previews are implemented but not device-qualified. Each unresolved registry entry is listed in uncovered-requirements.json.
- **RELEASE_AND_RIGHTS: BLOCKED** — No release signing or physical-device release checks; comprehensive SBOM/advisory and imported-asset rights review incomplete.
- **MANUAL_ACCESSIBILITY: NOT_RUN** — Automated Chromium/axe/keyboard/200% text checks do not establish full manual screen-reader, cross-browser or Android accessibility qualification.

## Current continuation

Bounded private text/image previews, persisted first-seen program indicators and the legacy raw digest signing contract are implemented; supporting local tests pass, Android hardware/lifecycle acceptance remains NOT_RUN. F097 now correctly links existing one-rebuild memo logic and fresh-review enforcement. No device pass is inferred.

Dependency/security/environment priorities: `evidence/qualification/prioritized-backlog.json`; code/evidence gaps and next steps: `docs/remaining-work.md`. Physical preflight: `python3 scripts/qualify-device.py`; install only with `--install` and follow `docs/device-qualification.md`.

## Produced artifacts

- `app/build/outputs/apk/debug/app-debug.apk` — debug-apk; SHA-256 `19134952737511c6e9e9be8d4129d72b0ee83521c699ac7ffd0983f00ca4a1a4`
- `node-agent/build/deproof-node` — darwin-amd64-node-binary; SHA-256 `99c7c824acbe4f8894021dbe5de525d45eaf7a44e1d675d34311d5a45b3caa91`
- `prover-worker/build/prove` — local-proof-producer; SHA-256 `4787649464f73cf71abeb9f5f7dd41114b29b5adc1b43e7d7ad07ce36604e782`
- `prover-worker/build/verify` — independent-proof-verifier; SHA-256 `f7c6c016fcf0c09919d61e2dffa75e4bed8f0ab11d14145c087281552b7c3301`
- `gradle/wrapper/gradle-wrapper.jar` — gradle-wrapper; SHA-256 `b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13`
- `tools/verify.py` — independent-receipt-verifier; SHA-256 `b5452e40c9da5fd9d0de41b95f0d1dce3163f8125e459e209a545724e987bde2`
- `prover-worker/build/qualified-result-20261006T051908Z-60750/circuit.r1cs` — local-proof-artifact; SHA-256 `d66951a99a8361b0677e6e08a8075773c1622f33645b2b1182ee60db39ef8954`
- `prover-worker/build/qualified-result-20261006T051908Z-60750/verification-key.bin` — local-proof-artifact; SHA-256 `ecdbe8118321ea2d649bd6cf5b0e0b295e256e26914e4019531df8619b224fb0`
- `prover-worker/build/qualified-result-20261006T051908Z-60750/proving-key.bin` — local-proof-artifact; SHA-256 `43a55c4baf0926845e1a5c9b390b0c8b54ae913d512c533afe35be61285687c8`
- `prover-worker/build/qualified-result-20261006T051908Z-60750/public-witness.bin` — local-proof-artifact; SHA-256 `da07f711885f5cdbf5a86f5e0c2d503f1cf6cf1ffe71d86cff6a4566b8c833cd`
- `prover-worker/build/qualified-result-20261006T051908Z-60750/manifest.json` — local-proof-artifact; SHA-256 `0bbf5da7c8c617718ea23b35e624b3841268c29e52eb696c825a2ca4fe38e3a1`
- `prover-worker/build/qualified-result-20261006T051908Z-60750/proof.bin` — local-proof-artifact; SHA-256 `67990a6f1fb8ced7fed8be89588f54543046e325d91c13719069f53c8e22076d`
- `prover-worker/build/qualified-result-20261006T060637Z-63781/circuit.r1cs` — local-proof-artifact; SHA-256 `d66951a99a8361b0677e6e08a8075773c1622f33645b2b1182ee60db39ef8954`
- `prover-worker/build/qualified-result-20261006T060637Z-63781/verification-key.bin` — local-proof-artifact; SHA-256 `83ff17084fe1db1e678f1fb0e3f45d1b52ca06c6389cb0963a945a1cbadaa982`
- `prover-worker/build/qualified-result-20261006T060637Z-63781/proving-key.bin` — local-proof-artifact; SHA-256 `205b44f23857c1e2f93dd00ef9c74ffb6c7eaa10bbaada255c88803652f2edf0`
- `prover-worker/build/qualified-result-20261006T060637Z-63781/public-witness.bin` — local-proof-artifact; SHA-256 `da07f711885f5cdbf5a86f5e0c2d503f1cf6cf1ffe71d86cff6a4566b8c833cd`
- `prover-worker/build/qualified-result-20261006T060637Z-63781/manifest.json` — local-proof-artifact; SHA-256 `6a58f53a7c16a9f89596a7ef2b21579279665769f936c16369ab0cf9327e5f26`
- `prover-worker/build/qualified-result-20261006T060637Z-63781/proof.bin` — local-proof-artifact; SHA-256 `7c4c749ddb5b8bf05a9b0da951f30feff5692659dca3e6c533b1a125e5743528`
- `prover-worker/build/qualified-result-20261006T054637Z-62530/circuit.r1cs` — local-proof-artifact; SHA-256 `d66951a99a8361b0677e6e08a8075773c1622f33645b2b1182ee60db39ef8954`
- `prover-worker/build/qualified-result-20261006T054637Z-62530/verification-key.bin` — local-proof-artifact; SHA-256 `6e04904836a36146a22f927befc1bbf1f5292771e2fbeb809ca0ba7833dd7a00`
- `prover-worker/build/qualified-result-20261006T054637Z-62530/proving-key.bin` — local-proof-artifact; SHA-256 `bfed8ebc058132c7e65bd39e1b8a483ef8da56b6a0191af57f97c94a30bf20fa`
- `prover-worker/build/qualified-result-20261006T054637Z-62530/public-witness.bin` — local-proof-artifact; SHA-256 `da07f711885f5cdbf5a86f5e0c2d503f1cf6cf1ffe71d86cff6a4566b8c833cd`
- `prover-worker/build/qualified-result-20261006T054637Z-62530/manifest.json` — local-proof-artifact; SHA-256 `82c101b179c083a1076b071cf6816ac3fab02d2d499789773f395b55ba4dca7d`
- `prover-worker/build/qualified-result-20261006T054637Z-62530/proof.bin` — local-proof-artifact; SHA-256 `c35e892937dd6caa065f226ba0ff414893ecd1e241e1b65b18bb78efc53232d1`
- `prover-worker/build/qualified-result-20261006T060344Z-63284/circuit.r1cs` — local-proof-artifact; SHA-256 `d66951a99a8361b0677e6e08a8075773c1622f33645b2b1182ee60db39ef8954`
- `prover-worker/build/qualified-result-20261006T060344Z-63284/verification-key.bin` — local-proof-artifact; SHA-256 `2bb4540a04c94235110c668004470206d917b197f46a97f6028a29f69afe567f`
- `prover-worker/build/qualified-result-20261006T060344Z-63284/proving-key.bin` — local-proof-artifact; SHA-256 `adf5233594f8c56a52c5c09eb5c545c0bc86a4acd2b73d23ab593ed8483df198`
- `prover-worker/build/qualified-result-20261006T060344Z-63284/public-witness.bin` — local-proof-artifact; SHA-256 `da07f711885f5cdbf5a86f5e0c2d503f1cf6cf1ffe71d86cff6a4566b8c833cd`
- `prover-worker/build/qualified-result-20261006T060344Z-63284/manifest.json` — local-proof-artifact; SHA-256 `636e81254249480e245b6903b3e7f61159a876523f548b75fb1032c4fc71abc0`
- `prover-worker/build/qualified-result-20261006T060344Z-63284/proof.bin` — local-proof-artifact; SHA-256 `c2ede77f4bea36107ec7adc1a1f292cc29216d929e254055369c14c8cb774ffc`
- `prover-worker/build/qualified-result-20261006T050420Z-60151/circuit.r1cs` — local-proof-artifact; SHA-256 `d66951a99a8361b0677e6e08a8075773c1622f33645b2b1182ee60db39ef8954`
- `prover-worker/build/qualified-result-20261006T050420Z-60151/verification-key.bin` — local-proof-artifact; SHA-256 `1e70273cda7799bf7650ca33ab5f719081d076dc8dd62ea7a657d947df2e5dd7`
- `prover-worker/build/qualified-result-20261006T050420Z-60151/public-witness.bin` — local-proof-artifact; SHA-256 `da07f711885f5cdbf5a86f5e0c2d503f1cf6cf1ffe71d86cff6a4566b8c833cd`
- `prover-worker/build/qualified-result-20261006T050420Z-60151/manifest.json` — local-proof-artifact; SHA-256 `39648eff367b6c022088a7050f2ab3b4ec689438618b6f5342b9bc299bc48ebb`
- `prover-worker/build/qualified-result-20261006T050420Z-60151/proof.bin` — local-proof-artifact; SHA-256 `547ab90e2af8648705ca95ecbaba433d28ce13165bdaca1cdfd4dced8a1ea78c`
- `prover-worker/build/qualified-result-20261006T054103Z-62006/circuit.r1cs` — local-proof-artifact; SHA-256 `d66951a99a8361b0677e6e08a8075773c1622f33645b2b1182ee60db39ef8954`
- `prover-worker/build/qualified-result-20261006T054103Z-62006/verification-key.bin` — local-proof-artifact; SHA-256 `bea6b8d2ed6bced44cd124240a3ebaffd611e740021f528912a6980a81b0dbd0`
- `prover-worker/build/qualified-result-20261006T054103Z-62006/proving-key.bin` — local-proof-artifact; SHA-256 `0c7276ee86eb5fed28f0a512ff326ef0655a48459e47fb510a9b47b1dca869c8`
- `prover-worker/build/qualified-result-20261006T054103Z-62006/public-witness.bin` — local-proof-artifact; SHA-256 `da07f711885f5cdbf5a86f5e0c2d503f1cf6cf1ffe71d86cff6a4566b8c833cd`
- `prover-worker/build/qualified-result-20261006T054103Z-62006/manifest.json` — local-proof-artifact; SHA-256 `deca73127e14167e6150c1b6599536143f51fe7ef0acf00a9d6f8684ad1b66ca`
- `prover-worker/build/qualified-result-20261006T054103Z-62006/proof.bin` — local-proof-artifact; SHA-256 `e93beacb3586145defea7d8a677c67a14d70ce6b0631813a2ece21026e987517`
- `evidence/qualification/browser/features-1280.png` — browser-evidence-screenshot; SHA-256 `aa6987e4063d199db2275cbf39c7c083a462fa1145d7510192a78eea52949f97`
- `evidence/qualification/browser/privacy-320.png` — browser-evidence-screenshot; SHA-256 `0a0ff6ac7e4ef3316820b86cf42fde88fec7ec8e44fd8581d7803891eae5c4f5`
- `evidence/qualification/browser/features-320.png` — browser-evidence-screenshot; SHA-256 `28ddaa9a86a09aaf3521886eb32532beb663586429380f3ba91572bb5c3307f8`
- `evidence/qualification/browser/index-1280.png` — browser-evidence-screenshot; SHA-256 `51da5c438fc0f6103b4c387e12ddd066b0f03e3290b1b68a8b73ee8995488161`
- `evidence/qualification/browser/project-1280.png` — browser-evidence-screenshot; SHA-256 `354c7476539f5b42eeda8373c0b217cb7ed5cabc240d6894b18c2a29a1db6e06`
- `evidence/qualification/browser/project-320.png` — browser-evidence-screenshot; SHA-256 `b4c481d886f24884343025662164e32974e6cc762b4927ee0ea71c95d4f435e7`
- `evidence/qualification/browser/how-it-works-1280.png` — browser-evidence-screenshot; SHA-256 `a2b5f6efcb810b7728dcaa1dc90f8489cc615a23abe909fc110defe5219ae4aa`
- `evidence/qualification/browser/privacy-1280.png` — browser-evidence-screenshot; SHA-256 `76ffd0e6308fd4b0736943cc1fa81d189a5e5992c69d0d4241a853c64c4ba7d5`
- `evidence/qualification/browser/documentation-320.png` — browser-evidence-screenshot; SHA-256 `d31b383b1e23f5c02fc1ea1a0d804020473eef118d009aa7bccc89b9aa94acaa`
- `evidence/qualification/browser/index-320.png` — browser-evidence-screenshot; SHA-256 `5239314078c6b613f49cb13e6c2b2714a5f2692dfb338d82c12a71ffd15f1502`
- `evidence/qualification/browser/how-it-works-320.png` — browser-evidence-screenshot; SHA-256 `be0a034146134e66cbd343006dbd1bba701d675122055d10b5687d81795a650b`
- `evidence/qualification/browser/documentation-1280.png` — browser-evidence-screenshot; SHA-256 `f17bc6bfbbad19d97526427e431aca1563bf66da76534ee0a4687e9e227be9e3`
- `evidence/qualification/browser/ecosystem-320.png` — browser-evidence-screenshot; SHA-256 `77d8483a8768a4ed9f600dc348cdb77aaadd2c5db9e4404d78e53058e76c26ec`
- `evidence/qualification/browser/ecosystem-1280.png` — browser-evidence-screenshot; SHA-256 `816c5d8d77670518d5936cb486713ab64465d212c6bd809a9ae126642f279c61`

Source snapshot: `evidence/qualification/deproof-source-snapshot.zip`. Local artifact only; imported asset rights remain unresolved. No publishing, deployment, token issuance or mainnet spending occurred.
