#!/usr/bin/env bash
# Run every independent gate; never hide failures behind a successful final command.
set -uo pipefail
cd "$(dirname "$0")/.."
mkdir -p evidence/qualification
RESULTS=evidence/qualification/command-results.jsonl
RUN_DIR="evidence/qualification/runs/$(date -u +%Y%m%dT%H%M%SZ)-$$"
mkdir -p "$RUN_DIR"
if test -f "$RESULTS"; then cp "$RESULTS" "$RUN_DIR/previous-command-results.jsonl"; fi
: > "$RESULTS"
qualify() {
  local gate="$1"; shift
  local log="evidence/qualification/${gate}.log"
  if test -f "$log"; then cp "$log" "$RUN_DIR/previous-${gate}.log"; fi
  "$@" > "$log" 2>&1
  local result=$?
  python3 - "$gate" "$result" "$log" "$@" >> "$RESULTS" <<'PY'
import sys,json,datetime
print(json.dumps({'gate':sys.argv[1],'exitCode':int(sys.argv[2]),'log':sys.argv[3],'command':sys.argv[4:],'ranAt':datetime.datetime.now(datetime.timezone.utc).isoformat()}))
PY
  cp "$log" "$RUN_DIR/${gate}.log"
  printf '%s: exit %s (%s)\n' "$gate" "$result" "$log"
}
qualify core-tests python3 scripts/qualify-core.py
qualify android-build scripts/qualify-android.sh
qualify node-tests scripts/qualify-node.sh
qualify prover-tests scripts/qualify-prover.sh
qualify verifier-tests python3 -m unittest discover -s tools -p 'test_*.py'
qualify localization-catalogs python3 scripts/qualify-localization.py
qualify hosting-isolation python3 scripts/qualify-hosting.py
qualify node-integration env DEPROOF_TEST_HOSTING=1 python3 scripts/qualify-integration.py
qualify rpc-read-only python3 scripts/qualify-rpc.py
python3 scripts/update-coverage.py
qualify coverage-reconciliation python3 scripts/reconcile-coverage.py
qualify backlog-priorities python3 scripts/prioritize-uncovered.py
qualify registry-checks python3 scripts/check-registries.py
qualify web-build python3 scripts/build-web.py
python3 scripts/report-qualification.py
qualify web-checks python3 scripts/qualify-web.py
qualify browser-checks node scripts/browser/qualify.mjs
qualify coverage-reconciliation python3 scripts/reconcile-coverage.py
qualify backlog-priorities python3 scripts/prioritize-uncovered.py
python3 scripts/report-qualification.py
cp "$RESULTS" "$RUN_DIR/command-results.jsonl"
cat "$RESULTS" >> evidence/qualification/qualification-events.jsonl
python3 - <<'PY'
import json,pathlib,sys
results=[json.loads(x) for x in pathlib.Path('evidence/qualification/command-results.jsonl').read_text().splitlines()]
sys.exit(1 if any(x['exitCode'] for x in results) else 0)
PY
