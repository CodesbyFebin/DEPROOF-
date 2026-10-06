# Deproof ecosystem requirements — v3.0

Derived from the final master prompt; IDs remain stable.

## 19. Ecosystem directive — decentralized by design

Build the complete 30-requirement ecosystem extension below after the focused core gates, without claiming unavailable external integrations are working. These requirements are must-haves for the final requested scope. Stage them by dependencies rather than omitting them. If an external interface or device is missing, produce a real local implementation where meaningful, an explicit unqualified external adapter boundary, reproducible evidence, and a concrete blocker. Do not reinterpret the mandate as permission to replace all work with Planned cards.

Product positioning: **Deproof — Own your node. Prove your contribution.** Supporting promise: **Review decentralized actions, measure contributions, verify records, retain local control.** Public creator credit: **Built by CodesbyFebin**. SKR is a Solana Mobile ecosystem integration, not a token issued by Deproof. This specification does not establish a partnership, reward entitlement or access to a provider’s network.

The application is an operator workspace. It may observe, prepare and review supported Solana actions; coordinate user-owned hosting and contribution jobs; verify evidence/proofs; and preserve receipts. It never becomes a custody wallet, general remote shell, anonymous open exit proxy, automatic mainnet spender, or universal interceptor.

Decentralized principles are concrete behaviors:

- Local ownership: evidence stays private locally unless explicitly exported/uploaded; core app and node functions need no Deproof account.
- Portable identity: node public-key identity, exportable public verification material, independently verifiable receipts and documented schemas.
- Replaceable infrastructure: configurable validated RPC and connector endpoints, explicit provider adapters, no compulsory hosted Deproof backend.
- Least authority: per-device pairing, scoped service profiles, quotas, consent, expiration, revocation and no cloud agent that bypasses local approval.
- Verifiable outcomes: node activity, counters, cryptographic job results, signature integrity and chain observations are separately evidenced.
- Honest decentralization: one self-hosted process is local hosting, not proof of a globally decentralized network. Document central dependencies and trust assumptions.

### 19.1 Runtime boundaries

| Runtime | Responsibilities | Exclusions |
| --- | --- | --- |
| Android / Solana Mobile app | Observe/review, MWA authorization, local evidence, receipts, consent and paired-node dashboard | No wallet private keys, arbitrary remote shell or implicit root/container hosting on a phone |
| User-owned edge agent | Identity, constrained service profiles, local counters, health, signed events, resource limits | No open anonymous relay or unbounded jobs |
| Prover worker | Isolated verified job backend, bounded input/resources, proof/result production | No private evidence sent without consent or invented proofs |
| Connector adapters | Protocol-specific RPC/provider data, job discovery, payout observation, proof verification | Provider logo/link is not implementation |
| Optional coordination server | Authenticated discovery/ingestion only where needed, quotas, metadata | No required account for core local use, wallet custody, global power to approve jobs |
| Website/docs | Accurate capabilities, status, installation and reproducible verification | Concept screenshots never presented as installed production proof |

Default edge implementation: a maintained Go toolchain with pinned dependencies on supported Linux x86_64/ARM64 hosts. This is a design choice, not a claim that the build environment supports it. Verify compatibility during setup. A Rust implementation is acceptable only as an explicit documented replacement, not a duplicate runtime to maintain. Android talks to the edge agent; it does not run privileged Docker/container workloads itself. Publish a support matrix distinguishing tested hosts, unsupported environments and device limits.

Add `node-agent/`, `prover-worker/`, `connectors/`, `contracts/ecosystem/`, `docs/ecosystem/` and `evidence/ecosystem/`. These are additive to the repository structure; the initial Android app remains buildable without Go, containers, provider credentials or prover dependencies installed.

Node identity is an Ed25519 key pair generated with a maintained cryptographic implementation and OS entropy. On supported hosts use OS-backed protection; otherwise a permission-restricted private key with explicit security-level disclosure. Public keys are portable; private-key export is off by default. Never reuse node keys as Solana wallets or describe an Ed25519 node key as Android Keystore ECDSA. Evidence-envelope and node-event domains remain distinct.

