#!/usr/bin/env bash
# android-workflows qualification gate
set -euo pipefail
ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT"
python3 scripts/qualification/android_workflows_gate.py
