# DEPROOF — single authoritative spec

Version 3.0 • 2026-10-06, Asia/Kolkata • CodesbyFebin

Paste this document as the only build instruction. Reference images guide appearance. Their badges, chip text, and poster lines are not evidence that a feature runs.

Counts, not summed into fake uniqueness:

- 120 base product features, F001–F120
- 100 core acceptance checks, C001–C100
- 62 base function contracts, FN001–FN062
- 30 ecosystem requirements, E001–E030
- 24 ecosystem contracts, EF001–EF024
- 86 contracts in total (62 + 24)
- 150 tracked requirements (120 + 30). Overlaps are mapped. Do not claim 150 unique features.

Statuses: NOT_STARTED, IMPLEMENTING, IMPLEMENTED_UNVERIFIED, VERIFIED, BLOCKED, LATER. Checks: PASS, FAIL, NOT_RUN, NOT_APPLICABLE. LATER and BLOCKED are incomplete. Never use Safe. Never invent a balance, signature, node, proof, reward, partnership, or passing test.

---

## 1. Authority

Brand: Deproof. Creator: Febin Francis — CodesbyFebin.

Taglines, each with a job:

- Product: **See, understand, verify before you sign.**
- Short: **Review. Sign. Verify.**
- Ecosystem poster: **Own your node. Prove your contribution.**
- Board line, Ecosystem and website only: **One workspace. Connected ecosystems.**

Logo authority is `assets/reference/depr-mark-obsidian.png`. The mark is the four-letter **DEPR** monogram: rounded D, E as three bars, P, R. Gradient mint → cyan → blue → violet. Wordmark under or beside it is **DEPROOF**, wide white tracking. Credit **CodesbyFebin** / **Built by CodesbyFebin**. Do not ship the older two-letter DE as the ecosystem mark. Do not invent an S.

**$DEPR is brand identity only.** The contribution board labels it `$DEPR — Brand concept`. There is no mint, no supply, no launch, no airdrop, no price. Do not read a $DEPR balance. Do not build a transfer for it.

SKR is the Solana Mobile SPL integration, mint candidate `SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3`, not a Deproof-issued token.

Showcase references, labeled concept:

- `assets/reference/deproof-30-solutions-showcase.png`
- `assets/reference/deproof-contribution-workspace.png`
- `assets/reference/ecosystem-concept-orbit.png`

Poster footer is a rule: **Project blueprint • UI/UX concept • Proposed integrations. No token launch, audit or partnership implied.**

Where this spec and a picture disagree, this spec wins. Known picture errors: Devnet chip on the public read screen, “validate integrity on-chain” as a current fact, SKR as a Deproof utility token, bell as live push, orbit partners as connected.

---

## 2. Design system

Obsidian field. Neon is edge light, not a text background. Rocky scenes and crystal arcs stay in marketing art. App screens are flat obsidian so hashes and errors remain readable.

| Token | Hex | Role |
| --- | --- | --- |
| obsidian | `#070B14` | Screen |
| raised | `#101722` | Cards |
| ink | `#E8EEF8` | Primary text |
| muted | `#93A0B8` | Secondary |
| mint | `#3DDC97` | Connect, Payable outline |
| cyan | `#22D3EE` | Focus, links, cluster chip |
| blue | `#3B82F6` | Gradient mid |
| purple | `#8B5CF6` | Gradient end, Concept edge |
| danger | `#F07178` | Do not sign, RPC error |
| warning | `#E7C36A` | Stale, Later, Roadmap |
| line | `#1E2A3D` | Hairline |
| buttonLabel | `#062018` | Text on mint gradient |

Gradient, mark and primary button only: `#3DDC97` → `#22D3EE` → `#3B82F6` → `#8B5CF6`. Light theme: pearl `#F7F4EF`, ink `#1A1714`, mint `#1F8A62`, lavender `#6E62C9`.

Components: 16dp cards, 1dp line, 48dp primary button, 999dp pills, 12sp monospace for addresses and hashes. Pills: Concept, Roadmap, Later, Payable, Do not sign, mainnet-beta, devnet. Status is never color alone. Short keys are 4 + ellipsis + 4, with a reveal control. TalkBack labels on Connect, Approve, Reject, Copy. Disabled Approve exposes the reason. Body at least 14sp, scale to 1.3 without clipping the verdict.

App bar: DEPR mark, DEPROOF, cluster chip of the cluster actually in use. No fake notification dot. Center or end credit only on Ecosystem and About.