Pairing defaults to localhost or a deliberately user-enabled authenticated LAN endpoint. Use an expiring single-use pairing code/QR, explicit host fingerprint approval, scoped authorization and replay-protected challenge/response. Bind port exposure and TLS policy to configuration, not to an unconditional 0.0.0.0 listener. A custom Android URI that starts pairing cannot silently authorize a host. Lost-device revocation must work from the node owner’s local control path.

### 19.2 Thirty mandatory ecosystem requirements

Every E item needs real UI/API wiring, named failures, a mapped base feature or explicit extension implementation, automated tests and any required device/network evidence. IDs below match the thirty-solution showcase. Mapping indicates overlap, not automatic completion.

| ID | Requirement | F overlap / phase | C overlap | Contract dependencies | Acceptance check |
| --- | --- | --- | --- | --- | --- |
| E001 | Mobile wallet connect | F011–F020 / P1 | C055–C058,C062–C063 | FN026–FN027 | EA001: Real MWA authorized account; absent wallet/rejection/disconnect handled; no fixture session in production; device proof recorded |
| E002 | Account and network checks | F001–F010, F018 / P1 | C001–C012,C051,C062,C093 | FN001–FN004,FN023,FN037 | EA002: Decode addresses, validate cluster/genesis context and account roles; query identity distinct from input draft; test wrong-cluster handoff |
| E003 | Transaction review | F031–F044 / P1 | C014–C015,C025–C044,C064,C087 | FN005–FN011,FN018–FN021,FN041–FN046 | EA003: Expose every instruction, recipient/token account, amount, fee, requester and limits; historical observation cannot authorize |
| E004 | Message tamper guard | F045–F050 / P1 | C045–C054,C095 | FN012–FN017,FN039,FN048,FN050 | EA004: Full immutable message/context binding before and after wallet signing; mutation and lookup/account/cluster/evidence changes refuse submit |
| E005 | Chain status tracking | F021–F030, F079 / P1 | C013–C024,C068,C075 | FN022,FN028,FN055 | EA005: Real processed/confirmed/finalized/failed/unknown status and unavailable observation; retain prior verified state with timestamp |
| E006 | Verified mint checks | F005,F007,F036 / P1 | C005–C007,C028–C030,C034–C035,C043 | FN007–FN008,FN023,FN046 | EA006: Official-source plus on-chain ownership/decimals/deployment validation; wrong/lookalike mint refuses; source version recorded |
| E007 | Exact token amounts | F004,F007,F033 / P1 | C004,C006–C007,C027–C028,C086 | FN002–FN003,FN009,FN011,FN021 | EA007: Full u64 boundaries, 6-decimal SKR after validation, exact decimal strings, no Number/float truncation |
| E008 | SKR transfer review | F032–F050 / P1/P3 | C025–C035,C041–C064,C084,C087,C094–C095 | FN017,FN024–FN025,FN039,FN043–FN050 | EA008: Qualified TransferChecked draft/account policy after devnet gate; explicit device approval; no automatic mainnet spending or mandatory fee disguised as receipt proof |
| E009 | Staking state observer | F081–F086 / P3 | C036–C037,C080–C083 | FN056–FN058 | EA009: Protocol-specific verified state/layout adapter; liquid/staked/cooling separation; native SOL stake never relabeled SKR |
| E010 | Cooldown and claim alerts | F087–F090,F115 / P3 | C077–C083 | FN036,FN056,FN058 | EA010: Current verified start/duration/season/eligibility; source/date; exact alarm preference and permission handling; local clock cannot authorize withdrawal |
| E011 | Node identity | New extension / P3 | None: new EA011 | EF001–EF003 | EA011: Stable public-key identity, authenticated pairing, protected key, key rotation/revocation; forged/replayed event and unauthorized host rejected |
| E012 | Local service hosting | New extension / P3 | None: new EA012 | EF004–EF007 | EA012: Real start/stop/status for reviewed allowlisted user-owned service profiles; actual process/container evidence; loopback-first, declared ports/mounts and recovery |
| E013 | Container isolation | New extension / P3 | None: new EA013 | EF004–EF007,EF024 | EA013: Qualified runtime with resource caps, nonroot user, restricted capabilities/mounts and network policy; hostile resource/filesystem/process fixtures prove boundaries; failure disables hosting |
| E014 | Health and uptime checks | F108 + extension / P3 | C012,C024 (freshness only) | EF008 | EA014: Actual process identity, health probe, observed state and timestamp; stale heartbeat is unavailable/offline, not measured uptime; kill/restart/outage tests |
| E015 | Encrypted peer tunnels | New extension / P3 | None: new EA015 | EF009 | EA015: Supported maintained tunnel protocol with identity binding, authorized peers, scoped routes, key rotation/revocation and measured successful handshake; no invented mesh status |
| E016 | Consent-based bandwidth sharing | F104 + extension / P3 | C089 (consent principle only) | EF010–EF012 | EA016: Explicit provider/destination/use consent, opt-in scope and instant stop; default disabled; permitted contribution flow rather than unrestricted anonymous exit traffic |
| E017 | Traffic and quota controls | New extension / P3 | None: new EA017 | EF010,EF014 | EA017: Enforced byte/time/rate limits and endpoint policy; pause on cap and report actual reason; controlled tests demonstrate enforcement despite restart |
| E018 | Contribution metering | F104 + extension / P3 | None: new EA018 | EF013 | EA018: Provider-defined raw byte/counter semantics, interval, sender/receiver identities and freshness; compare known transfers; local claim not independently verified automatically |
| E019 | Signed usage receipts | F061–F070,F077 + extension / P3 | C069–C073 (integrity principle only) | EF015–EF016 | EA019: Domain-separated signed metering envelope with counter provenance/sequence; exact signature verification and tamper/replay tests; label producer versus peer-confirmed evidence |
| E020 | Payout reconciliation | F079,F104 + extension / P3 | C016–C019,C081 (observation limits only) | EF017 | EA020: Compare attributable provider accrual/claimable/paid records with observed on-chain payments; identify asset and mint; no fixed/guaranteed rewards or presumption every payout is SKR |
| E021 | Proof job discovery | New extension / P3 | None: new EA021 | EF018 | EA021: Real local discovery endpoint plus qualified external adapter if available; signed bounded schema, version, deadline, input digest and compensation terms; malformed/expired jobs refused |
| E022 | Capability matching | New extension / P3 | None: new EA022 | EF004,EF019 | EA022: Observed CPU/RAM/disk/runtime/backend capabilities versus job requirements; unsupported jobs blocked with reason; no fabricated GPU/device attestation |
| E023 | Verifiable job results | New extension / P3 | None: new EA023 | EF020 | EA023: Execute a real supported proof backend with pinned circuit/program/version; produce actual proof/result bytes and commitments; known valid vector reproduced within resource caps |
| E024 | Proof verification | New extension / P3 | None: new EA024 | EF021 | EA024: Independent maintained verifier over proof/public inputs/verification key with pinned provenance; known valid succeeds, tampered/wrong-key/input/unsupported scheme fails |
| E025 | Contribution receipts | F075–F080 + extension / P3 | C065–C069,C073,C075 (receipt overlap only) | EF022 | EA025: Immutable job identity, agreed input commitment, producer/result digest, verification observation and optional chain anchor; receipt distinguishes claimed execution from independently verified result |
| E026 | Raw-file hashing | F061–F064 / P1 | C070 | FN032 | EA026: Stream raw file bytes, bounded content access, compare known digests; no base64-wrapper hash, false physical claim or lost URI permission |
| E027 | Hardware-backed signing | F067–F070 / P1 | C071–C072 | FN034,FN062 | EA027: Qualified Android evidence envelope, true reported security level and independent verification; unsupported hardware/software key never masquerades as TEE/StrongBox |
| E028 | Evidence manifests | F063,F066,F074 / P1/P2 | C073 | FN031,FN052–FN053 | EA028: Deterministic versioned JCS and golden vectors, exact notes/Unicode/file order/nulls; bind to action or explicit evidence-only context |
| E029 | Portable receipt export | F080,F118 / P1/P4 | C066–C067,C075–C076 | FN030,FN040,EF023 | EA029: Schema-valid self-contained verification material, redacted secrets, v1/v2 migration semantics, offline verification/import and hashes; local paths never substitute for portable evidence |
| E030 | Privacy and permission controls | F058,F068,F116 + extension / All phases | C089–C090 (AI consent only) | EF003,EF010–EF012,EF020,EF023 | EA030: Explicit capture/network/share/job/upload scopes, retention/deletion/export/revoke; no implicit evidence upload or wallet secret access; tests confirm denial and stop behavior |

