# Deproof — Design Specification and Vision-to-Build Blueprint

Version 3.0 • 6 October 2026, Asia/Kolkata • CodesbyFebin

This specification connects the full ecosystem vision to the focused native Android build. It governs visual identity, navigation, components, states, content, responsive behavior and design verification. It complements the final merged master prompt; it does not weaken its transaction, privacy, hardware or qualification rules. This is a design specification, not a claim of a working application.

## 1. Visual authority and reference analysis

![Deproof ecosystem concept](assets/reference/ecosystem-concept.png)

The ecosystem reference uses a centered, front-facing device as the organizing object. Three partner-category cards flank each side; luminous cyan and violet connections converge on the device. A large DEPROOF wordmark and tagline occupy the upper left, an SKR + SOLANA pill balances the header, CodesbyFebin sits upper right, and three benefit cards anchor the footer. Dark layered surfaces, thin luminous borders, outlined icons and strong section titles unify the composition. Rocks, reflections and crystals add a cinematic environment.

Transfer the composition’s hierarchy and reusable card vocabulary into product UI. Keep the rocky scene, reflective floor, crystal ornaments and large connective arcs in marketing artwork. They are unsuitable backgrounds for long transaction details, hashes, forms or errors. The phone illustration is a presentation frame; the application itself must use the available screen rather than render another phone inside the device.

![Deproof DEPR logo authority](assets/reference/depr-mark-obsidian.png)

The approved visual reference is the four-letter rounded **DEPR** mark: D, three-bar E, P and R; mint → cyan → blue → violet. Use DEPROOF as the product wordmark and CodesbyFebin as creator credit. Earlier two-letter DE and S artwork is historical reference only. A raster image is not a source vector: recreate one consistent reviewed SVG/vector master, label it provisional until geometry review, and use it across launcher, app, repository and website.

![Latest Deproof showcase concept](assets/reference/deproof-contribution-workspace.png)

$DEPR is a brand concept only. Display `$DEPR — Brand concept` on marketing/contribution concepts; no mint, supply, launch, price, balance, airdrop or transfer is implemented. SKR is a separately source-verified Solana Mobile integration; it is not Deproof-issued.

The source design Markdown is useful for the three-screen structure, palette, field layouts and state inventory. Its green Payable treatment, 44dp Android targets, forced one-second splash, single Signed status, sign-immediately flow, unconditional URI and mixed cluster selector require the corrections below.

### Keep, adapt, correct

| Reference detail | Product interpretation | Phase |
| --- | --- | --- |
| Center device and six connected category cards | Ecosystem catalogue on web; optional discovery sheet inside Now later | P3/P4 |
| Large DEPR and DEPROOF lockup | Small stable app identity; full lockup on marketing covers | P1/P4 |
| Connect wallet mint CTA | Real MWA action with inline missing-wallet/cancellation states | P1 |
| Transaction review / evidence / tasks / receipt tiles | Route cards for implemented workflows, not decorative tiles | P1/P2 |
| Category Concept pills | Planned/Needs verification states driven by feature registry | P3/P4 |
| SKR integration card | Solana Mobile ecosystem integration; never Deproof-issued token | P1/P3 |
| Neon lines and glass borders | Restrained decorative emphasis; semantic borders remain legible | All |
| Three bottom benefit cards | Review before signing; organize evidence; inspect chain status with assurance limits | P4 |
| Dark crystalline scene | Optional hero background with text scrim; absent from operational UI | P4 |

## 2. Product boundary and information architecture

P1 Android has exactly three destinations: **Now, Review, Receipts**. Now is the workspace, Review is the decision surface, and Receipts is the durable record. Users can observe an address without connecting a wallet. No fourth Ecosystem tab is added merely because it appears in the promotional phone.

P2 adds Tasks and Evidence as nested workflows: Now → Tasks → Task detail → Evidence; Review → Attach evidence; Receipts → Evidence manifest. Maintain top-level destination identity when opening details. P3 adds a discovery sheet reachable from Now, with categories and supported connector detail. P4 adds the public website with Home, Features, How It Works, Ecosystem, Project, Documentation and Privacy. Each layer uses the same tokens and components while preserving platform conventions.

