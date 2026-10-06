#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 -m unittest discover -s tools -p 'test_*.py'
python3 scripts/check-registries.py