All thirty are final-scope mandatory. During staged development a requirement may be BLOCKED or IMPLEMENTED_UNVERIFIED. The final status must state those gaps; do not say thirty must-haves are delivered unless all required assurance gates passed. Existing base implementations may satisfy overlapping E checks only when the new acceptance conditions and mappings are evidenced.

### 19.3 Local hosting operational contract

Service profiles declare immutable image/artifact digest, required runtime, CPU/memory/PID/disk limits, health endpoint, allowed egress, exposed ports, read-only configuration mounts and optional writable data volume. Review the profile and consequences before start. Never interpolate arbitrary user text into shell commands. Use structured APIs/arguments and deny unqualified profiles rather than bypass sandbox policy.

The node reports desired state separately from observed process/container state. A successful API acknowledgment means command accepted, not service healthy. Persist operation ID and compare actual runtime identity. Stop, freeze, timeout, health degradation, restart and failure recovery are real paths. Host process crash cannot leave the UI showing Online without a fresh heartbeat. Use host-local monotonic time for interval measurement and wall-clock UTC only for human timestamps; clock jumps/reboots create explicit gaps.

Storage boundaries, logging retention, disk exhaustion, corrupt state, conflicting ports, unsupported container runtime and rootless inability must have named outcomes. Qualify the supported hosting profile on real target hosts. A mock Docker API is not proof that workloads are isolated. Run hostile fixtures only in controlled user-owned test environments; do not weaken production restrictions for them.