| Layer | Visible capability | Incomplete behavior |
| --- | --- | --- |
| P1 core | Observe account, inspect history, reject, qualified devnet memo, import/hash files, inspect/export receipts | No camera/DePIN/task success presented as complete |
| P2 workflow | Real tasks/checklists, capture/import, previews and multiple attachments | Unsupported capture types have named reasons |
| P3 ecosystem | Only independently qualified adapters; catalogue separates external resources | Planned categories may open information, never fake connect/swap/stake results |
| P4 public story | Accurate project status, real screenshots, source/docs links | Concept images labeled as concepts; no implied partnerships |

Use the master’s 120 base product features, 100 core checks and 62 base contracts, plus E001–E030 mandatory ecosystem requirements and EF001–EF024 extension contracts. Preserve IDs and map overlap instead of adding counts. This design spec adds acceptance details; it does not create another product-feature registry.

## 3. Design tokens and color roles

The following values are explicit design decisions grounded in the references and source palette, not claims of exact pixel sampling. The machine-readable counterpart is `design-tokens.json` in this bundle.

| Semantic token | Dark hex |
| --- | --- |
| obsidian | #070B14 |
| raised | #101722 |
| ink | #E8EEF8 |
| muted | #93A0B8 |
| mint | #3DDC97 |
| cyan | #22D3EE |
| blue | #3B82F6 |
| purple | #8B5CF6 |
| danger | #F07178 |
| warning | #E7C36A |
| line | #1E2A3D |
| buttonLabel | #062018 |

Exactly twelve dark semantic tokens are authoritative. Pearl theme values are separately defined in JSON. `line` is decorative and cannot serve as the sole interactive boundary. Use measured muted/cyan boundaries for controls. Gradient mint → cyan → blue → violet belongs on the mark, hero accents and primary-button perimeter. Small labels require at least 4.5:1 at every pixel beneath them; otherwise use the solid mint label panel with buttonLabel text. Status includes text and icons and never derives trust from the gradient.

Glow tokens: decorative mint/violet at low opacity, radius 12–24dp on hero accents. Application cards use solid surfaces and 1dp boundaries; outer glow is optional on the brand or focused primary element, never every panel. Glass effects must have an opaque fallback and meet contrast against the underlying scene. The outline token supports field/component identification; quieter decorative borders cannot replace it.

Semantic roles are independent of branding. Mint may mark an action or observed finalization; violet means a category or processing state, not verified trust. All outcomes include readable text and icons. A refused instruction uses an error icon/border plus explanation; eligible policy uses a neutral card with mint accent and **Allowed by current policy**, without a shield/checkmark implying audited safety.

Spacing scale: 4, 8, 12, 16, 20, 24, 32, 40, 48. Phone gutters 16dp; compact cards 16dp padding, feature panels 20dp. Card radius 16dp, inputs 12dp, buttons 12dp, chips full capsule. Standard icon glyph 24dp inside a **48×48dp minimum Android touch target**. Keep at least 8dp between adjacent targets. Android dimensions are dp and text sp; website dimensions use CSS rem/px as appropriate. Do not treat these units as interchangeable.

Typography: platform sans-serif in Android with optional licensed brand face; website uses a bundled or system sans-serif with explicit fallback. Display 32sp/38sp bold, screen title 24sp/30sp semibold, section 18sp/24sp semibold, body 16sp/24sp, supporting 14sp/20sp, label 12sp/16sp. Hash/address text is monospaced 12sp/20sp with tabular numerals. Never force font size down to fit a key. At large font scale cards grow, actions stack, and full values wrap in a dedicated view. Marketing DEPROOF letter spacing is not applied to transaction text.

## 4. Shared component contracts