P1 phone navigation, matching the latest phone mock: **Now, Review, Receipts**. Tasks and Evidence are nested in Now and Review, not extra tabs. P3 adds nested routes under Now: Nodes, Bandwidth, Proof jobs. Do not replace the three destinations with the four-tab orbit mock. Laptop “Contribution workspace” is the P3/P4 operator view, badged Roadmap until the agent runs.

---

## 3. Phases

| Phase | Window | Deliverable | Done when |
| --- | --- | --- | --- |
| P1 | 2026-10-08 MVP | Now, Review, Receipts. Observe, decode, bind, reject, devnet memo if a wallet exists | `assembleDebug` and `testDebugUnitTest` exit 0. APK installs or install is NOT_RUN. No invented signature |
| P2 | 2026 Q4 | Tasks, file evidence, hardware sign, concept directory, quoted SKR staking observation | Local flows tested. Staking sign stays blocked without a verified layout |
| P3 | After P2 gates | Edge agent, local hosting, bandwidth, prover, contribution receipts | Each E item is VERIFIED_LOCAL or visibly BLOCKED. No fake online count |
| P4 | After P3 status is honest | Docs site, status pages, checksums | Every page matches registry status |
| P5 | Separate authorization | Optional provider ingestion, backup, remittance-class research | Not required for P1. No custody |

P1 is the hackathon slice. Do not spend 8 October on Go, Docker, or a website.

Package: `com.aistudio.deproof.sdwk`. Namespace: `com.example`. Launcher: `com.example.MainActivity`. Kotlin, Compose, Material 3, one module. Min SDK 24. MWA candidate `com.solanamobile:mobile-wallet-adapter-clientlib-ktx:2.0.7`. Pin whatever 2.x resolves and record it. No Expo. No Firebase. Builds must succeed with placeholder or absent AI keys.

---

## 4. Protocol lock

Candidates until `docs/protocol-sources.md` records URL, date, and fixture. If a read disagrees, show the disagreement and refuse the sign path.

- Token program `TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA`
- Token-2022 `TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb` refused in the strict path
- SKR mint `SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3`, expected decimals 6
- System `11111111111111111111111111111111`
- Memo `MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr`
- SOL stake `Stake11111111111111111111111111111111111111` — label SOL stake, do not sign
- SKR staking program quote `SKRskrmtL83pcL4YqLWt6iPefDqwXQWHSw9S9vz94BZ` — id is not a codec
- Associated token program display only

TransferChecked: discriminator 12, amount u64 LE at offset 1, decimals at offset 9, 10 bytes, accounts source, mint, destination, authority. Payable only if program, mint, decimals 6, roles, and every sibling instruction pass. Refuse Transfer 3, Approve 4, Revoke 5, SetAuthority 6, MintTo 7, Burn 8, CloseAccount 9, ApproveChecked 13, BurnChecked 14, unknown discriminators, and lookalike program ids.

SOL stake discriminators are u32 LE: 0 initialize, 1 authorize, 2 delegate, 3 split, 4 withdraw, 5 deactivate, 7 merge. Deactivate is not an SKR unstake.

Home reads are mainnet-beta. First wallet sign is a devnet memo. Mainnet SKR draft waits until that memo signature is stored and observed at least confirmed. Automated runs must not submit mainnet. Public RPC allowed. User can override with HTTPS. Failure shows the error string. No invented lamports.

u64 uses an unsigned representation. `2^64-1` displays `18446744073709551615`. No float formatting.

Wallet path: MWA authorize. Address is the 32 protocol bytes, base58. Not the chain field. Prefer `signTransactions`, check returned message bytes, then `sendTransaction`. Do not claim a post-hoc check blocked a `signAndSendTransactions` broadcast. If the wallet only exposes atomic sign-and-send, mark strict submit BLOCKED and keep read, review, and reject.

Identity name `Deproof`. URI candidate `https://deproof.app` is a label, not a deployed site.

---

## 5. P1 behavior

Now: paste address, reject bad base58, independent SOL and SKR reads, slot, observation time, last 10 signatures, pull to refresh, stale label after error, last receipt, no chart. Empty history is empty.

Review: one transaction, all instructions evaluated. Verdict Payable or Do not sign. Program id shown. Fee or unknown. `This cannot be reversed` on a payable transfer. Approve off until session, cluster match, memo gate, and both hashes match. Copy says the app will not invent a signature. Reject writes `signature: null` and does not need a matching hash.

