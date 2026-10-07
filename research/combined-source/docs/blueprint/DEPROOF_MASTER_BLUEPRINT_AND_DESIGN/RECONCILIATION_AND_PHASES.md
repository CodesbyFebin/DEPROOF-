## 22. Final v3 reconciliation, authority and execution lock

This full document is the single build authority. Standalone design and ecosystem files are derived views, not competing instructions. Archives under assets/reference/source-* are provenance only. This delivery specifies work; no application, test, wallet, host, proof backend or external adapter is verified by this document creation.

Counts are exact: 120 F records, 100 C core checks, 62 FN contracts, 30 E records and 24 EF contracts = 86 function contracts. The 150 F+E records overlap; they are not 150 unique features. EA001–EA030 are acceptance identities for the existing E requirements, not 30 additional features or additions to C001–C100. Supplementary implementation helpers and test cases do not alter these counts.

**Brand lock:** DEPR four-letter rounded mark only; DEPROOF product wordmark; mint→cyan→blue→violet; Built by CodesbyFebin. **$DEPR — Brand concept** only: no issuance, tokenomics, supply, launch, price, balance or transfer. Future issuance requires a separately authorized and source-verified specification. SKR operations use its verified mint/program/cluster and never imply a DEPR token exists.

| Source conflict or gap | Final resolution |
| --- | --- |
| Two-letter DE versus four-letter DEPR | Latest DEPR reference wins; older artwork remains historical only |
| New source renumbers ecosystem IDs | Preserve original thirty showcase IDs; crosswalk below identifies renamed concepts |
| Source uses F059/F060 for memo | Memo gate maps to F019/F020 and C059/C060/C061; F059/F060 keep evidence meanings |
| Compressed FN list repeats functions and refers to a missing prompt | Complete FN001–FN062 and EF001–EF024 definitions remain embedded here |
| All 100 checks allegedly verified | No implementation evidence supplied; registry starts NOT_STARTED and checks NOT_RUN |
| Newline evidence versus arbitrary JSON | Constrained review card uses newline; v2 evidence uses JCS; never rewrite signed legacy bytes |
| Card hash treated as entire authorization | Bind cardHash, exact raw messageSha256 and context; validate wallet-returned bytes before submit |
| Any MWA wallet claimed supported | Strict signing capability required; sign-and-send-only wallets return STRICT_SIGNING_UNSUPPORTED |
| SOL stake conflated with SKR | Name then refuse SOL stake; separate verified SKR staking adapter staged P3 |
| Local timer says Withdraw ready | Elapsed timer is informational; protocol eligibility is independently observed |
| P2 described as UI-only versus real Tasks/Evidence | P2 Tasks/Evidence must persist and work; node/bandwidth/prover controls remain informative until P3 |
| Simulation failure left to discretionary approval | Known execution failure blocks core submit; simulation success is not a safety guarantee |
| Local node key called Android hardware-backed | Node signatures report actual key protection; no inferred TEE/StrongBox or measurement truth |
| Nix/install packaging called runtime isolation | Require demonstrated restricted runtime boundaries; packaging alone is insufficient |
| Proof publication assumed P3 universally supported | P3 executes/verifies locally; on-chain anchor requires separately qualified program/policy, explicit approval and reviewed bytes |
| Every contribution payout implied SKR | Reward asset, mint, source and settlement are provider-specific; no guaranteed payout |
| RPC response called cryptographically verified | Label source/slot/freshness and provider observation; independent proof only where actually verified |
| Mandatory AI credentials or overwritten local.properties | AI optional, NOT_CONFIGURED explicit; preserve developer config, no compiled provider secrets |
| Oct 8 deadline/hackathon or APK budget asserted fact | Planning targets only; qualification follows evidence, not date, size or test-count minimum |
| Public repository, domain, partner or audit asserted | Use only real owned URLs and verified status; concept catalogue never implies live integration |

### Stable E-ID crosswalk from the supplied compressed v3 source

| Compressed source concept / ID | Final stable ID or core mapping |
| --- | --- |
| E001 Mobile wallet | E001 |
| E002 Transaction review | E003 |
| E003 Message tamper | E004 |
| E004 Cluster checks | E002 |
| E005 Mint validation | E006 |
| E006 Exact amounts | E007 |
| E007 Devnet memo gate | E001/E004; F019/F020; C059–C061,C094–C095 |
| E008 Mainnet draft gate | E008; C061–C064 |
| Remaining ecosystem concepts | Match by named behavior to Section 19; never change stable IDs based on source numbering |

