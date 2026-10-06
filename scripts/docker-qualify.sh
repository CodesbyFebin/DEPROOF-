#!/usr/bin/env bash
set -uo pipefail
cd "$(dirname "$0")/.."
mkdir -p evidence/docker
case "${1:-portable}" in
 portable)
  docker compose config --quiet || exit $?
  docker compose run --rm prover-worker qualify || exit $?
  python3 scripts/check-registries.py || exit $?
  ;;
 integration)
  python3 scripts/qualify-docker.py || exit $?
  ;;
 hosting)
  # Host-owned existing restricted profile: this command needs the actual owner runtime.
  python3 scripts/qualify-hosting.py || exit $?
  ;;
 *) echo 'Usage: bash scripts/docker-qualify.sh portable|integration|hosting' >&2; exit 2 ;;
esac
