# Qualification run record

- Source identity: HEAD `efcc853ca25f3d87d27f76d7681bb772fcea2f19` (tracked tree clean at start; untracked raw state excluded).
- Run directory: `evidence/qualification/runs/20261006T095457Z-77693` (raw run state; not committed).
- Command: `bash scripts/qualify.sh` (exit 0).

## Gate results (from evidence/qualification/command-results.jsonl)

| Gate | Exit | Ran at (UTC) | Command |
|---|---:|---|---|
| core-tests | 0 | 09:55:10 | `python3 scripts/qualify-core.py` |
| android-build | 0 | 09:55:16 | `scripts/qualify-android.sh` |
| node-tests | 0 | 09:55:21 | `scripts/qualify-node.sh` |
| prover-tests | 0 | 09:55:25 | `scripts/qualify-prover.sh` |
| verifier-tests | 0 | 09:55:25 | `python3 -m unittest discover -s tools -p test_*.py` |
| localization-catalogs | 0 | 09:55:25 | `python3 scripts/qualify-localization.py` |
| hosting-isolation | 0 | 09:55:34 | `python3 scripts/qualify-hosting.py` |
| node-integration | 0 | 09:56:09 | `env DEPROOF_TEST_HOSTING=1 python3 scripts/qualify-integration.py` |
| rpc-read-only | 0 | 09:56:10 | `python3 scripts/qualify-rpc.py` |
| coverage-reconciliation | 0 | 09:56:11 | `python3 scripts/reconcile-coverage.py` |
| backlog-priorities | 0 | 09:56:11 | `python3 scripts/prioritize-uncovered.py` |
| registry-checks | 0 | 09:56:11 | `python3 scripts/check-registries.py` |
| web-build | 0 | 09:56:11 | `python3 scripts/build-web.py` |
| web-checks | 0 | 09:56:13 | `python3 scripts/qualify-web.py` |
| browser-checks | 0 | 09:56:34 | `node scripts/browser/qualify.mjs` |
| coverage-reconciliation | 0 | 09:56:35 | `python3 scripts/reconcile-coverage.py` |
| backlog-priorities | 0 | 09:56:35 | `python3 scripts/prioritize-uncovered.py` |

## Exact counts and key lines

- core-tests: OK (45 tests)
- verifier-tests: Ran 22 tests in 0.075s
- prover-tests: PASS: local Groth16 relation verified. Verification key trust remains an explicit setup assumption.
- android-build: `testDebugUnitTest` and `assembleDebug` were UP-TO-DATE (same inputs as the last executed build); `lintDebug` executed. Last executed unit-test XML totals in app/build: tests=45, failures=0, errors=0, skipped=0.
- hosting-isolation: 12 PASS lines (FAIL lines: 0).
- node-integration: 23 PASS lines (FAIL lines: 0).

## Registry and levels

- 336 registry IDs present; zero status changes against the source commit.
- BUILD_QUALIFIED: True; CORE_DEVICE_QUALIFIED: False; FULL_SCOPE_QUALIFIED: False.
- Device checks: NOT_RUN (no device attached). Docker runtime: BLOCKED (storage exhausted). Live RPC: read-only probe only.

## Artifact hashes (SHA-256)

- evidence/qualification/implementation-status.json: `af145f48d45b7b6a844b702a3c31aee56aadf31d15b1cdebd735f323496c99b6`
- evidence/qualification/command-results.jsonl: `954daa9f5ce121a076371366716b91a22d8312921dc408b8f8e2d48d836d9cc4`
- evidence/qualification/source-after.json: `c8a80e11fb9a01a8bf06456d3ce090006586a8a14a22245e5a257577fdd66772`

## Known limits of this run

- Android gates were up-to-date, not re-executed; their counts come from the last executed build.
- Cited but unbundled artifacts (APK, zip, prover build outputs) are listed in evidence/qualification/unbundled-references.json.
- This record does not establish device, hardware-security, live-RPC, recovery, hosting-matrix, external-provider, or production readiness.
