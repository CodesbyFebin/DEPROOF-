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
| core-tests | `python3 scripts/qualify-core.py` | 0 | PASS | [evidence/qualification/core-tests.log](../evidence/qualification/core-tests.log) |
| android-build | `scripts/qualify-android.sh` | 0 | PASS | [evidence/qualification/android-build.log](../evidence/qualification/android-build.log) |
| node-tests | `scripts/qualify-node.sh` | 0 | PASS | [evidence/qualification/node-tests.log](../evidence/qualification/node-tests.log) |
| prover-tests | `scripts/qualify-prover.sh` | 0 | PASS | [evidence/qualification/prover-tests.log](../evidence/qualification/prover-tests.log) |
| verifier-tests | `python3 -m unittest discover -s tools -p test_*.py` | 0 | PASS | [evidence/qualification/verifier-tests.log](../evidence/qualification/verifier-tests.log) |
| localization-catalogs | `python3 scripts/qualify-localization.py` | 0 | PASS | [evidence/qualification/localization-catalogs.log](../evidence/qualification/localization-catalogs.log) |
| hosting-isolation | `python3 scripts/qualify-hosting.py` | 0 | PASS | [evidence/qualification/hosting-isolation.log](../evidence/qualification/hosting-isolation.log) |
| node-integration | `env DEPROOF_TEST_HOSTING=1 python3 scripts/qualify-integration.py` | 0 | PASS | [evidence/qualification/node-integration.log](../evidence/qualification/node-integration.log) |
| rpc-read-only | `python3 scripts/qualify-rpc.py` | 0 | PASS | [evidence/qualification/rpc-read-only.log](../evidence/qualification/rpc-read-only.log) |
| coverage-reconciliation | `python3 scripts/reconcile-coverage.py` | 0 | PASS | [evidence/qualification/coverage-reconciliation.log](../evidence/qualification/coverage-reconciliation.log) |
| backlog-priorities | `python3 scripts/prioritize-uncovered.py` | 0 | PASS | [evidence/qualification/backlog-priorities.log](../evidence/qualification/backlog-priorities.log) |
| registry-checks | `python3 scripts/check-registries.py` | 0 | PASS | [evidence/qualification/registry-checks.log](../evidence/qualification/registry-checks.log) |
| web-build | `python3 scripts/build-web.py` | 0 | PASS | [evidence/qualification/web-build.log](../evidence/qualification/web-build.log) |
| web-checks | `python3 scripts/qualify-web.py` | 0 | PASS | [evidence/qualification/web-checks.log](../evidence/qualification/web-checks.log) |
| browser-checks | `node scripts/browser/qualify.mjs` | 0 | PASS | [evidence/qualification/browser-checks.log](../evidence/qualification/browser-checks.log) |
| coverage-reconciliation | `python3 scripts/reconcile-coverage.py` | 0 | PASS | [evidence/qualification/coverage-reconciliation.log](../evidence/qualification/coverage-reconciliation.log) |
| backlog-priorities | `python3 scripts/prioritize-uncovered.py` | 0 | PASS | [evidence/qualification/backlog-priorities.log](../evidence/qualification/backlog-priorities.log) |

Android script executes `bash ./gradlew testDebugUnitTest --stacktrace`, `bash ./gradlew assembleDebug --stacktrace`, and `bash ./gradlew lintDebug --stacktrace`. It hashes the APK only after these tasks succeed.

Historical failures: sandbox cache/native-library/socket and DNS restrictions were cleared through approved execution. Source failures were the MWA shared namespace and unguarded StrongBox exception. The live RPC probe additionally exposed shortened genesis constants; full-hash checks now have regression tests. Historical failures are preserved in prior run logs.

## Registry coverage

- features: {"BLOCKED": 19, "IMPLEMENTED_UNVERIFIED": 94, "VERIFIED": 7}
- core-checks: {"NOT_RUN": 80, "PASS": 20}
- functions: {"BLOCKED": 9, "IMPLEMENTED_UNVERIFIED": 49, "VERIFIED": 28}
- ecosystem-requirements: {"BLOCKED": 6, "IMPLEMENTED_UNVERIFIED": 24}

