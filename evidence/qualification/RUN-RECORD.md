# Qualification run record

- Source identity: HEAD `7dcd9e36a9daf30e39203c9242fed82df1deb4be` (tracked tree clean at start; untracked raw state excluded).
- Run directory: `evidence/qualification/runs/20261006T123812Z-86707` (raw run state; not committed).
- Command: `bash scripts/qualify.sh` (exit 0).

## Gate results (from evidence/qualification/command-results.jsonl)

| Gate | Exit | Ran at (UTC) | Command |
|---|---:|---|---|
| core-tests | 0 | 12:38:28 | `python3 scripts/qualify-core.py` |
| android-build | 0 | 12:38:49 | `scripts/qualify-android.sh` |
| node-tests | 0 | 12:38:54 | `scripts/qualify-node.sh` |
| prover-tests | 0 | 12:38:57 | `scripts/qualify-prover.sh` |
| verifier-tests | 0 | 12:38:57 | `python3 -m unittest discover -s tools -p test_*.py` |
| localization-catalogs | 0 | 12:38:58 | `python3 scripts/qualify-localization.py` |
| hosting-isolation | 0 | 12:39:08 | `python3 scripts/qualify-hosting.py` |
| node-integration | 0 | 12:39:46 | `env DEPROOF_TEST_HOSTING=1 python3 scripts/qualify-integration.py` |
| rpc-read-only | 0 | 12:39:51 | `python3 scripts/qualify-rpc.py` |
| coverage-reconciliation | 0 | 12:39:51 | `python3 scripts/reconcile-coverage.py` |
| backlog-priorities | 0 | 12:39:51 | `python3 scripts/prioritize-uncovered.py` |
| registry-checks | 0 | 12:39:51 | `python3 scripts/check-registries.py` |
| web-build | 0 | 12:39:51 | `python3 scripts/build-web.py` |
| web-checks | 0 | 12:39:53 | `python3 scripts/qualify-web.py` |
| browser-checks | 0 | 12:40:16 | `node scripts/browser/qualify.mjs` |
| coverage-reconciliation | 0 | 12:40:16 | `python3 scripts/reconcile-coverage.py` |
| backlog-priorities | 0 | 12:40:16 | `python3 scripts/prioritize-uncovered.py` |

## Exact counts and key lines

- core-tests: OK (66 tests)
- verifier-tests: Ran 24 tests in 0.092s
- prover-tests: PASS: local Groth16 relation verified. Verification key trust remains an explicit setup assumption.
- android-build: `testDebugUnitTest` and `assembleDebug` were UP-TO-DATE (same inputs as the last executed build); `lintDebug` executed. Last executed unit-test XML totals in app/build: tests=88, failures=0, errors=0, skipped=0.
- hosting-isolation: 12 PASS lines (FAIL lines: 0).
- node-integration: 23 PASS lines (FAIL lines: 0).

## Registry and levels

- 336 registry IDs present; zero status changes against the source commit.
- BUILD_QUALIFIED: True; CORE_DEVICE_QUALIFIED: False; FULL_SCOPE_QUALIFIED: False.
- Device checks: NOT_RUN (no device attached). Docker runtime: BLOCKED (storage exhausted). Live RPC: read-only probe only.

## Artifact hashes (SHA-256)

- evidence/qualification/implementation-status.json: `c119855e6155b1243d3bd20dc41f0bb8ef3508d2139c4bea2b7a17d3b46e153a`
- evidence/qualification/command-results.jsonl: `f7fa509ee0b0e1b0547bfb7c7d9138fb29c839cd176e187cfb2233641a71ab36`
- evidence/qualification/source-after.json: `98a4063b66a583c1a7607c696875fbee02f121b7ce8c90fa3448115640886ccb`

## Recalculated registry status

- 336 IDs present. Zero status changes against the source commit 7dcd9e3.
- F030 and F079 remain IMPLEMENTED_UNVERIFIED, with local criterion tests linked. Live provider and device acceptance remain NOT_RUN.
- Uncovered requirements: 281, unchanged.

## Known limits of this run

- Android gates were up-to-date, not re-executed; their counts come from the last executed build.
- Cited but unbundled artifacts (APK, zip, prover build outputs) are listed in evidence/qualification/unbundled-references.json.
- This record does not establish device, hardware-security, live-RPC, recovery, hosting-matrix, external-provider, or production readiness.
