# Qualification run record

- Source identity: HEAD `4e0e81e623c2cc9bce4f537c6f46d125261fc742` (tracked tree clean at start; untracked raw state excluded).
- Run directory: `evidence/qualification/runs/20261006T110405Z-83574` (raw run state; not committed).
- Command: `bash scripts/qualify.sh` (exit 0).

## Gate results (from evidence/qualification/command-results.jsonl)

| Gate | Exit | Ran at (UTC) | Command |
|---|---:|---|---|
| core-tests | 0 | 11:04:20 | `python3 scripts/qualify-core.py` |
| android-build | 0 | 11:04:26 | `scripts/qualify-android.sh` |
| node-tests | 0 | 11:04:31 | `scripts/qualify-node.sh` |
| prover-tests | 0 | 11:04:34 | `scripts/qualify-prover.sh` |
| verifier-tests | 0 | 11:04:35 | `python3 -m unittest discover -s tools -p test_*.py` |
| localization-catalogs | 0 | 11:04:35 | `python3 scripts/qualify-localization.py` |
| hosting-isolation | 0 | 11:04:44 | `python3 scripts/qualify-hosting.py` |
| node-integration | 0 | 11:05:19 | `env DEPROOF_TEST_HOSTING=1 python3 scripts/qualify-integration.py` |
| rpc-read-only | 0 | 11:05:20 | `python3 scripts/qualify-rpc.py` |
| coverage-reconciliation | 0 | 11:05:20 | `python3 scripts/reconcile-coverage.py` |
| backlog-priorities | 0 | 11:05:20 | `python3 scripts/prioritize-uncovered.py` |
| registry-checks | 0 | 11:05:20 | `python3 scripts/check-registries.py` |
| web-build | 0 | 11:05:21 | `python3 scripts/build-web.py` |
| web-checks | 0 | 11:05:22 | `python3 scripts/qualify-web.py` |
| browser-checks | 0 | 11:05:44 | `node scripts/browser/qualify.mjs` |
| coverage-reconciliation | 0 | 11:05:44 | `python3 scripts/reconcile-coverage.py` |
| backlog-priorities | 0 | 11:05:44 | `python3 scripts/prioritize-uncovered.py` |

## Exact counts and key lines

- core-tests: OK (66 tests)
- verifier-tests: Ran 24 tests in 0.079s
- prover-tests: PASS: local Groth16 relation verified. Verification key trust remains an explicit setup assumption.
- android-build: `testDebugUnitTest` and `assembleDebug` were UP-TO-DATE (same inputs as the last executed build); `lintDebug` executed. Last executed unit-test XML totals in app/build: tests=79, failures=0, errors=0, skipped=0.
- hosting-isolation: 12 PASS lines (FAIL lines: 0).
- node-integration: 23 PASS lines (FAIL lines: 0).

## Registry and levels

- 336 registry IDs present; zero status changes against the source commit.
- BUILD_QUALIFIED: True; CORE_DEVICE_QUALIFIED: False; FULL_SCOPE_QUALIFIED: False.
- Device checks: NOT_RUN (no device attached). Docker runtime: BLOCKED (storage exhausted). Live RPC: read-only probe only.

## Artifact hashes (SHA-256)

- evidence/qualification/implementation-status.json: `414c9f9aba0848392840b11ee384e5749d8135c67e4c0611f917bfe0bed05762`
- evidence/qualification/command-results.jsonl: `0c081e888ff9c84ba118b138ead37f10fb68deed02f403f9f04ea910620c4b98`
- evidence/qualification/source-after.json: `5c3f4190dd9d7294e126d3f2511b18f3833cb00c8f098bcbd833acc0aac3e30b`

## Notes for this run

- Source identity is the commit recorded above. Registry regeneration from this run changed one status against HEAD: EF019 BLOCKED -> IMPLEMENTED_UNVERIFIED (matcher and dispatch gate tested locally; node-integration passed the signed-job discovery, forged-signature refusal and signed-receipt steps). No VERIFIED status was added.
- Uncovered requirements: 281, unchanged.
- Untracked raw evidence (backup/proof logs with local paths, run directories) is not committed.

## Known limits of this run

- Android gates were up-to-date, not re-executed; their counts come from the last executed build.
- Cited but unbundled artifacts (APK, zip, prover build outputs) are listed in evidence/qualification/unbundled-references.json.
- This record does not establish device, hardware-security, live-RPC, recovery, hosting-matrix, external-provider, or production readiness.