| Component | Required anatomy and behavior |
| --- | --- |
| BrandHeader | Rounded DEPR mark, Deproof title, action-specific cluster pill; optional implemented settings button |
| WalletCard | Actual session/account identity, connection state, Connect/Disconnect; observed addresses explicitly separate |
| BalanceCard | Exact SOL/SKR amounts, per-read slot/time, address queried, independent failures and stale marker |
| QuickActionCard | Icon, title, one-line purpose, route; no interactive styling when an action does not exist |
| PolicyCard | Operation, policy result, reason, limits; all instruction verdicts remain inspectable |
| AddressRow | Role, short display, accessible full-value action, copy; token account and recipient wallet separately labeled |
| HashRow | Specific hash kind, abbreviation, full-view/copy; never generic “hash verified” reassurance |
| StatusChip | Text, icon, semantic role and evidence context; connection/cluster/RPC/outcome never conflated |
| ErrorPanel | Human reason, safe named code/method, recovery action; redact credentials and provider payload secrets |
| EvidenceCard | Imported/captured identity, raw-byte digest, MIME/size, note/provenance, verification limitations |
| ReceiptRow | Outcome, local submission state, chain observation, time; no single ambiguous Signed badge |
| IntegrationCard | Category/provider, Planned/Qualified/Unavailable, exact supported operation, source/freshness, external-link disclosure |
| ApprovalBar | Explicit action, visible gate failures, Reject, gated Approve; system-inset and keyboard safe |

Focus outline must remain distinct from a refusal/error boundary. Disabled controls have a readable adjacent explanation; no hover-only tooltips. Buttons use progress text and prevent duplicate submission. Reject is available when a request exists even if message validation failed, but is disabled during its own local persistence operation to avoid duplicates. If local receipt write fails, say the rejection was not recorded; never show a saved receipt that does not exist.

## 5. Now screen blueprint

Reading order: BrandHeader → workspace title → WalletCard/observed-address input → account snapshot → implemented quick actions → last receipt → recent signatures. On a compact phone avoid a large banner above actionable content.

Title **Your workspace**. Supporting text **Inspect accounts, review actions, keep records.** Address label **Observe a mainnet account**; placeholder **Paste a Solana address**. Button **Observe account**. Wallet CTA **Connect wallet**. These are parallel options; observing never creates a connected badge.

Display SOL and verified SKR independently, with exact decimal values, their individual observation time and slot. One shared slot is shown only when responses genuinely share a validated context. A failed token read leaves SOL intact. Refresh failure says **Stale — last successful read …** beside retained values. Unknown is never zero. Input edits do not rename an already displayed snapshot until the new address has been queried.

History shows up to ten real signatures with chain state, available decoded summary and time. Unsupported operation reads **Details unavailable** with its reason, not a fabricated transfer. Open routes to read-only Review. The last receipt card displays its event outcome and links to Receipts. No fake recent activity or empty portfolio graph.

Cluster pill is descriptive for the current action. Mainnet account observation and devnet memo are visibly separate sections/actions. Any action-specific network change invalidates existing approval and reconnects/reauthorizes if needed. Do not put a global dropdown on Now that silently changes all records.

P2 Task entry is a quick action and nested list. P3 Ecosystem entry opens a clearly labeled discovery sheet. Before those routes exist omit the tile or use a noninteractive Planned explanation inside a roadmap section below core actions.

## 6. Review screen blueprint and decision flow

Show a context label at the top: **Historical observation**, **Devnet memo draft**, or **Mainnet SKR draft**. Historical review has no Approve button; an imported completed transaction is not a new request.

Primary stack: operation summary → policy result/reasons → every instruction → account/cluster/requester → asset amount and account roles → fee and simulation context → review bindings → optional allowed evidence → action bar. Display **Allowed by current policy** or **Do not sign**. Show policy/version limitations beneath eligibility. A green card must not imply “Safe.”

Full account roles remain available before approval. Separate recipient wallet from destination token account; display source, mint, authority and signer/writable roles. Program details copy and correct explorer links are real actions. Raw amount and decimals are inspectable. Fee is an RPC-derived estimate for the exact message; unknown remains unknown and blocks core submission.

Bindings show **Transaction message SHA-256**, **Review card SHA-256**, and evidence digest when present. Abbreviate in summary only; full values are exposed without truncation in a readable copy view. Each label explains its scope. If bytes change, show **Message changed — review again**, include the safe named code, disable Approve and keep Reject. Re-review is an explicit action that rebuilds the displayed context; it must not automatically grant approval.

