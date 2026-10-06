#!/usr/bin/env bash
set -euo pipefail
umask 077
test -O /state || { echo "STATE_VOLUME_OWNER_MISMATCH"; exit 1; }
chmod 0700 /state
args=(--state /state --listen 0.0.0.0:9843 --container-listener --pair-scopes "${NODE_PAIR_SCOPES:-READ_NODE}")
if test -n "${NODE_CONTRIBUTION_ENDPOINT:-}"; then args+=(--contribution-endpoint "$NODE_CONTRIBUTION_ENDPOINT"); fi
if test -n "${NODE_PROOF_JOB:-}"; then
 test -n "${NODE_PROOF_OWNER:-}" || { echo 'Pinned proof owner is required'; exit 1; }
 args+=(--proof-tool /opt/deproof/tools/proof_jobs.py --proof-job "$NODE_PROOF_JOB" --proof-owner "$NODE_PROOF_OWNER")
fi
exec /opt/deproof/prover-worker/build/deproof-node "${args[@]}"
