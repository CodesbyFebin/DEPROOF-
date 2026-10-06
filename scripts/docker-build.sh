#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p evidence/docker/android
case "${1:-services}" in
 services) docker compose build node-agent prover-worker web ;;
 android) export DEPROOF_BUILD_UID="${DEPROOF_BUILD_UID:-$(id -u)}" DEPROOF_BUILD_GID="${DEPROOF_BUILD_GID:-$(id -g)}"; test "$DEPROOF_BUILD_UID" -gt 0 || { echo "Use a nonroot Android build UID"; exit 1; }; docker compose --profile build build android-build; docker compose --profile build run --rm android-build ;;
 *) echo 'Usage: bash scripts/docker-build.sh services|android' >&2; exit 2 ;;
esac