Button label can be **Approve in wallet** with helper **Your wallet will request authorization; Deproof validates the returned signed message before submitting.** Gating checks include valid policy, current wallet account/cluster, both unchanged hashes/context, valid blockhash, fee review, resolved lookup semantics, prior qualified devnet proof for mainnet, and strict signed-byte wallet capability. Evidence changes also invalidate review. Missing capability reads **This wallet cannot support Deproof’s strict submit flow**; no silent sign-and-send substitution.

Flow: reviewing → wallet authorization pending → signed bytes validated → submitting → RPC accepted or submission unknown → receipt. Wallet rejection is a separate cancellation outcome, never chain failure. Show a known signature while observing confirmation; do not toast “Confirmed” merely because wallet signing returned. On submit timeout show **Submission status unknown — checking this signature** and prevent an unsafe repeat. Reject writes a local record and shows **Rejected locally. Nothing submitted.** only when this is known and persistence succeeds.

P1 evidence section says **Import evidence**, not Capture photo. It is hidden for refused transaction attachment; standalone evidence integrity remains available through the workspace. P2 adds capture actions and task links. A photo cannot make a refused transaction eligible.

## 7. Receipts screen blueprint

List newest events first. Outcome labels: **Rejected locally**, **Observed transaction**, **Evidence signed locally**, **Wallet signed — not submitted**, **Submitted by Deproof**. Secondary chain label: Unknown/Processed/Confirmed/Finalized/Failed; separate availability label when RPC cannot currently be reached. Persist previous known status with observation time rather than replacing it with failure.

Detail sections: event/account/network → submission provenance → chain observation → transaction/card/evidence hashes → local signature envelope → chain signature → manifest → export actions. Local ECDSA and chain signature are visually separated and labeled with their algorithms/context. Slot/block time may exist on an imported observation even though Deproof never submitted it. Missing data stays unknown/null with a reason.

User wording **Submitted by Deproof: No / Yes / Unknown** replaces raw legacy submittedByClearance branding. The JSON compatibility field may exist for old exports but is not a product label. A historical confirmed signature does not turn app submission into Yes. Rejection keeps chain signature absent; submission timeout can be unknown, not definitely false.

Actions: **Copy JSON**, **Export file**, **View manifest**, **Open transaction in explorer** only when their data/implementation exists. Announce copy success only after clipboard write succeeds. Failure opens selectable same-content text. Exports redact sensitive session tokens and evidence paths, not silently invent missing hashes. P2 search/filter operates on actual records.

## 8. Ecosystem catalogue and website composition

Carry the reference’s six categories into the P3/P4 catalogue: Wallets, DeFi routing, Liquid staking, DePIN networks, Decentralized storage, Explorers. SKR is a separately described Solana Mobile ecosystem integration, not the app’s token. List supplied provider names as Proposed catalogue entries until their specific operation is qualified.

Each entry includes status, supported operation, account/network requirement, last successful observation when relevant, source, privacy/permission implications, and next available action. A wallet supported through MWA does not mean every feature of that wallet is integrated. An explorer link can be labeled External resource; it does not require or imply an authenticated adapter. Storage upload requires consent and a real supported service; content addressing is not a persistence guarantee.

Desktop hero: max content width 1200px, brand/header and copy above a centered product preview with three category cards on each side. Connectors are decorative SVG paths behind cards, not data-flow assertions. Tablet collapses to two columns; phone uses copy → preview → category list, no tiny unreadable six-card poster. A marketing device screenshot is labeled Concept until replaced by an actual build capture. Every category card has enough opaque backing to remain readable over the scene.

Header navigation leads to actual routes. Footer uses **Built by CodesbyFebin**, project status, privacy and real source/documentation links. Hero copy **One workspace. Clearer records. Connected ecosystems as they qualify.** Benefit cards: **Review before signing**, **Organize evidence across tasks**, **Inspect transaction status**. Supporting text says hashes support integrity; local signing is distinct from settlement. Never promise that evidence is automatically stored on-chain. Keep **Proposed integrations • UI/UX concept • No partnership implied** on concept artwork, but use per-entry actual statuses on the working site.