Encrypted tunnels use a maintained authenticated protocol, not invented key exchange. Document whether it is direct peer connectivity, a centrally relayed path, or unavailable behind NAT. Relays are explicitly configured and permissioned. Endpoint handoff and reconnect cannot expand allowed routes or expose private services automatically. A local two-node test qualifies that local topology only, not a worldwide mesh.

### 19.4 Bandwidth contribution model

Define a supported contribution profile: provider/network, allowed traffic class/endpoints, direction, caps, pricing/reward source, recipient and privacy implications. Start sharing requires explicit consent; pause/revoke immediately stops new contribution and closes the supported flow within a documented bound. Persist cap usage across restarts to prevent bypass. Do not turn the device into an unsolicited public proxy or general-purpose exit node.

Metering schema contains byte units and count point, interval boundaries, direction, provider/job IDs, node identity, local counter source/version, sequence, nonce and relevant peer acknowledgment. Read actual OS/application counters and document exclusions/retransmissions. Local metering is a producer assertion. When a peer/provider corroborates, preserve its separate signed acknowledgment. Neither signature proves honest measurement without the stated trust and counter model. Never label a file digest or signature alone proof that useful bandwidth was delivered.

Reconciliation separates measured contribution, provider accepted contribution, accrued reward, claimable reward, submitted claim and actual paid chain transaction. Assets may differ by provider. SKR reward/settlement is available only where a current verified provider/protocol explicitly supports it. No fabricated treasury, sponsorship, reward rate or entitlement. Missing external payout APIs leave payout reconciliation BLOCKED while real local metering/receipt tests can finish.

### 19.5 Prover-network operational contract

The prover adapter discovers jobs and declares supported cryptographic scheme, circuit/program digest, backend version, verification-key digest, input/public-input schema, maximum witness size, resource profile, deadline and output format. A job is not an arbitrary executable command. Verify job authenticity and allowlisted backend identity before execution. Treat fetched payloads as untrusted; bound size, time, retries and resource use.

For local qualification, choose a maintained backend with official sample circuit/program and known vectors, actually generate a proof, and run an independent verification path. Document scheme and inputs. Do not implement homegrown cryptography or replace a proof with SHA-256 of a task. External prover-network support additionally needs its published job protocol and attribution. A real local proof workflow may be VERIFIED_LOCAL while external membership is BLOCKED; neither implies external rewards or a public distributed network.

