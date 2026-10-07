# Qualified local workflows

Deproof / DEPR — Built by CodesbyFebin. $DEPR is a brand concept only.

Android keeps Now / Review / Receipts. Nodes, Bandwidth and Proof jobs are nested under Now. The production client is `NodeClient.kt`; the TLS qualification harness invokes that same client and `NodeProtocol.kt` on the JVM. Android Keystore and physical lifecycle are separate device gates.

## Pairing and revocation

Run the owner-controlled node on loopback. Review the node Ed25519 fingerprint, TLS certificate SHA-256, challenge, one-use code and scopes from its terminal. Enter those values into Nodes. The phone must reach the node through an explicitly configured loopback forward; this release does not silently open a LAN port or implement a mesh tunnel. The default hostname verifier remains enabled in addition to the exact certificate pin. TLS 1.3/Ed25519 certificate support must be qualified on the target Android version; older devices can report unavailable.

The node-control Ed25519 seed is encrypted with a separate Android Keystore AES-GCM key. It is not a Solana wallet key and is excluded from cloud and device-transfer backup. No private seed or pairing code appears in receipt exports. Never paste a wallet seed phrase.

The node validates scopes, signature, deadline and replay identity on every command. Revoking the current session is sent to the actual node. The owner can revoke all paired devices locally with `kill -USR1 NODE_PID`, even if the phone is lost. Restart revokes previous sessions and disables bandwidth consent; caps are not reset. Refresh after start/stop/revoke: an acknowledgment does not establish healthy/stopped state.

## Bandwidth

Only a fixed owner-configured HTTPS known-byte receiver is supported. It is a local contribution qualification profile, not an anonymous exit proxy or third-party rewards integration. `--contribution-ca` may supply the owner's receiver CA; it never disables certificate verification or permits a new endpoint from the API.

Consent explicitly names receiver, reserved-byte cap, payload rate and expiration. Quota reservations are durable before network I/O. Uncertain sends remain charged across restart. Counters measure bytes read by the application HTTP transport; headers/TLS/retransmissions are excluded. Stop disables consent and cancels the flow; refresh until active operations are empty. Peer/provider corroboration, useful contribution, external reward and payout are separate unavailable claims.

## Local hosting

`python3 scripts/qualify-hosting.py` tests a cached immutable BusyBox image on the owner's Linux Docker VM. The tested engine is rootful and owner-trusted, not rootless. Workloads are nonroot with read-only root, dropped capabilities, no-new-privileges, network none, no host ports/mounts, 0.5 CPU, 32 MiB memory, 16 PIDs and 1 MiB noexec/nosuid tmpfs. Hostile tests demonstrate CPU throttling, OOM, PID/disk exhaustion and filesystem boundaries.

The single supported `isolated-http-v1` profile serves a fixed local fixture inside the container. It exposes no host/public port. Pass absolute `--hosting-tool PATH/tools/local_hosting.py` and `--hosting-evidence PATH/evidence/qualification/hosting-isolation.json` to the node. Start requires the displayed immutable profile digest and explicit consent. Health is an actual HTTP probe inside the container. Stop inspects observed termination. Runtime/version changes or stale isolation evidence refuse new starts. General services, native Linux/ARM64, rootless engines and encrypted peer tunnels remain unqualified.

## Proof jobs

After `scripts/qualify-prover.sh`, create an owner-local job with `python3 tools/proof_jobs.py create NEW_DIRECTORY`. The job is Ed25519 signed, with a separately pinned owner public key. It pins the cubic circuit and verification key, exact public input, commitment, witness-size profile, version and deadline. The sample input x=3,y=35 is public; no private evidence is uploaded. This is an educational Groth16/BN254 trusted setup, not a production ceremony or external network job.

Start the node with absolute `--proof-tool`, `--proof-job` and `--proof-owner` paths. Proof jobs discovers and validates the signed job. Explicit acceptance invokes the fixed producer followed by a separate verifier process. Tampered job/result/key/public input fails. Cancellation kills the local subprocess group. Timeout bounds execution; production CPU/memory isolation for proofs is not qualified. Events are written durably and contribution records include result/input commitments, key/circuit pins, independent verifier provenance and exact node-signed bytes.

A valid node signature is signature integrity only. `tools/verify.py` deliberately reports proof verification NOT_RUN without proof artifacts; the separate `prover-worker/build/verify` verifies actual artifacts. Same-owner verification, cryptographic relation, physical truth, external provider acceptance and paid rewards remain distinct.

## Migration and recovery

Room schema v2 additively migrates v1. The packaged SQL preserves event/task/evidence bytes; SQLite tests check upgraded columns against exported Room schemas, rollback and operation uniqueness. No destructive migration fallback exists. Actual Room-on-device migration is NOT_RUN.

Commands/submission intents are written before network I/O. Process restoration changes pending effects to OUTCOME_UNKNOWN; it never retries a possible submission or node action. Known wallet signatures are observed read-only. Completed immutable receipts remain intact. File imports sync copied bytes and record FILE_READY before transactional attachment; recovery rehashes that file before attaching. Pending camera state uses saved state, but camera/permission/process-death behavior still needs device testing.

## Qualification

Run `bash scripts/qualify.sh` with permission to use Gradle caches, loopback sockets, the owner's Docker VM and local Chrome. No publishing, deployment, token issuance or mainnet spending occurs. Browser screenshots/axe/keyboard/200% text results qualify the tested Chromium views only; manual screen-reader and Android accessibility remain NOT_RUN. `uncovered-requirements.json` lists unfinished acceptance for every stable registry ID.