Hashes:

- `cardHash` is SHA-256 of the newline card below, not the wallet payload.
- `messageSha256` is SHA-256 of the exact serialized Solana message bytes.

```text
deproof-review-v1
network=<cluster>
feePayer=<base58 or unknown>
programId=<base58>
keys=<comma-separated base58>
data=<lowercase hex>
```

No JSON in the card string. A changed amount byte sets MESSAGE_CHANGED and disables Approve. Historical transactions are OBSERVED, not submitted. Approve cannot resubmit them.

Receipt `deproof-receipt-v2`: outcome separate from submission separate from chain observation. `broadcast` true only after RPC accepts the exact bytes. `submittedByDeproof` false for imports and rejects. Legacy `submittedByClearance` may mirror that boolean and must not brand the app. Copy returns the JSON even if the clipboard fails.

Evidence P1: stream raw file bytes, SHA-256, no full-file load. Keystore sign only if hardware-backed. Software key returns SOFTWARE_KEY. Local signature is local, not a Solana signature. Evidence UI hidden on Do not sign. Canonical manifest is RFC 8785 JCS, schema `deproof-evidence-v2`. Sign the envelope bytes with SHA256withECDSA, which hashes them once. Do not call it raw prehashed ECDSA.

Grok, if used, explains an already decoded packet. Placeholder key does not call the network and cannot enable Approve. Failed HTTP shows the status code.

Build gate:

```bash
printf 'GEMINI_API_KEY=MY_GEMINI_API_KEY\nGROK_API_KEY=MY_GROK_API_KEY\n' > .env
printf 'GEMINI_API_KEY=MY_GEMINI_API_KEY\nGROK_API_KEY=MY_GROK_API_KEY\n' > app/.env
echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties
./gradlew assembleDebug --rerun-tasks
./gradlew testDebugUnitTest
```

Demo, each step passed, failed, or not run: paste a real address; open a real signature; reject a SetAuthority fixture with null signature; tamper one byte and show MESSAGE_CHANGED; devnet memo only if a wallet device exists; copy receipt with broadcast false unless that memo was submitted.

---

## 6. Feature, check, and contract index

P1 features F001–F050, F061–F070, F075–F080, F091–F100 as they apply to observation, decode, binding, receipts, and failure states. P2 features F051–F060 and F071–F074. P3 features F081–F090 only with a verified staking layout, else BLOCKED. F101–F110 stay Concept until an attributable adapter exists. F111–F120 payment, escrow, remittance, and recovery stay P5 and unwired. F119 is the P4 site. F120 is release packaging after P1 artifacts exist.

C001–C012 account. C013–C024 history. C025–C044 decoder. C045–C054 review binding. C055–C064 wallet. C065–C076 receipts and evidence. C077–C088 cooldown honesty and combined-instruction refusal. C089–C100 Grok skip, cluster locks, no second live product, unit tests, green assemble. Map each C to its F in `core-checks.json`.

FN001–FN040 are the original pure and wallet contracts: isPubkey, formatSol, formatSkr, loadAccount, rankInstruction, toRequest, materialize, decodeInstruction, readU64LE, readU32LE, transferCheckedData, canonicalMessage, sha256Text, sha256Hex, tamperAmount, assertUnchanged, canSign, verdictOf, summaryOf, shortKey, formatAmount, observeSettlement, getSkrRawBalance, signDevnetMemo, latestDevnetBlockhash, connect, disconnect, saveObservation, reject, copyReceipt, canonicalEvidence, sha256File, signHash, verifyLocalSignature, statusLabel, cooldownRemaining, selectCluster, refuseUndecoded, prepareReview, exportReceipt.

FN041–FN062: parseTransaction, resolveLookupTables, evaluateAllInstructions, simulateTransaction, estimateFee, validateTokenAccount, buildSkrTransfer, rebuildExpiredTransaction, submitSignedTransaction, validateReturnedTransaction, createTask, attachEvidence, verifyManifest, createReceipt, refreshReceiptStatus, verifyStakingProtocol, decodeSkrStake, computeWithdrawEligibility, runConnector, exportQualificationReport, explainPacket, signEvidenceEnvelope. Named failures only. No silent null success.

---

## 7. Thirty ecosystem requirements

All thirty are in the final scope. P1 does not implement the node. Each row names phase, overlap, and the acceptance check. External protocol claims stay BLOCKED until a real adapter returns attributable data.