Exact counts remain 120 F, 100 C, 62 FN + 24 EF = 86 contracts and 30 E. `uncovered-requirements.json` enumerates each unresolved acceptance with paths/tests/evidence; local-profile verification never implies external/device qualification.

## Unresolved qualification gates

- **DEVICE_WALLET_HARDWARE: NOT_RUN** — ADB lists no connected physical device. MWA signing, Keystore protection/security level, Room-on-device migration and process-death/camera tests remain unrun.
- **HOST_SUPPORT_MATRIX: BLOCKED** — Only the documented constrained HTTP profile on this local Linux Docker VM is qualified. The owner-trusted Docker engine is rootful; rootless engines, native Linux/ARM64 targets and arbitrary services are unqualified.
- **EXTERNAL_PEER_PROVIDER: NOT_RUN** — Real loopback TLS pairing and controlled known-byte receiver passed locally; remote peer tunnels, NAT/mobile network transitions, provider corroboration and payouts remain untested.
- **PROOF_PRODUCTION_SETUP: NOT_RUN** — Owner-signed local cubic jobs and separate gnark verifier pass. Educational trusted setup and same-owner verifier domain; isolated production proof execution, external jobs/provider membership/rewards remain unqualified.
- **P5_EXTERNAL_ADAPTERS: BLOCKED** — Verified SKR staking layouts and additional provider authorization/protocol interfaces unavailable. No mainnet spending.
- **SOURCE_COMPLETENESS: NOT_RUN** — Full lookup resolution, legacy receipt import, unsupported media previews/capture lifecycle, device validation of newly implemented consented location/offline mapping/reminders, complete dynamic translations, background network policy and complete Android backup integration remain incomplete. Bounded text/image previews are implemented but not device-qualified. Each unresolved registry entry is listed in uncovered-requirements.json.
- **RELEASE_AND_RIGHTS: BLOCKED** — No release signing or physical-device release checks; comprehensive SBOM/advisory and imported-asset rights review incomplete.
- **EXPLANATION_PROVIDER: BLOCKED** — Typed consent/configuration/error guard exists; no qualified external explanation adapter or outbound request is registered. Guard tests do not establish provider integration.
- **MANUAL_ACCESSIBILITY: NOT_RUN** — Automated Chromium/axe/keyboard/200% text checks do not establish full manual screen-reader, cross-browser or Android accessibility qualification.
- **CONTAINER_QUALIFICATION: BLOCKED** — See evidence/docker/qualification-summary.json. Service images and portable checks passed; Docker storage is exhausted, Android cache ownership correction is untested, container APK absent and final owner-control revisions remain unqualified. Host Android build passes do not establish container build success.

## Current continuation

Consented foreground location with optional hash-bound metadata, private offline mapping plans and opt-in inexact task reminders are implemented. English/Spanish resource catalogs and persisted UI language selection are implemented, but full dynamic explanation localization and independent/device review remain incomplete. See docs/local-workflows.md and evidence/qualification/localization-report.json. Bounded private text/image previews, persisted first-seen program indicators, the legacy raw digest signing contract and bounded read-only account/rent explanations are implemented; supporting local tests pass, Android hardware/lifecycle acceptance remains NOT_RUN. Optional explanation consent/configuration/error guards are tested, but the external provider adapter remains BLOCKED. F097 now correctly links existing one-rebuild memo logic and fresh-review enforcement. No device pass is inferred.

Dependency/security/environment priorities: `evidence/qualification/prioritized-backlog.json`; code/evidence gaps and next steps: `docs/remaining-work.md`. Physical preflight: `python3 scripts/qualify-device.py`; install only with `--install` and follow `docs/device-qualification.md`.

## Produced artifacts