### Phase dependencies and completion evidence

| Phase | Implementation obligation | Required evidence |
| --- | --- | --- |
| P1 Core | Now/Review/Receipts, E001–E008,E026–E029 core portions,E030 consent; real reads, strict decoder, memo gate, file integrity and receipts | Reproducible build/tests; full mutation/refusal vectors; hardware evidence; real wallet/device memo and confirmed chain evidence; report BUILD_QUALIFIED versus CORE_DEVICE_QUALIFIED separately |
| P2 Workflow | Persisted tasks/checklists, capture/import, multiple attachments, evidence associations; E028 expanded; permission handling E030 | Process-death and migration tests, real capture/import permission tests, digest/signature/export golden vectors; no operational node/prover fiction |
| P3 Operator ecosystem | E009–E025, operational nodes/tunnels/consent/quotas/metering/proofs/contributions; E030 revoke/privacy | Per-host/runtime/provider/backend EA011–EA025 tests; caps/kill/restart/replay/revoke vectors; known valid/invalid proofs; local versus external qualification independently recorded |
| P4 Public/open-source delivery | Website/docs/catalogue, E029 offline verification bundle, accurate status and attribution | Real functional routes/links, responsive/accessibility verification, exported bundle independently verified, licensing and SBOM inventory |
| P5 Broader adapters | Additional qualified wallets/providers/DePIN/staking connections; harden all thirty requirements | Versioned adapter support matrix, source/protocol verification, integration regression and required device/network gates; unresolved items remain incomplete |

Always bind local hosting operations to authenticated scoped node sessions: pair → validate immutable profile → explicit start → observe actual health → explicit stop → observe termination. Bandwidth: consent scope/provider/endpoints/caps → enforce before enabling → meter with provenance → stop/revoke → sign usage record → independently corroborate where available → reconcile actual payout. Prover: discover signed job → validate schema/backend/circuit → match actual capabilities → approve input disclosure/resource limits → execute in isolation → independently verify proof/public inputs/key → create contribution receipt → optionally propose a separately qualified anchor. A receipt, node signature, hash or successful proof does not prove physical-world truth, honest bandwidth measurement or entitlement to rewards.

Provider boundaries are explicit: MWA authorizes wallet operations; RPC returns chain observations; Android Keystore signs local evidence; paired node agent owns reviewed host operations; provider adapters define metering/reward semantics; prover adapters define scheme/circuit/verifier; SKR transfers remain inside SKR_TRANSFER_V1. Never route arbitrary prover/payout/staking instructions through that strict transfer allowlist. External unavailable adapters are BLOCKED; local execution does not qualify external connectivity.

Statuses: NOT_STARTED, IMPLEMENTING, IMPLEMENTED_UNVERIFIED, VERIFIED, BLOCKED, LATER. Checks: PASS, FAIL, NOT_RUN, NOT_APPLICABLE with reason. Every VERIFIED entry needs implementation paths, pinned dependencies/protocol sources, named tests and evidence artifact checksums. Final full-scope completion requires every F/E applicable requirement and all 86 applicable contracts qualified; a transparent incomplete report is useful but is not full completion.

### Copy-ready start instruction

You are the implementing coding agent. Read this entire prompt, design-tokens.json, ecosystem-requirements.json and the labeled showcase references. Inspect existing source/config/toolchains before editing. Implement P1 first through reproducible core and real device gates, then progress P2→P3→P4→P5. Maintain the exact IDs/counts and update evidence-backed statuses after each vertical slice. Implement executable behavior, typed named errors, portable schemas, consent/revocation and source-verified protocol boundaries. Use DEPR branding and CodesbyFebin attribution consistently. Preserve user configuration and prohibit fabricated signatures, balances, measurements, rewards or passing tests. Deliver source, APK when built, test/evidence artifacts, documentation, registries, design assets, SBOM, license and reproducible self-hosting instructions. Do not describe a proposed adapter, concept image or unfinished contract as working. Report specific blockers and continue independent authorized work.
