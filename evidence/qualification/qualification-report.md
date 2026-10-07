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
| localization-catalogs | `python3 scripts/qualify-localization.py` | 0 | PASS | [evidence/qualification/localization-catalogs.log](../../evidence/qualification/localization-catalogs.log) |
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

- features: {"BLOCKED": 19, "IMPLEMENTED_UNVERIFIED": 94, "VERIFIED": 7}
- core-checks: {"NOT_RUN": 80, "PASS": 20}
- functions: {"BLOCKED": 8, "IMPLEMENTED_UNVERIFIED": 50, "VERIFIED": 28}
- ecosystem-requirements: {"BLOCKED": 6, "IMPLEMENTED_UNVERIFIED": 24}

Exact counts remain 120 F, 100 C, 62 FN + 24 EF = 86 contracts and 30 E. `uncovered-requirements.json` enumerates each unresolved acceptance with paths/tests/evidence; local-profile verification never implies external/device qualification.

## Unresolved qualification gates

- **DEVICE_WALLET_HARDWARE: NOT_RUN** — ADB lists no connected physical device. MWA signing, Keystore protection/security level, Room-on-device migration and process-death/camera tests remain unrun.
- **HOST_SUPPORT_MATRIX: BLOCKED** — Only the documented constrained HTTP profile on this local Linux Docker VM is qualified. The owner-trusted Docker engine is rootful; rootless engines, native Linux/ARM64 targets and arbitrary services are unqualified.
- **EXTERNAL_PEER_PROVIDER: NOT_RUN** — Real loopback TLS pairing and controlled known-byte receiver passed locally; remote peer tunnels, NAT/mobile network transitions, provider corroboration and payouts remain untested.
- **PROOF_PRODUCTION_SETUP: NOT_RUN** — Owner-signed local cubic jobs and separate gnark verifier pass. Educational trusted setup and same-owner verifier domain; isolated production proof execution, external jobs/provider membership/rewards remain unqualified.
- **P5_EXTERNAL_ADAPTERS: BLOCKED** — Verified SKR staking layouts and additional provider authorization/protocol interfaces unavailable. No mainnet spending.
- **SOURCE_COMPLETENESS: NOT_RUN** — Full lookup resolution, legacy receipt import, unsupported media previews/capture lifecycle, device validation of newly implemented consented location/offline mapping/reminders, complete dynamic translations, background network policy and physical document-provider/crash acceptance for bounded Android backups remain incomplete. Bounded text/image previews are implemented but not device-qualified. Each unresolved registry entry is listed in uncovered-requirements.json.
- **RELEASE_AND_RIGHTS: BLOCKED** — No release signing or physical-device release checks; comprehensive SBOM/advisory and imported-asset rights review incomplete.
- **EXPLANATION_PROVIDER: BLOCKED** — Typed consent/configuration/error guard exists; no qualified external explanation adapter or outbound request is registered. Guard tests do not establish provider integration.
- **MANUAL_ACCESSIBILITY: NOT_RUN** — Automated Chromium/axe/keyboard/200% text checks do not establish full manual screen-reader, cross-browser or Android accessibility qualification.
- **CONTAINER_QUALIFICATION: BLOCKED** — See evidence/docker/qualification-summary.json. Service images and portable checks passed; Docker storage is exhausted, Android cache ownership correction is untested, container APK absent and final owner-control revisions remain unqualified. Host Android build passes do not establish container build success.

## Current continuation

Authenticated bounded Room backup/export/additive restore UI, raw-file metadata association and fresh observed local proof-profile matching are wired; supporting domain/Room tests are recorded, physical document-provider/crash and Android-to-Linux dispatch remain NOT_RUN. Backup excludes private signing keys, credentials and active permissions; see docs/backup-and-proof-wiring.md. Consented foreground location with optional hash-bound metadata, private offline mapping plans and opt-in inexact task reminders are implemented. English/Spanish resource catalogs and persisted UI language selection are implemented, but full dynamic explanation localization and independent/device review remain incomplete. See docs/local-workflows.md and evidence/qualification/localization-report.json. Bounded private text/image previews, persisted first-seen program indicators, the legacy raw digest signing contract and bounded read-only account/rent explanations are implemented; supporting local tests pass, Android hardware/lifecycle acceptance remains NOT_RUN. Optional explanation consent/configuration/error guards are tested, but the external provider adapter remains BLOCKED. F097 now correctly links existing one-rebuild memo logic and fresh-review enforcement. No device pass is inferred.

Dependency/security/environment priorities: `evidence/qualification/prioritized-backlog.json`; code/evidence gaps and next steps: `docs/remaining-work.md`. Physical preflight: `python3 scripts/qualify-device.py`; install only with `--install` and follow `docs/device-qualification.md`.

## Produced artifacts

