#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 scripts/qualify-core.py
python3 -m unittest discover -s tools -p 'test_*.py'
./scripts/qualify-node.sh
printf '%s\n' 'Local domain/node/portable checks complete. This does not represent a real wallet, hardware, network contribution or proof demo.'
printf '%s\n' 'Use docs/setup.md and docs/ecosystem/node.md for the concrete manual gates. No mainnet spending.'
