# Qualification run record

- Source identity: HEAD `4940ea1b5b953230853310db8a85cb9ae083d2c2` (tracked tree clean at start; untracked raw state excluded).
- Run directory: `evidence/qualification/runs/20261006T125146Z-88018` (raw run state; not committed).
- Command: `bash scripts/qualify.sh` (exit 0).

## Gate results (from evidence/qualification/command-results.jsonl)

| Gate | Exit | Ran at (UTC) | Command |
|---|---:|---|---|
| core-tests | 0 | 12:52:02 | `python3 scripts/qualify-core.py` |
| android-build | 0 | 12:52:15 | `scripts/qualify-android.sh` |
| node-tests | 0 | 12:52:20 | `scripts/qualify-node.sh` |
| prover-tests | 0 | 12:52:24 | `scripts/qualify-prover.sh` |
| verifier-tests | 0 | 12:52:24 | `python3 -m unittest discover -s tools -p test_*.py` |
| localization-catalogs | 0 | 12:52:24 | `python3 scripts/qualify-localization.py` |
| hosting-isolation | 0 | 12:52:35 | `python3 scripts/qualify-hosting.py` |
| node-integration | 0 | 12:53:11 | `env DEPROOF_TEST_HOSTING=1 python3 scripts/qualify-integration.py` |
| rpc-read-only | 0 | 12:53:12 | `python3 scripts/qualify-rpc.py` |
| coverage-reconciliation | 0 | 12:53:13 | `python3 scripts/reconcile-coverage.py` |
| backlog-priorities | 0 | 12:53:13 | `python3 scripts/prioritize-uncovered.py` |
| registry-checks | 0 | 12:53:13 | `python3 scripts/check-registries.py` |
| web-build | 0 | 12:53:13 | `python3 scripts/build-web.py` |
| web-checks | 0 | 12:53:15 | `python3 scripts/qualify-web.py` |
| browser-checks | 0 | 12:53:37 | `node scripts/browser/qualify.mjs` |
| coverage-reconciliation | 0 | 12:53:37 | `python3 scripts/reconcile-coverage.py` |
| backlog-priorities | 0 | 12:53:37 | `python3 scripts/prioritize-uncovered.py` |

## Exact counts and key lines

- core-tests: OK (66 tests)
- verifier-tests: Ran 24 tests in 0.098s
- prover-tests: PASS: local Groth16 relation verified. Verification key trust remains an explicit setup assumption.
- android-build: `testDebugUnitTest` and `assembleDebug` were UP-TO-DATE (same inputs as the last executed build); `lintDebug` executed. Last executed unit-test XML totals in app/build: tests=91, failures=0, errors=0, skipped=0.
- hosting-isolation: 12 PASS lines (FAIL lines: 0).
- node-integration: 23 PASS lines (FAIL lines: 0).

## Registry and levels

- 336 registry IDs present; zero status changes against the source commit.
- BUILD_QUALIFIED: True; CORE_DEVICE_QUALIFIED: False; FULL_SCOPE_QUALIFIED: False.
- Device checks: NOT_RUN (no device attached). Docker runtime: BLOCKED (storage exhausted). Live RPC: read-only probe only.

## Artifact hashes (SHA-256)

- evidence/qualification/implementation-status.json: `f88fb1ecc52ee55524f520556b642fc42ff1df6ba991bb52b13bc945895353f4`
- evidence/qualification/command-results.jsonl: `1e11c9ce06155f39eb5b89aa1b0999fe16295b9af554a8e01c30ec71e34f2e20`
- evidence/qualification/source-after.json: `3826ddb130d76d97177245ad8f352c586d39593f31d49341f46c6286f0a25e32`

## Gate count versus command results

- command-results.jsonl has 17 rows covering 15 distinct gates. coverage-reconciliation and backlog-priorities run twice (before and after report-qualification).

## Executed, cached, device and external

- Executed this run: 15 distinct gates, including core-tests, node-tests, prover-tests, verifier-tests, hosting-isolation, node-integration, rpc-read-only, registry-checks, web-build, web-checks and browser-checks. lintDebug executed.
- Cached this run: testDebugUnitTest and assembleDebug UP-TO-DATE. The last executed unit run (--rerun-tasks, minutes earlier, same inputs) recorded 91 tests, 0 failures.
- Cached artifact, not newly built: app/build/outputs/apk/debug/app-debug.apk, SHA-256 a12f5931f8c16d5863f59b6a07609a6b6c3ebaf3aac319df5cdce4fa8ab6a814.
- Device checks: NOT_RUN (no device attached).
- Docker runtime: BLOCKED (storage).
- Live provider and mainnet: NOT_RUN. No spending.

## Divergence (record only)

- Remote origin/feat/deproof-full-build aa94eea is not merged or referenced here. See evidence/source-identity/branch-divergence-report.md.

## Known limits of this run

- Android gates were up-to-date, not re-executed; their counts come from the last executed build.
- Cited but unbundled artifacts (APK, zip, prover build outputs) are listed in evidence/qualification/unbundled-references.json.
- This record does not establish device, hardware-security, live-RPC, recovery, hosting-matrix, external-provider, or production readiness.
