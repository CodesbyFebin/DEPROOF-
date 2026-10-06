# Threat model

Assets: wallet authorization context (never wallet keys), exact reviewed message bytes, private evidence, local signing keys, immutable events, node control identity, scopes and quotas. Adversaries include malicious transactions/wallet returns, untrusted RPC/provider responses, malformed imports, replayed node commands, filesystem failure and stolen devices.

Wallet trust: MWA holds wallet keys; Deproof does not request seeds. All instruction siblings must satisfy the selected policy. Legacy/v0 static-key parser refuses unresolved lookups. Complete message/card/context hashes are separate; approval does not survive process restoration. Signed-byte return is verified before RPC broadcast. Mainnet submission is disabled. Fee/simulation/blockhash must be available and fresh. Simulation and provider responses are not cryptographic safety guarantees.

RPC trust: HTTPS, cluster genesis check, bounded reads/timeouts, no redirect following. Query addresses are separate from input edits. Independent balance errors do not zero other results. Unknown status remains unknown. RPC observations are not finality proofs; external provider disagreement is outside current scope.

Evidence trust: stream raw bytes with size/cancellation bounds; app-private copies; constrained JCS profile and exact note preservation. A P-256 envelope establishes integrity/key possession. KeyInfo metadata is local observation, not attestation or physical presence. No software fallback for hardware-required action. Local hashes, wallet signatures, RPC acceptance and settlement remain distinct.

Node trust: local TLS trust and out-of-band owner fingerprint/code, single-use challenge, Ed25519 commands, independent scopes, strict bounded JSON, durable replay log, expiry and revocation. Permission-restricted keys are explicitly software. Owner-controlled filesystem is trusted; concurrent instances are refused by a nonblocking owner file lock. The node currently supports Unix hosts; Linux runtime qualification remains unrun. Lost-device owner action is stop/restart, revoking all sessions; fine-grained live control incomplete.

Bandwidth: off by default; fixed owner endpoint, scoped consent, caps reserved before flow, rate/time bounds, stop cancellation. HTTP transport payload counters are local claims; signed usage does not prove honest/useful transfer or rewards. Independent receiver/provider corroboration and metered-network transitions are missing release gates.

Hosting/proofs: disabled without qualified runtime/backend. No arbitrary remote shell. Standalone gnark source is an educational circuit with unqualified setup, not physical proof or network reward evidence. Hostile resource/filesystem/runtime tests are unrun. No safety claim is made for container isolation on this host.

Remaining risks: Android compilation/device flows, migrations/process recovery, complete schema enforcement, legacy migration, preview/capture recovery, multi-wallet selection, app node API wiring, local owner revocation endpoint, dependency advisories/locks, full accessibility, release signing and imported asset rights. The qualification report preserves these gaps.