Keep private witness/evidence on the chosen execution boundary. If a remote worker is needed, disclose exactly what leaves the device and obtain consent. Do not imply a zero-knowledge privacy guarantee unless the selected protocol/circuit and verification path actually provide and qualify it. Hardware may be required on the host rather than the phone; capability matching decides, not promotional device imagery.

Job state: DISCOVERED → VALIDATED → ACCEPTED → RUNNING → RESULT_READY → VERIFIED or VERIFICATION_FAILED → DELIVERED. Orthogonal terminal states: CANCELLED, EXPIRED, EXECUTION_FAILED. Record each event durably with idempotent identity. Producer cannot approve its own result as independently verified merely by setting a flag. Verification receipt identifies verifier/software/version and trust domain. Valid proof alone does not establish economic contribution, job ownership or reward eligibility; those remain separately corroborated requirements.

Contribution receipt includes job ID, protocol version, input commitment, public inputs or their specified reference, circuit/program and verification-key digests, output digest, producer identity, verification observation, resource/counter provenance and settlement observation if any. Avoid hashes whose original bytes/circuit semantics cannot be recovered or verified. Chain anchoring is optional under a separately qualified policy and user approval, not required to make local receipts legitimate.

### 19.6 Typed extension functions — EF001–EF024

Implement these real contracts with schemas, named errors and evidence. They add ecosystem capabilities without renaming the 62 base contracts.

| ID | Contract | Output and critical failures |
| --- | --- | --- |
| EF001 | createNodeIdentity(config) | Protected identity/public metadata; entropy/key storage failure |
| EF002 | pairNode(challenge, fingerprint, consent) | Scoped authenticated session; expired/replayed/unapproved peer |
| EF003 | revokeNodeSession(sessionId) | Persisted revocation and denied subsequent operations |
| EF004 | probeNodeCapabilities() | Observed host/runtime limits and timestamp; unsupported/unknown values explicit |
| EF005 | validateServiceProfile(profile, policy) | Qualified immutable profile or denied reason |
| EF006 | startLocalService(profile, operationId) | Accepted operation ID then actual observed process state; resource/runtime/port failures |
| EF007 | stopLocalService(serviceId, operationId) | Observed termination or timed-out failure; no premature Stopped badge |
| EF008 | observeNodeHealth(nodeId) | Probe results/uptime intervals with freshness and gaps |
| EF009 | establishPeerTunnel(peer, routes, consent) | Authenticated measured connectivity; revoked/NAT/unavailable errors |
| EF010 | setBandwidthConsent(profile, limits) | Persisted explicit scope and cap policy; denied endpoint/unsupported provider |
| EF011 | startContributionFlow(profile) | Actual restricted contribution session or named capability failure |
| EF012 | stopContributionFlow(sessionId) | Flow termination and final counters; stop timeout explicit |
| EF013 | measureContribution(sessionId, interval) | Raw sourced counters/units/sequence; gaps/counter reset handled |
| EF014 | enforceTrafficQuota(sessionId, counters) | Applied pause/rate/cap decision; cannot enforce means sharing refuses |
| EF015 | signUsageReceipt(meteringEnvelope) | Domain-separated node signature plus exact payload/public key |
| EF016 | verifyUsageReceipt(receipt, peerEvidence) | Integrity, replay/freshness and corroboration report; no automatic measurement-truth claim |
| EF017 | reconcilePayouts(providerLedger, chainObservations) | Accrued/claimable/paid discrepancies, asset/cluster/time; unsupported reward protocol blocked |
| EF018 | discoverProofJobs(adapter, cursor) | Signed validated bounded jobs with source/freshness |
| EF019 | matchProofJob(job, capabilities) | Compatible backend/resource profile or explicit mismatch |
| EF020 | executeProofJob(job, approvedInputs) | Actual bounded proof/result and execution events; cancellation/deadline/backend failures |
| EF021 | verifyProofResult(result, publicInputs, verificationKey) | Independent valid/invalid/unsupported verification report |
| EF022 | createContributionReceipt(job, metering, verification) | Immutable schema-valid provenance with explicit assurance levels |
| EF023 | exportVerificationBundle(receipts, publicArtifacts) | Portable offline-verifiable package, manifest/checksums, sensitive-data minimization |
| EF024 | runEcosystemQualification(matrix) | Per-host/provider/backend gates, local/external distinction, named blockers and evidence |

