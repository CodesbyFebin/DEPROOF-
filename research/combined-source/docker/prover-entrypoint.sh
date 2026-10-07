#!/usr/bin/env bash
set -euo pipefail
umask 077
test -O /state || { echo "STATE_VOLUME_OWNER_MISMATCH"; exit 1; }
chmod 0700 /state
case "${1:-idle}" in
 idle) exec python3 -c 'import signal; signal.pause()' ;;
 qualify)
  python3 -m unittest discover -s tools -p 'test_*.py'
  OUT="/state/qualified-$(date -u +%Y%m%dT%H%M%SZ)-$$"
  prover-worker/build/prove "$OUT"
  prover-worker/build/verify "$OUT" 35
  if prover-worker/build/verify "$OUT" 36; then echo 'FAIL: wrong public input accepted'; exit 1; fi
  python3 - "$OUT" <<'PY'
from pathlib import Path
import subprocess,sys
p=Path(sys.argv[1]);f=p/'proof.bin';raw=f.read_bytes();f.write_bytes(bytes([raw[0]^1])+raw[1:])
try:
 assert subprocess.run(['prover-worker/build/verify',str(p),'35']).returncode!=0,'tampered proof accepted'
finally:f.write_bytes(raw)
PY
  printf '%s\n' "$OUT" > /state/latest-qualified-result.txt
  ;;
 *) exec "$@" ;;
esac
