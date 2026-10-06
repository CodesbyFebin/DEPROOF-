#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../prover-worker"
export GOCACHE="$(pwd)/build/cache" GOMODCACHE="$(pwd)/build/modules"
mkdir -p build
GOTOOLCHAIN=local go mod download
GOTOOLCHAIN=local go build -trimpath -o build/prove ./cmd/prove
GOTOOLCHAIN=local go build -trimpath -o build/verify ./cmd/verify
DEMO_DIR=$(mktemp -d "${TMPDIR:-/tmp}/deproof-proof.XXXXXX")
./build/prove "$DEMO_DIR/result"
./build/verify "$DEMO_DIR/result" 35
if ./build/verify "$DEMO_DIR/result" 36; then
  echo 'FAIL: wrong public input accepted' >&2
  exit 1
fi
RESULT_OUT="build/qualified-result-$(date -u +%Y%m%dT%H%M%SZ)-$$"
cp -R "$DEMO_DIR/result" "$RESULT_OUT"
printf '%s\n' "$RESULT_OUT" > build/latest-qualified-result.txt