### 19.7 Data schemas and trust boundaries

Add versioned JSON schemas and golden vectors for NodeIdentity, PairingChallenge, ServiceProfile, NodeObservation, SharingConsent, MeteringEnvelope, PeerAcknowledgment, ProofJob, ProofResult, VerificationObservation, ContributionReceipt and PayoutObservation. Use exact decimal strings for counters/amounts that can exceed safe JSON numeric precision, explicit units, UTC timestamps, monotonic-duration provenance and declared null semantics. Domain-separate node event, metering and contribution signatures; store exact signed payload bytes. Do not reuse Android evidence-envelope schema to ambiguously sign another data type.

Scopes: READ_NODE, MANAGE_SERVICE, MANAGE_TUNNEL, SHARE_BANDWIDTH, RUN_PROOF_JOB and EXPORT_PUBLIC_RECORDS are independently authorized; a read-only paired phone cannot start hosting. Nodes validate every command, not just the UI. Include authenticated caller, operation ID, deadline, idempotency key and scoped policy version. Prevent replay after reconnect/restart. Reject unauthorized config changes, unknown fields where security-relevant, oversized payloads and path traversal. Bounded queues and immutable event logs protect recovery without claiming unlimited offline operation.

Privacy defaults: no evidence upload, witness export, unrestricted egress, mobile-data sharing or background contribution without specific consent. Pause on metered-network transition unless the user explicitly permits that network. Local notifications and Android background lifecycle follow current supported platform rules; implementation must verify required service permissions/policies rather than promise an immortal background node. Keep Android battery/thermal constraints visible; pair to an external host for workloads requiring unsupported resources.

### 19.8 UI integration and ecosystem cards

Use the rounded DEPR brand authority, dark Solana palette, mint/cyan/violet accent tokens, accessible glass-style opaque surfaces and CodesbyFebin credit from the integrated design specification. Preserve Now/Review/Receipts navigation. Under Now add nested **Nodes**, **Bandwidth**, **Proof jobs** and **Tasks** as their implementations qualify. These are routes, not fake functional preview tiles.

Node card: identity fingerprint, paired status, actual host/runtime, observed health freshness, resource limits, service count from actual records, and explicit start/stop control. Bandwidth card: consent state, network/traffic scope, actual metered counter, remaining cap, last accepted provider observation, pause/stop and payout uncertainty. Proof-job card: job/circuit/backend, compatible host, deadline, execution state, verifier result, assurance level and associated receipt. Never display imagined “online nodes,” GPU figures, usage, earnings or verified job totals.

SKR card: validated mainnet mint/balance, staking observation provenance, action-specific cluster, eligibility limits and signed/paid states separately. WalletConnect is not automatically the supported protocol; the core path remains the qualified Solana MWA adapter. Solana Mobile/Seed Vault branding does not establish hardware attestation.

Ecosystem cards on the website organize Solana Mobile, SKR workspace, Edge & Local Hosting, Bandwidth Sharing, Prover Network, Evidence & Control. Each supports five requirements from E001–E030 with per-entry implementation status and evidence links. The original Wallets/DeFi/Liquid Staking/DePIN/Storage/Explorers catalogue remains a provider layer underneath this operator-workflow view, not a contradictory new registry. Outbound provider links remain External resources or Proposed integrations until qualified.

Include the latest thirty-solution banner at `assets/reference/deproof-30-solutions-showcase.png`, explicitly labeled concept reference. It does not override navigation or verified content. Correct any generated four-tab preview to the three-destination core; poster language about real-world proof must be replaced in runtime copy with exact assurance. The logo reference still governs geometry. No new image generation is required to implement the specification.

### 19.9 Qualification gates for the ecosystem extension