| ID | Requirement | Phase | Overlap | Acceptance check |
| --- | --- | --- | --- | --- |
| E001 | Wallet connect and session display | P1 | F011–F020, C055–C058 | MWA returns an address or NO_COMPATIBLE_WALLET. No mock |
| E002 | Transaction review | P1 | F031–F050, C025–C054 | Payable only for official SKR TransferChecked. Sibling refusal blocks all |
| E003 | Message tamper guard | P1 | F045–F050, C045–C050 | One amount byte sets MESSAGE_CHANGED. Both hashes recomputed |
| E004 | Cluster lock | P1 | C093–C094 | Home is mainnet-beta. Memo is devnet. Mismatch refuses |
| E005 | Official SKR mint read | P1 | F005–F010, C005–C010 | Sum of matching token accounts. Independent error. Not a $DEPR balance |
| E006 | Exact amount formatting | P1 | FN002, FN003, FN021 | 9 SOL decimals, 6 SKR decimals, u64 boundary, no float |
| E007 | Devnet memo proof | P1 | F059–F060, C059–C060 | Stored signature equals wallet return, else NOT_RUN |
| E008 | Mainnet transfer draft gate | P2 | F081 path, C061 | Draft only after confirmed memo. No CI submit. No mandatory payment |
| E009 | Staking state observer | P3 | F081–F086 | Verified layout or `SKR stake layout not verified. Do not sign.` SOL stake never relabeled |
| E010 | Cooldown and claim alerts | P3 | F087–F090, C077–C083 | Timer starts only from a supplied timestamp. Expiry does not enable withdraw |
| E011 | Node identity | P3 | EF001–EF003 | Ed25519 identity persists. Replay and unauthorized host rejected. Not a Solana wallet key |
| E012 | Local service hosting | P3 | EF005–EF007 | Allowlisted profile starts and stops. API accept is not healthy |
| E013 | Container isolation | P3 | EF005 | Resource caps and nonroot. Hostile fixture on a real host, or BLOCKED. Phone does not run Docker |
| E014 | Health and uptime | P3 | F108, EF008 | Stale heartbeat is offline. Crash cannot leave Online |
| E015 | Encrypted peer tunnels | P3 | EF009 | Real handshake and route limit, or unavailable. No invented mesh |
| E016 | Bandwidth consent | P3 | F104, EF010–EF012 | Default off. Stop closes the flow. Not an open exit proxy |
| E017 | Traffic quotas | P3 | EF014 | Cap survives restart. Cannot enforce means sharing refuses |
| E018 | Contribution metering | P3 | EF013 | Raw counters, units, sequence, gaps. Local claim is not auto-verified |
| E019 | Signed usage receipts | P3 | EF015–EF016 | Domain-separated node signature. Tamper and replay fail |
| E020 | Payout reconciliation | P3 | EF017 | Compare provider ledger to chain. No SKR reward unless that provider pays SKR. No invented APY |
| E021 | Proof job discovery | P3 | EF018 | Signed bounded job schema. Malformed and expired refused |
| E022 | Capability matching | P3 | EF019 | Observed CPU, RAM, disk, backend. No fabricated GPU |
| E023 | Verifiable job results | P3 | EF020 | Real proof bytes from a pinned sample. SHA-256 of a note is not a proof |
| E024 | Proof verification | P3 | EF021 | Independent verifier. Wrong key and tampered proof fail |
| E025 | Contribution receipts | P3 | EF022 | Claimed execution separate from independently verified result |
| E026 | Raw-file hashing | P1 | F061–F064, C070 | Streaming raw bytes. Known digest matches |
| E027 | Hardware-backed signing | P1 | F067–F070, C071–C072 | Reported level only. Software key refused |
| E028 | Evidence manifests | P1/P2 | F063, F066 | JCS golden vectors. Unicode and nulls stable |
| E029 | Portable receipt export | P1/P4 | F080, C066 | Schema-valid JSON. Secrets redacted. Offline check |
| E030 | Privacy and permission controls | All | F058, F116 | Denial stops the action. No implicit upload. No seed entry |

---

## 8. Three P3 flows and the SKR boundary

Android is the review surface. The edge agent is a user-owned Go process on Linux x86_64 or ARM64. The phone pairs to it. The phone does not host containers. A missing agent shows Not configured, as on the laptop mock. Configure node does not fake a start.

### Local hosting

