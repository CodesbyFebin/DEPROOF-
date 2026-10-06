#!/usr/bin/env bash
# Foundation qualification gate — delegates to Python for check execution and JSON output
set -euo pipefail
ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT"
python3 scripts/qualification/foundation_gate.py