## 9. Accessibility, adaptation and motion

Phone baseline 360–414dp with 16dp gutters; support 320dp compact widths and 200% text without clipping. Bottom navigation clears system gestures/insets. Actions remain reachable with keyboard open. At 600dp use a navigation rail or suitable platform-adaptive destination controls, and optional list/detail panes; do not show two conflicting live approval contexts. Preserve drafts/selection on rotation, invalidate sensitive approval after process restoration as the master requires.

All interactive elements have semantic role/name/state. Decorative icons have no redundant spoken descriptions. Compose text buttons already supply labels; avoid duplicate contentDescription announcements. Critical state changes announce accurate text such as **Message changed; approval unavailable**, **Submitted; awaiting confirmation**, not generic Approved. Full key copy is accessible through an explicit action; long press is optional, not the only path.

Use 4.5:1 normal-text and 3:1 large-text/nontext component contrast targets, measure both themes and state variants, and document actual checks. Critical helper text stays readable even when an action is disabled. Web uses native links/buttons, logical focus, semantic headings and an explicit modal focus-return contract. Focused controls are never hidden behind the action bar.

Motion: 120ms press feedback, 200ms sheet/route changes, at most 300ms discretionary fades. Respect reduced-motion/system animation settings. No continuous neon pulses around verdicts; loading animations do not imply chain progress. No forced one-second splash delay; use system launch behavior and show real loading when needed. Do not scale large screens or blur text as feedback. Haptic feedback is optional and must not announce confirmation.

## 10. Implementation handoff and acceptance

Compose building blocks: DeproofTheme, BrandHeader, NetworkChip, WalletCard, BalanceCard, PolicyCard, AddressRow, HashRow, EvidenceCard, ReceiptRow, IntegrationCard, ErrorPanel, ApprovalBar. Map colors and typography through semantic roles, not inline hex values. Keep UI projections separate from policy/state logic. Present real domain models; previews use clearly labeled synthetic preview data and never become production fallback.

Website counterparts share token names and content-status semantics. Use CSS custom properties and components corresponding to cards/chips/rows rather than duplicating independently styled pages. The ecosystem catalogue can read the verified registry at build time; it cannot mark a provider qualified from marketing metadata alone.

Design acceptance checklist:

1. Rounded DE geometry is consistent; no S, standalone D or angular alternate accidentally shipped.
2. Three core destinations remain intact; future workflows have explicit phase/route mapping.
3. Observed account, wallet session, network and RPC health are independent states.
4. Every transaction instruction and binding is reachable; no historical Approve or policy-is-safe claim.
5. Review UI gates reflect the full master predicates and strict sign/validate/submit flow.
6. Local evidence, wallet signing, submission and chain settlement remain visibly separate.
7. Planned ecosystem entries do not resemble working connect/swap/stake controls.
8. Dark/pearl themes, large text, touch targets, focus, screen reader and reduced motion pass meaningful checks.
9. Screens handle loading/empty/error/unavailable/stale/success and submit-unknown; no fabricated list rows or status.
10. Tokens/components are shared, approved assets are recorded, sources/statuses/privacy copy match implementation.

Verify Compose semantics and critical route/state interactions, font-scale screenshots and contrast for actual foreground/background pairs; manually qualify wallet/system handoff on a device. Web checks include narrow/desktop widths, keyboard focus, real links and per-entry status. Do not call the spec or component previews device-qualified. Provide actual screenshots and evidence paths once implemented.

## 11. Design-source corrections incorporated