- `app/build/outputs/apk/debug/app-debug.apk` — debug-apk; SHA-256 `6ca1f419036b40228f2d99fc753469e1322d2c10c5a6c27bed48db97ae023def`
- `node-agent/build/deproof-node` — darwin-amd64-node-binary; SHA-256 `9e8836fe5d858450f1ac4244d30b39cb94ea7cf26a3ae7a6f0f644e52b7406a5`
- `prover-worker/build/prove` — local-proof-producer; SHA-256 `42d33d74471a2e84ea44b2197fb87d1fa985816dc911435ef30e7341dda68d5a`
- `prover-worker/build/verify` — independent-proof-verifier; SHA-256 `0011598abd70624a26ef8cade51b605641b3f8c043872ff1643550fd4e170bd5`
- `gradle/wrapper/gradle-wrapper.jar` — gradle-wrapper; SHA-256 `b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13`
- `tools/verify.py` — independent-receipt-verifier; SHA-256 `dd17a4b493cd1d11e700879a6023308daacb6315fb665e4bb5665041e6f1195a`
- `prover-worker/build/qualified-result-20261006T172803Z-30786/circuit.r1cs` — local-proof-artifact; SHA-256 `d66951a99a8361b0677e6e08a8075773c1622f33645b2b1182ee60db39ef8954`
- `prover-worker/build/qualified-result-20261006T172803Z-30786/public-witness.bin` — local-proof-artifact; SHA-256 `da07f711885f5cdbf5a86f5e0c2d503f1cf6cf1ffe71d86cff6a4566b8c833cd`
- `prover-worker/build/qualified-result-20261006T172803Z-30786/proof.bin` — local-proof-artifact; SHA-256 `c501257f8809b703d7787b4b266a11903f70e357908444cb8a9c87679bc70483`
- `prover-worker/build/qualified-result-20261006T172803Z-30786/manifest.json` — local-proof-artifact; SHA-256 `477e8bb1960662ebce11a63d188bec4ad5350522815531857659df50d364fb92`
- `prover-worker/build/qualified-result-20261006T172803Z-30786/verification-key.bin` — local-proof-artifact; SHA-256 `3b9f62c70c00dfa7291d805ba888f5b0f864a53a76223aec64dd34c5743c37f7`
- `prover-worker/build/qualified-result-20261006T172803Z-30786/proving-key.bin` — local-proof-artifact; SHA-256 `976cb3264944cdea9c86d34fd6cd1bc21f6013b24f5d894b0f720e43c50e7f1e`
- `evidence/qualification/browser/index-320.png` — browser-evidence-screenshot; SHA-256 `391521697de518b00974d4589583d08f8c6be37af2b529ebc28b0dd953e81f64`
- `evidence/qualification/browser/index-1280.png` — browser-evidence-screenshot; SHA-256 `85cfcc939ff6e24fc539bb3813cba35076eba6aa83c6c1d4931bc58fcf5ac93f`
- `evidence/qualification/browser/project-1280.png` — browser-evidence-screenshot; SHA-256 `05accf7191081f07fd73ee62c33b2fc13876941816776c273fffe0749af2d3d3`
- `evidence/qualification/browser/ecosystem-1280.png` — browser-evidence-screenshot; SHA-256 `25844a0337b238c7f355650524cb90af25825f274de5b15aced078ef7004139d`
- `evidence/qualification/browser/how-it-works-320.png` — browser-evidence-screenshot; SHA-256 `f0f7e5d7c024ab2b561317cd0df6e5215e1676ea38e2d40b163966c85d2837d1`
- `evidence/qualification/browser/ecosystem-320.png` — browser-evidence-screenshot; SHA-256 `527711d0d7efb96ccf1a9b9777ecfe41d1bbd8ba272523070f4817880ed5788f`
- `evidence/qualification/browser/project-320.png` — browser-evidence-screenshot; SHA-256 `d8a4e22a8d566728ed2a3b252e7bbabe45e2ac8d5770a198a94cd4b22106f0dc`
- `evidence/qualification/browser/features-1280.png` — browser-evidence-screenshot; SHA-256 `a3624734c692bf45a325ed7fdb741763c3e5dd36a3d5176a84ed15e47bb07860`
- `evidence/qualification/browser/documentation-1280.png` — browser-evidence-screenshot; SHA-256 `de4f552a2fc2c865899e9c557b265300d1c6126f5d30319c66881c2b1d0de637`
- `evidence/qualification/browser/privacy-1280.png` — browser-evidence-screenshot; SHA-256 `99d5faebb0a825590e23b71ee1e26c2b0119c4be3bc7ed9bbcdad81cd19e6008`
- `evidence/qualification/browser/privacy-320.png` — browser-evidence-screenshot; SHA-256 `8bd64c61271a0f5444431dd1c03292e4de9488ed2e9a8c884189b58929ae0e49`
- `evidence/qualification/browser/features-320.png` — browser-evidence-screenshot; SHA-256 `0d2f9194cddd1a4e82bc5712cb88f1f5246d64b5aef3b486f68247c7671185d1`
- `evidence/qualification/browser/how-it-works-1280.png` — browser-evidence-screenshot; SHA-256 `ef226151c647d387bf497e0126a43fd1a5b7b0acc7f8d6a2e8e85825126be847`
- `evidence/qualification/browser/documentation-320.png` — browser-evidence-screenshot; SHA-256 `adad8dbd3b85da01c12140a69a4f3986584076b51791f1314693aa573cec7818`

Source snapshot: `evidence/qualification/deproof-source-snapshot.zip`. Local artifact only; imported asset rights remain unresolved. No publishing, deployment, token issuance or mainnet spending occurred.

## Container development qualification

Container scope: **BLOCKED**. Detailed commands, pinned images, actual builds and blockers: `evidence/docker/qualification-summary.json` and `evidence/docker/qualification-report.md`. No container APK is claimed; existing host APK qualification remains separate. All 336 registry IDs and unavailable device/external gates are preserved.