- `app/build/outputs/apk/debug/app-debug.apk` — debug-apk; SHA-256 `64cd7d377a33385156013c61c61528ac7a5f07690fe7d340d19eeafd5ce72ab2`
- `node-agent/build/deproof-node` — darwin-amd64-node-binary; SHA-256 `780d0a2d360425d585bfe26216341f6b005c30cb8031f17adcc3281945b088b2`
- `prover-worker/build/prove` — local-proof-producer; SHA-256 `6827a818c67677e243f92a9c3759d5429cd73a5a8afe36c95cab5eb7602b0985`
- `prover-worker/build/verify` — independent-proof-verifier; SHA-256 `be80d05954fb7e0bc0107ea0a8fc5d6960c15a43c2c9c200fe9f057f39839621`
- `gradle/wrapper/gradle-wrapper.jar` — gradle-wrapper; SHA-256 `b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13`
- `tools/verify.py` — independent-receipt-verifier; SHA-256 `dd17a4b493cd1d11e700879a6023308daacb6315fb665e4bb5665041e6f1195a`
- `prover-worker/build/qualified-result-20261006T094220Z-76453/circuit.r1cs` — local-proof-artifact; SHA-256 `d66951a99a8361b0677e6e08a8075773c1622f33645b2b1182ee60db39ef8954`
- `prover-worker/build/qualified-result-20261006T094220Z-76453/verification-key.bin` — local-proof-artifact; SHA-256 `62b7dae0dc23fb0355826f15df2ccb0b8c094b0f3828537de0c56545d1c7554b`
- `prover-worker/build/qualified-result-20261006T094220Z-76453/proving-key.bin` — local-proof-artifact; SHA-256 `ec1063a6a3ae08937ec0b0896f0f54547c94a9f8906d8e6df114d090752e2baf`
- `prover-worker/build/qualified-result-20261006T094220Z-76453/public-witness.bin` — local-proof-artifact; SHA-256 `da07f711885f5cdbf5a86f5e0c2d503f1cf6cf1ffe71d86cff6a4566b8c833cd`
- `prover-worker/build/qualified-result-20261006T094220Z-76453/manifest.json` — local-proof-artifact; SHA-256 `e9a6112a336a876c83625548c8806ef04f05b834acbef6dd7cddc5072d55b627`
- `prover-worker/build/qualified-result-20261006T094220Z-76453/proof.bin` — local-proof-artifact; SHA-256 `11da504348dc2b415c460ccd0c543a6ad19b04db2701bff781e216df22eabb88`
- `prover-worker/build/qualified-result-20261006T084501Z-72561/circuit.r1cs` — local-proof-artifact; SHA-256 `d66951a99a8361b0677e6e08a8075773c1622f33645b2b1182ee60db39ef8954`
- `prover-worker/build/qualified-result-20261006T084501Z-72561/verification-key.bin` — local-proof-artifact; SHA-256 `e77e6d1bf6ef7692a861a937e2230c872e3ac3c8c0cec711ab41f68a55a2cf8a`
- `prover-worker/build/qualified-result-20261006T084501Z-72561/proving-key.bin` — local-proof-artifact; SHA-256 `7fc3217aca39b8b2882e10d2223fc3bdd27470d9ad5cccab54981462c08c8a13`
- `prover-worker/build/qualified-result-20261006T084501Z-72561/public-witness.bin` — local-proof-artifact; SHA-256 `da07f711885f5cdbf5a86f5e0c2d503f1cf6cf1ffe71d86cff6a4566b8c833cd`
- `prover-worker/build/qualified-result-20261006T084501Z-72561/manifest.json` — local-proof-artifact; SHA-256 `6a3274473c44a8393881833dde20ad861e547753ae970a0eafdd494b4c9652c8`
- `prover-worker/build/qualified-result-20261006T084501Z-72561/proof.bin` — local-proof-artifact; SHA-256 `bda045c863aafddb852a455373b8349ac33ef9cd663bf1c3158aee86586cf871`
- `prover-worker/build/qualified-result-20261006T100018Z-78325/circuit.r1cs` — local-proof-artifact; SHA-256 `d66951a99a8361b0677e6e08a8075773c1622f33645b2b1182ee60db39ef8954`
- `prover-worker/build/qualified-result-20261006T100018Z-78325/verification-key.bin` — local-proof-artifact; SHA-256 `5341af0da53a647338bd259bdad3c15b0a3ef5524ba4456cc03839a8e1f9182c`
- `prover-worker/build/qualified-result-20261006T100018Z-78325/proving-key.bin` — local-proof-artifact; SHA-256 `b21738cd09c4a2f28d3af31a7fa77e5c1d3d7670fd5899c8468273fd56c924ae`
- `prover-worker/build/qualified-result-20261006T100018Z-78325/public-witness.bin` — local-proof-artifact; SHA-256 `da07f711885f5cdbf5a86f5e0c2d503f1cf6cf1ffe71d86cff6a4566b8c833cd`
- `prover-worker/build/qualified-result-20261006T100018Z-78325/manifest.json` — local-proof-artifact; SHA-256 `cb88899cc7af43ddef0e1f05a5d83c99ab6fecad03029a29aa111fb21599491d`
- `prover-worker/build/qualified-result-20261006T100018Z-78325/proof.bin` — local-proof-artifact; SHA-256 `59f291371a5abd6ce22aa79c077c3f0868fe6fc124b69147fa5e24ea3467b0e1`
- `prover-worker/build/qualified-result-20261006T071930Z-66799/circuit.r1cs` — local-proof-artifact; SHA-256 `d66951a99a8361b0677e6e08a8075773c1622f33645b2b1182ee60db39ef8954`
- `prover-worker/build/qualified-result-20261006T071930Z-66799/verification-key.bin` — local-proof-artifact; SHA-256 `6a5ce2c8fa9a04a408847f60b7717c4aad2e46098ebb6e66cc6468c1f55612c1`
- `prover-worker/build/qualified-result-20261006T071930Z-66799/proving-key.bin` — local-proof-artifact; SHA-256 `6852f327a30a044935650da49fc7066d6bd86030477e3de32d3881d350d15a2a`
- `prover-worker/build/qualified-result-20261006T071930Z-66799/public-witness.bin` — local-proof-artifact; SHA-256 `da07f711885f5cdbf5a86f5e0c2d503f1cf6cf1ffe71d86cff6a4566b8c833cd`
- `prover-worker/build/qualified-result-20261006T071930Z-66799/manifest.json` — local-proof-artifact; SHA-256 `b57726525b8b8eb297051bf9cbd0d8ef9cf38ff4049baee18214ac1c4813ada8`
- `prover-worker/build/qualified-result-20261006T071930Z-66799/proof.bin` — local-proof-artifact; SHA-256 `fe388e0efe9104cd6cec07caecfcab1251f3412517acaf448e8bae076c1787b3`
- `prover-worker/build/qualified-result-20261006T093752Z-75899/circuit.r1cs` — local-proof-artifact; SHA-256 `d66951a99a8361b0677e6e08a8075773c1622f33645b2b1182ee60db39ef8954`
- `prover-worker/build/qualified-result-20261006T093752Z-75899/verification-key.bin` — local-proof-artifact; SHA-256 `f2aa47e1aa41c1bfccdd3cd33879b119fb72d47e0cfe85f4e69ebfe65fb1819c`
- `prover-worker/build/qualified-result-20261006T093752Z-75899/proving-key.bin` — local-proof-artifact; SHA-256 `05463a3188afdd8bc58ffca1770d5e64d3f660dea75b73899841f7d99e186e4f`
- `prover-worker/build/qualified-result-20261006T093752Z-75899/public-witness.bin` — local-proof-artifact; SHA-256 `da07f711885f5cdbf5a86f5e0c2d503f1cf6cf1ffe71d86cff6a4566b8c833cd`
- `prover-worker/build/qualified-result-20261006T093752Z-75899/manifest.json` — local-proof-artifact; SHA-256 `d279da0870c10465245f373fbcc656770b48217223945b47fb9ce6efbbb11bf7`
- `prover-worker/build/qualified-result-20261006T093752Z-75899/proof.bin` — local-proof-artifact; SHA-256 `154216995f500ffebb1023153476eeefee4ec62940dedb323ce008cdee274d8b`
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
- `prover-worker/build/qualified-result-20261006T095525Z-77774/circuit.r1cs` — local-proof-artifact; SHA-256 `d66951a99a8361b0677e6e08a8075773c1622f33645b2b1182ee60db39ef8954`
- `prover-worker/build/qualified-result-20261006T095525Z-77774/verification-key.bin` — local-proof-artifact; SHA-256 `211a029b94e0de158973f06b2de625a89d5aa0e071493a5b1eeea27a9566424e`
- `prover-worker/build/qualified-result-20261006T095525Z-77774/proving-key.bin` — local-proof-artifact; SHA-256 `6135521d16464d8671f5bb9b9c010caa270bfdeb57de02b25e5797d53baf7286`
- `prover-worker/build/qualified-result-20261006T095525Z-77774/public-witness.bin` — local-proof-artifact; SHA-256 `da07f711885f5cdbf5a86f5e0c2d503f1cf6cf1ffe71d86cff6a4566b8c833cd`
- `prover-worker/build/qualified-result-20261006T095525Z-77774/manifest.json` — local-proof-artifact; SHA-256 `0aa53e443dd4ab5f5f47ad80730554444615bd6508acf6fcd8d0d01c143a4e92`
- `prover-worker/build/qualified-result-20261006T095525Z-77774/proof.bin` — local-proof-artifact; SHA-256 `18040ea62c499715b6115646ccde9ff0b8203ddd03f2030f3ff93899876ba773`
- `prover-worker/build/qualified-result-20261006T054637Z-62530/circuit.r1cs` — local-proof-artifact; SHA-256 `d66951a99a8361b0677e6e08a8075773c1622f33645b2b1182ee60db39ef8954`
- `prover-worker/build/qualified-result-20261006T054637Z-62530/verification-key.bin` — local-proof-artifact; SHA-256 `6e04904836a36146a22f927befc1bbf1f5292771e2fbeb809ca0ba7833dd7a00`
- `prover-worker/build/qualified-result-20261006T054637Z-62530/proving-key.bin` — local-proof-artifact; SHA-256 `bfed8ebc058132c7e65bd39e1b8a483ef8da56b6a0191af57f97c94a30bf20fa`
- `prover-worker/build/qualified-result-20261006T054637Z-62530/public-witness.bin` — local-proof-artifact; SHA-256 `da07f711885f5cdbf5a86f5e0c2d503f1cf6cf1ffe71d86cff6a4566b8c833cd`
- `prover-worker/build/qualified-result-20261006T054637Z-62530/manifest.json` — local-proof-artifact; SHA-256 `82c101b179c083a1076b071cf6816ac3fab02d2d499789773f395b55ba4dca7d`
- `prover-worker/build/qualified-result-20261006T054637Z-62530/proof.bin` — local-proof-artifact; SHA-256 `c35e892937dd6caa065f226ba0ff414893ecd1e241e1b65b18bb78efc53232d1`
- `prover-worker/build/qualified-result-20261006T084931Z-72962/circuit.r1cs` — local-proof-artifact; SHA-256 `d66951a99a8361b0677e6e08a8075773c1622f33645b2b1182ee60db39ef8954`
- `prover-worker/build/qualified-result-20261006T084931Z-72962/verification-key.bin` — local-proof-artifact; SHA-256 `14fa3818b2d72a10b8b8bbc720b60b68c1a4a68923090593b642382f7755fbf9`
- `prover-worker/build/qualified-result-20261006T084931Z-72962/proving-key.bin` — local-proof-artifact; SHA-256 `c072a2f91b7ad9f2b4bff9521223d6d4f9685e94f60452e779f9a78ceef22dd6`
- `prover-worker/build/qualified-result-20261006T084931Z-72962/public-witness.bin` — local-proof-artifact; SHA-256 `da07f711885f5cdbf5a86f5e0c2d503f1cf6cf1ffe71d86cff6a4566b8c833cd`
- `prover-worker/build/qualified-result-20261006T084931Z-72962/manifest.json` — local-proof-artifact; SHA-256 `889489927926e6e54943b58da9bf191174a54f3b7741ef35702fcb2fe7e84a1d`
- `prover-worker/build/qualified-result-20261006T084931Z-72962/proof.bin` — local-proof-artifact; SHA-256 `dcc29a780b8cdc1fe439817d9fa4b0af7d3ace1d932b4f5dc53de85797cee0ea`
- `prover-worker/build/qualified-result-20261006T061453Z-64386/circuit.r1cs` — local-proof-artifact; SHA-256 `d66951a99a8361b0677e6e08a8075773c1622f33645b2b1182ee60db39ef8954`
- `prover-worker/build/qualified-result-20261006T061453Z-64386/verification-key.bin` — local-proof-artifact; SHA-256 `873462258f3bedeba5fdd75c697158569bed5fadc12c154f31e80afa4e12d400`
- `prover-worker/build/qualified-result-20261006T061453Z-64386/proving-key.bin` — local-proof-artifact; SHA-256 `4f9428c2b4a8c1f0052eee76f67a20d53aadf12a041980397efb66b29636dc8c`
- `prover-worker/build/qualified-result-20261006T061453Z-64386/public-witness.bin` — local-proof-artifact; SHA-256 `da07f711885f5cdbf5a86f5e0c2d503f1cf6cf1ffe71d86cff6a4566b8c833cd`
- `prover-worker/build/qualified-result-20261006T061453Z-64386/manifest.json` — local-proof-artifact; SHA-256 `5e1492e284c5b38860174a4dbab63921bdfba1ff86c04c1fff6fbdbeebf860fa`
- `prover-worker/build/qualified-result-20261006T061453Z-64386/proof.bin` — local-proof-artifact; SHA-256 `5f57012566d84fdafdb546890525f78530199298b1d4296b4e5ea1942475de32`
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
- `evidence/qualification/browser/features-1280.png` — browser-evidence-screenshot; SHA-256 `0c4b2d2d0b70188870ce6003e1c25ed78b0d20d00fab904adf23d49d10a2aafc`
- `evidence/qualification/browser/privacy-320.png` — browser-evidence-screenshot; SHA-256 `9ed442b992d42fce87321c44e7e52076180c531592e5172c6ac580ef9fd28113`
- `evidence/qualification/browser/features-320.png` — browser-evidence-screenshot; SHA-256 `68abd32a0b1a8a829977621a9574364f23c89d67c21558d12b02e12e8b15a876`
- `evidence/qualification/browser/index-1280.png` — browser-evidence-screenshot; SHA-256 `5904b1ee93c81617000b0387fc5ff73bac934b0b7bafb9d83b59add55929d648`
- `evidence/qualification/browser/project-1280.png` — browser-evidence-screenshot; SHA-256 `b6713e07475e5906048508c7df8ab95ceb24dac50f6c27b6b1def73b9406eceb`
- `evidence/qualification/browser/project-320.png` — browser-evidence-screenshot; SHA-256 `4c44231fea5f351a299662ef6e8a3a63a4e2cfbe3181714d78e5dedce30730f1`
- `evidence/qualification/browser/how-it-works-1280.png` — browser-evidence-screenshot; SHA-256 `c72d1c702972d12febffe22c5dc5bb13d882825ebcd93d41960e5c63bd371704`
- `evidence/qualification/browser/privacy-1280.png` — browser-evidence-screenshot; SHA-256 `4c78eb299e4ae9ff4094766329f1052beca8c34810ce13021a14699a6b12defe`
- `evidence/qualification/browser/documentation-320.png` — browser-evidence-screenshot; SHA-256 `701f97ea96e0921d12e2e977bb7f3ed9a2cfe89eabb952667fd15617ecf5749f`
- `evidence/qualification/browser/index-320.png` — browser-evidence-screenshot; SHA-256 `0ee1b1a5dceabefff0451f02f4af9bb148980027dd2c8405c816f75ed7d693e1`
- `evidence/qualification/browser/how-it-works-320.png` — browser-evidence-screenshot; SHA-256 `ee686090002176ddf823f98109d425c71c19093b79c25911a80ef68e78771596`
- `evidence/qualification/browser/documentation-1280.png` — browser-evidence-screenshot; SHA-256 `1b021f9011cf60d58f7a881623bcd9e197c2d37bfa9fee13171e60e79c133d77`
- `evidence/qualification/browser/ecosystem-320.png` — browser-evidence-screenshot; SHA-256 `bb7265be8b5118aa6f455a6c4448d781f66ec7476bad95eab7cd52945bf40325`
- `evidence/qualification/browser/ecosystem-1280.png` — browser-evidence-screenshot; SHA-256 `13de3f001221a2244e6c2a00b6c28cdd115bdb3a3cc61ec5c9e40b3160bf0d7a`

Source snapshot: `evidence/qualification/deproof-source-snapshot.zip`. Local artifact only; imported asset rights remain unresolved. No publishing, deployment, token issuance or mainnet spending occurred.

## Container development qualification

Container scope: **BLOCKED**. Detailed commands, pinned images, actual builds and blockers: `evidence/docker/qualification-summary.json` and `evidence/docker/qualification-report.md`. No container APK is claimed; existing host APK qualification remains separate. All 336 registry IDs and unavailable device/external gates are preserved.
