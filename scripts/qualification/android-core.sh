#!/usr/bin/env bash
# android-core qualification gate
set -euo pipefail
ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT"
python3 scripts/qualification/android_core_gate.py
