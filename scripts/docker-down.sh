#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose --profile build --profile owner-hosting down
printf '%s\n' 'Stopped project containers. Named state/cache volumes are retained.'