| Source issue | Replacement |
| --- | --- |
| Ecosystem poster angular logo versus supplied rounded mark | Separate rounded logo reference is authoritative |
| SKR called DEPROOF utility token | Solana Mobile ecosystem integration; no Deproof-issued-token claim |
| Green Payable + checkmark | Neutral policy eligibility card; no Safe guarantee |
| Global network selector | Action-specific cluster plus explicit reauthorization/invalidation |
| RPC badge labeled only Mainnet/Devnet | Separate network identity from actual RPC health |
| One shared slot for independent balance reads | Per-result context and time |
| Message hash singular | Distinct card, full message and evidence digest labels |
| Approve immediately returns a receipt | Sign → validate returned bytes → submit → observe; unknown outcome represented |
| Receipt Signed/Rejected only | Outcome, submission provenance, local signature and chain state separated |
| Slot shown only if broadcast | Chain observation may have slot without app submission |
| Raw submittedByClearance in user UI | Submitted by Deproof wording, legacy import/export only |
| 44dp Android targets and mixed px/dp | 48dp Android target; dp layout/sp text, CSS units for website |
| Disabled control explanation via tooltip | Persistent readable inline reason |
| Alt/contentDescription on every icon | Platform semantics with decorative icons excluded and no duplicate labels |
| One-second forced splash and pulsing states | Platform launch behavior, bounded/reduced motion |
| Full RPC error string displayed | Named code/method with redacted safe detail |
| Capture evidence means file import in P1 | Import evidence in P1, real capture in P2 |
| Glow in all contexts | Marketing/brand emphasis only; opaque operational surfaces |
| Copy toast always succeeds | Announce only actual clipboard success; selectable fallback |

## 12. Start prompt for the implementing agent

Read DEPROOF_FINAL_MASTER_PROMPT.md in full, then this design specification, then design-tokens.json. Inspect the two supplied reference PNGs. Treat the rounded logo as brand authority and the ecosystem poster as composition inspiration. Build the native Android P1 Now/Review/Receipts flow first, using these shared semantic components and exact security/provenance states. Preserve Tasks/Evidence and the six-category ecosystem vision through staged nested routes and P3/P4 catalogue/website designs. Do not fabricate an adapter, wallet, signature or status to resemble the concept image. Keep all 120 product features, 100 core checks and 62 contracts mapped and evidence-gated. Continue meaningful implementation through the authorized phases, recording concrete blockers and device checks honestly. Produce executable code and screenshots only after the associated behavior runs.


## 13. Mandatory ecosystem workflow extension — v2.0

The full requested scope now includes user-controlled edge/local hosting, consented bandwidth contribution and actual prover workflows. These are mandatory staged requirements E001–E030 in the unified master, not merely decorative ecosystem categories. Keep three destinations: Now hosts nested Nodes, Bandwidth and Proof jobs; Review controls Solana authorization; Receipts exposes local contribution/verification and chain provenance separately.

| View | Minimum visible fields | Primary controls and uncertainty |
| --- | --- | --- |
| Nodes | Approved host fingerprint, authenticated pairing, actual host/runtime, resource limits, health freshness, observed service state | Pair/revoke; start/stop qualified profile; command accepted is not healthy service |
| Bandwidth | Consent scope, network/provider, counter units/source, current interval, cap remaining, peer/provider acceptance, payout status | Enable only after consent; pause/stop; unknown earnings remain unknown |
| Proof jobs | Job/backend/circuit identity, input commitment, compatible host, deadline, execution result, independent verifier observation | Accept/cancel; inspect result; local verification is not third-party reward acceptance |
| Contribution receipt | Producer identity, exact signed event, metering/proof provenance, verifier domain, reward/chain observations | Export verification bundle; distinguish local claim, corroboration, verified proof and paid status |

Both new banners are concept references under assets/reference. The thirty-solution banner uses an illustrative four-tab phone; this does not change three-destination implementation. Its prover-card wording about verifying real-world activity is not an assurance claim: runtime copy must identify the actual proof statement and separately qualified contribution evidence. No sampled node count, traffic, reward or proof result from a concept becomes production data.

The six solution groups each contain five mapped requirements: Solana Mobile, SKR Workspace, Edge & Local Hosting, Bandwidth Sharing, Prover Network, Evidence & Control. The six original provider categories remain a separate catalogue beneath this operator-workflow view. Both consume actual registry status; external-link availability cannot imply an operational provider adapter.

Credit Built by CodesbyFebin on public website, repository and banner; Android About contains creator credit. SKR retains Solana Mobile ecosystem identity. Use the same rounded DEPR logo and measured token roles throughout.
