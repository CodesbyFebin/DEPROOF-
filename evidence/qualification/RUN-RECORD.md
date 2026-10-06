# Qualification run record

- Source identity: HEAD `611045e5` (tracked tree clean at start; untracked raw state excluded).
- Run directory: `evidence/qualification/runs/20261006T103121Z-80605` (raw run state; not committed).
- Command: `bash scripts/qualify.sh` (exit 0).

## Gate results (from evidence/qualification/command-results.jsonl)

| Gate | Exit | Ran at (UTC) | Command |
|---|---:|---|---|
| core-tests | 0 | 10:31:36 | `python3 scripts/qualify-core.py` |
| android-build | 0 | 10:31:46 | `scripts/qualify-android.sh` |
| node-tests | 0 | 10:31:51 | `scripts/qualify-node.sh` |
| prover-tests | 0 | 10:31:55 | `scripts/qualify-prover.sh` |
| verifier-tests | 0 | 10:31:55 | `python3 -m unittest discover -s tools -p test_*.py` |
| localization-catalogs | 0 | 10:31:56 | `python3 scripts/qualify-localization.py` |
| hosting-isolation | 0 | 10:32:05 | `python3 scripts/qualify-hosting.py` |
| node-integration | 0 | 10:32:40 | `env DEPROOF_TEST_HOSTING=1 python3 scripts/qualify-integration.py` |
| rpc-read-only | 0 | 10:32:41 | `python3 scripts/qualify-rpc.py` |
| coverage-reconciliation | 0 | 10:32:42 | `python3 scripts/reconcile-coverage.py` |
| backlog-priorities | 0 | 10:32:42 | `python3 scripts/prioritize-uncovered.py` |
| registry-checks | 0 | 10:32:42 | `python3 scripts/check-registries.py` |
| web-build | 0 | 10:32:42 | `python3 scripts/build-web.py` |
| web-checks | 0 | 10:32:44 | `python3 scripts/qualify-web.py` |
| browser-checks | 0 | 10:33:05 | `node scripts/browser/qualify.mjs` |
| coverage-reconciliation | 0 | 10:33:05 | `python3 scripts/reconcile-coverage.py` |
| backlog-priorities | 0 | 10:33:05 | `python3 scripts/prioritize-uncovered.py` |

## Exact counts and key lines

- core-tests: OK (62 tests)
- verifier-tests: Ran 22 tests in 0.079s
- prover-tests: PASS: local Groth16 relation verified. Verification key trust remains an explicit setup assumption.
- android-build: `testDebugUnitTest` and `assembleDebug` were UP-TO-DATE (same inputs as the last executed build); `lintDebug` executed. Last executed unit-test XML totals in app/build: tests=66, failures=0, errors=0, skipped=0.
- hosting-isolation: 12 PASS lines (FAIL lines: 0).
- node-integration: 23 PASS lines (FAIL lines: 0).

## Registry and levels

- 336 registry IDs present; zero status changes against the source commit.
- BUILD_QUALIFIED: True; CORE_DEVICE_QUALIFIED: False; FULL_SCOPE_QUALIFIED: False.
- Device checks: NOT_RUN (no device attached). Docker runtime: BLOCKED (storage exhausted). Live RPC: read-only probe only.

## Artifact hashes (SHA-256)

- evidence/qualification/implementation-status.json: `40c95bcea14c10eaa44b57debaa3d78f33d3bd914568cced9b45ec0983f28b97`
- evidence/qualification/command-results.jsonl: `d2d9db62f4dc6d21ee4ec2cf238746d0931bad9825be864e7e475278de8f2742`
- evidence/qualification/source-after.json: `506dea5cde7f7873aeb977a3856457e4ebf39405204260099fef1e9a41b8639e`

## Notes for this run

- core-tests runs the `com.example.domain` JUnit classes in plain JVM (scripts/qualify-core.py). Robolectric Room tests in `com.example.data` run only under Gradle (android-build gate).
- node-integration.log contains one traceback line naming a local script path (not a credential); not altered.

## Known limits of this run

- Android gates were up-to-date, not re-executed; their counts come from the last executed build.
- Cited but unbundled artifacts (APK, zip, prover build outputs) are listed in evidence/qualification/unbundled-references.json.
- This record does not establish device, hardware-security, live-RPC, recovery, hosting-matrix, external-provider, or production readiness.