- Node gate: identity persists, pairing/revocation works, unauthorized/replayed commands fail and fingerprints are explicit.
- Hosting gate: allowlisted service actually starts/stops, observed PID/container identity correct, crash recovery/port conflict/disk failure work; isolation hostile fixtures demonstrate limits on supported hosts.
- Tunnel gate: real authenticated handshake and route restrictions, revocation/key rotation, peer loss/NAT unavailability represented; central relay dependency disclosed.
- Bandwidth gate: controlled known-byte transfer with matching counters and cap enforcement; consent withdrawal stops flow, restarted counters cannot reset caps, signed/peer evidence tamper/replay fails.
- Prover gate: actual proof generation for a pinned supported sample, independent verification, wrong proof/key/public input rejection, expired/malformed job/capability mismatch, cancellation and resource exhaustion handling.
- Settlement gate: real source records reconcile with chain observations; unconfigured external reward system does not show paid or invent SKR rewards. Development tests do not spend mainnet funds.
- Portability gate: export/import preserves exact signed payloads; independent offline verifier reproduces integrity results without a Deproof account or central server, with missing private inputs marked unavailable rather than false-valid.
- Privacy/UI gate: read-only scope cannot operate services, unconsented upload/witness sharing denied, network transitions respect consent, every runtime state has truthful reachable UI and accessible controls.

Report qualification dimensions separately: VERIFIED_LOCAL, VERIFIED_DEVICE, VERIFIED_EXTERNAL_PROTOCOL, BLOCKED, NOT_RUN. These are evidence dimensions attached to the existing feature status, not replacements for the six status values. Local proof or bandwidth tests do not qualify membership/rewards in a third-party network. Final full-scope qualification requires all thirty E items’ selected external/deployment claims to be supported or visibly excluded as unresolved; unresolved mandatory requirements keep the full-scope gate incomplete.

## 20. Open-source project and final delivery contract

Publishable source must include the native app, edge agent, actual supported prover backend/adapter glue, schemas, fixtures, local/offline verifier, documented setup and runnable scripts. Optional services must be independently self-hostable, with no hidden provider account dependency. Use a monorepo with clear runtime boundaries and separate builds; a node dependency cannot break Android P1 compilation.

Choose **MIT** as the proposed project license, credited to Febin Francis / CodesbyFebin, subject to confirming rights to imported code and brand assets. During implementation create LICENSE, CONTRIBUTING.md, CODE_OF_CONDUCT.md, SECURITY.md, CHANGELOG.md, architecture/threat-model documents, provider/dependency license inventory, issue/PR templates and reproducible build instructions. Mark the license selection as provisional until rights/dependencies are reviewed; a poster is not an open-source grant. Do not silently relicense imported archive material. Logos/provider assets need their own attribution/usage provenance.

Maintain base feature/function IDs and `ecosystem-requirements.json` for E001–E030, plus EF001–EF024 in `functions.json`. Map exact overlap to F/C/FN IDs; store phase, dependencies, supported platforms/providers, implementation paths, named errors, status, qualification dimensions, tests and evidence. Add source-verified protocol manifest, service-profile schemas, packet/event golden vectors, CI for both app and node/verifier, checksum manifests and explicit release-support matrix. Generate an SBOM/dependency inventory and record pinned artifact/circuit/verification-key digests.

Deliver debug APK, supported node binary/container when actually built, independently runnable verification tools, qualified local demo, exact source snapshot and checksums. Release artifacts and real deployed URLs are produced only when authorized/configured. The local demo should show account observation → review/tamper/refusal → real devnet memo → file integrity/local signature → node pairing → constrained local service → measured contribution/stop → actual proof generation/verification → portable receipt inspection. Label any missing hardware/provider segment NOT_RUN or BLOCKED instead of substituting a success animation.

Do not stop at a roadmap: implement every independent authorized requirement and resolve code failures. Checkpoint with exact commands/errors for unavailable infrastructure. A later requirement stays incomplete, not deleted. Mainnet funds, public hosting exposure, provider credentials, actual redistribution rights and publishing remain separate actions requiring applicable authorization; local reversible build/document work should progress autonomously.

