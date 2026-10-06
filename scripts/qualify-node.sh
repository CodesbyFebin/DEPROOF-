#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../node-agent"
export GOCACHE="$(pwd)/build/cache"
export GOMODCACHE="$(pwd)/build/modules"
mkdir -p "$GOCACHE" "$GOMODCACHE" build
GOTOOLCHAIN=local go test -count=1 ./...
GOTOOLCHAIN=local go vet ./...
GOTOOLCHAIN=local go build -trimpath -o build/deproof-node ./cmd/deproof-node