Profile lists image digest, CPU, memory, PID, disk, health URL, egress, ports, mounts. User reviews the profile. Start returns an operation id, then a later health observation. Stop is observed termination, not a button badge. Port conflict, disk full, and missing runtime are named errors. A mock Docker API is not isolation proof.

### Bandwidth

Consent names provider, destinations, direction, caps, and reward source. Default off. Metering records byte unit, interval, direction, sequence, and counter source. Peer acknowledgment is a second signature, stored separately. Reconciliation fields: measured, accepted, accrued, claimable, submitted, paid. Paid SKR exists only if the observed transaction mint is the official SKR mint and the provider record says so. Otherwise the asset is named or unknown. No treasury, no sponsorship, no guaranteed rate.

### Prover

A job is a pinned circuit or program, not a shell string. States: DISCOVERED, VALIDATED, ACCEPTED, RUNNING, RESULT_READY, VERIFIED, VERIFICATION_FAILED, plus CANCELLED, EXPIRED, EXECUTION_FAILED. Producer cannot mark its own result independently verified. Qualification uses one maintained sample and an independent verifier. VERIFIED_LOCAL does not mean network membership or a reward. Witness leaves the device only with consent. Do not claim zero-knowledge unless that circuit was actually verified.

SKR boundary for all three: node keys are not wallet keys. Contribution receipts are not Solana transactions. An optional chain anchor is a separate reviewed transaction under the P1 policy. It is not required to make a local receipt legitimate. Staking layout remains unsigned until verified. $DEPR is never the payout asset.

---

## 9. Ecosystem contracts

EF001 createNodeIdentity. EF002 pairNode. EF003 revokeNodeSession. EF004 probeNodeCapabilities. EF005 validateServiceProfile. EF006 startLocalService. EF007 stopLocalService. EF008 observeNodeHealth. EF009 establishPeerTunnel. EF010 setBandwidthConsent. EF011 startContributionFlow. EF012 stopContributionFlow. EF013 measureContribution. EF014 enforceTrafficQuota. EF015 signUsageReceipt. EF016 verifyUsageReceipt. EF017 reconcilePayouts. EF018 discoverProofJobs. EF019 matchProofJob. EF020 executeProofJob. EF021 verifyProofResult. EF022 createContributionReceipt. EF023 exportVerificationBundle. EF024 runEcosystemQualification.

Each returns a typed value or a named error. Qualification dimensions: VERIFIED_LOCAL, VERIFIED_DEVICE, VERIFIED_EXTERNAL_PROTOCOL, BLOCKED, NOT_RUN. They attach to status. They do not replace it.

---

## 10. Website and license

P4 site pages: Home, Workflows, Requirements, Protocol, Privacy, Status. Six workflow cards match the showcase and carry Roadmap or the real status. Orbit names from the older board (Phantom, Solflare, Helium, Hivemapper, Jupiter, Raydium, Marinade, Jito, Arweave, IPFS, Explorer, Solscan) stay Proposed integrations. Outbound links are external. No partnership copy.

License proposal: MIT, Febin Francis / CodesbyFebin, provisional until dependency rights are checked. Include LICENSE, CONTRIBUTING, SECURITY, CHANGELOG. Do not treat the poster as a grant.

---

## 11. Done

P1 done: assembleDebug 0, unit tests 0, APK checksum, demo steps honest, device steps NOT_RUN if adb is empty.

Full scope done only when all 30 E rows are verified or explicitly unresolved in `docs/status.md`. Unresolved mandatory rows leave the full-scope gate incomplete.

Do not claim a GitHub URL, a deployed site, a token launch, an audit, or a device pass that did not happen.

---

## 12. Copy-ready brief

Build Deproof from an empty directory. Mark is DEPR. Wordmark is DEPROOF. $DEPR is brand only. SKR is the Solana Mobile mint, not a Deproof token. Credit CodesbyFebin. P1 by 2026-10-08 is Now, Review, Receipts: observe mainnet, decode, bind both hashes, reject, optional devnet memo. Then P2 tasks and evidence, P3 node, bandwidth, and prover with the contracts above, P4 docs, P5 only with authorization. Keep 120 features, 100 checks, 62 functions, 30 requirements, and 24 extension contracts mapped, not double-counted. Local counters, signatures, proofs, and external rewards stay separate claims. Never fabricate balances, nodes, bandwidth, rewards, proofs, signatures, partnerships, or passing tests.
