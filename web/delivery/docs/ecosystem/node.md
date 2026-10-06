# Node operation and consent boundaries

The Go agent has no third-party dependency. It generates an Ed25519 control identity using OS entropy and stores a 0600 private key in a 0700 directory. This is software file protection, not Android hardware backing. Identity persists; control sessions are revoked after restart. Consent is disabled after restart while reserved usage persists.

Start with READ_NODE scope only:

```sh
node-agent/build/deproof-node --state /private/owner/node --pair-scopes READ_NODE
```

Copy the console's challenge JSON into challenge.json locally. Copy node/tls.pem via an owner-approved channel. Approve the actual fingerprint and supply the expiring code:

```sh
python3 tools/node-client.py --certificate /private/owner/node/tls.pem pair \
  --challenge challenge.json --code ACTUAL_OWNER_CODE --fingerprint ACTUAL_FINGERPRINT
python3 tools/node-client.py --certificate /private/owner/node/tls.pem command observe
```

The listener is bound to 127.0.0.1:9843 and TLS 1.3. Client certificate trust comes from copied owner certificate; node identity approval is separate. No LAN/public listener or encrypted mesh tunnel is implemented. Pairing creates a 24-hour scoped session. Every command binds session, operation ID, deadline, policy, action and exact signature bytes. Unknown JSON fields, duplicate keys, forged commands, replay and oversized payloads fail. A replay log cap refuses new operations until the owner performs controlled maintenance; it never silently discards replay protection.

The local owner can stop the agent (SIGINT/SIGTERM), which closes the server and terminates the process; restart revokes every old session and disables sharing. Fine-grained live owner CLI revocation and Android pairing UI are incomplete. Revoke(sessionId) is implemented/tested in the agent library; production owner API wiring remains required.

For a controlled known-byte receiver, the owner may start with SHARE_BANDWIDTH and --contribution-endpoint HTTPS_URL. A scoped client must then explicitly consent with exactly that endpoint, byte cap, bytes/second and deadline. No arbitrary egress proxy exists. Transfer generates zero-filled qualification payloads, not private evidence. Caps reserve bytes durably before I/O; failed/uncertain sends remain charged. Measurement counts bytes read by HTTP transport, excluding TLS/headers/retransmissions, and does not establish receiver acceptance. Every local usage receipt signs exact domain-separated JSON payload bytes and separately discloses software key protection, no peer acknowledgment and no reward.

The stop command cancels active flows and disables consent; the response is STOP_REQUESTED until completion is observed. No stopped badge is claimed merely from the command acknowledgment. Network-transition detection and independent peer acknowledgments are unfinished. Actual traffic/stop/rate/TLS qualification is NOT_RUN under this sandbox's listener restrictions.

Service start/stop explicitly refuses RUNTIME_ISOLATION_UNQUALIFIED. Docker access was denied in this host. No reviewed image digest, rootless runtime or hostile isolation fixtures have qualified. Proof command explicitly refuses PROOF_BACKEND_UNAVAILABLE. Payouts, tunnels, external job adapters and provider membership are blocked; no nodes, rewards, GPU capability or proof totals are simulated.
