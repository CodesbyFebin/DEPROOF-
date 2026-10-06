#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose up -d --wait node-agent prover-worker web
printf '%s\n' 'Web: http://127.0.0.1:8088 • Node: https://127.0.0.1:9843'
printf '%s\n' 'Owner pairing details: docker compose logs node-agent (private console; do not publish codes). Bandwidth is off until explicit endpoint/scope/consent.'
