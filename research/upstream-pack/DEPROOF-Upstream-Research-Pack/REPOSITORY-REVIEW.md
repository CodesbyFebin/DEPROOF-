# DEPROOF upstream research review

Built for CodesbyFebin — Own Your Infrastructure. Carry Your Proof.

Reviewed 2026-10-07 UTC using read-only GitHub repository metadata, recursive path inventories and selected immutable blobs. The original list contains 42 links and 40 unique repositories (Solwave-kt and templates repeated). This is a targeted source review, not a full security audit of every line or a build qualification. All 40 repositories were contacted. One tree is empty; the other 39 tree responses were not truncated. Selected files were read and checked against Git blob SHA-1; supplied source bytes are unchanged.

## What to use first

1. Official `solana-mobile/templates`: current Kotlin Compose wallet connection and message-signing examples, build configuration, and optional Expo transport comparisons. Use only after checking versions against DEPROOF's actual checkout. The extracted subset is not a complete runnable template; binary wrapper JAR and assets are intentionally absent.
2. Official `solana-mobile/solana-mobile-docs`: Kotlin MWA integration, protocol spec, SKR asset and domain documentation. Documentation is a source for protocol research, not a live-chain test.
3. Community `seeker-sdk`: compare SKR reads and tests; rewrite precision, cluster-aware cache and unavailable/error handling before adopting any behavior. Staking layouts and program identity remain unqualified.
4. DAppNode: inspect package/Compose test patterns separately under GPL. It is an Ethereum-oriented host framework, not a drop-in Solana node agent. It supplies no proof of DEPROOF resource confinement.

## Important findings

- Framework-kit README explicitly says it is no longer maintained. Treat it as research and use its referenced replacements only after a separate review. It also has no detected repository license in this snapshot, so source is not redistributed here.
- The older official Kotlin scaffold, dapp-scaffold, mobile-doc-site and stack-sdk are marked archived by GitHub metadata. Official origin does not imply active maintenance.
- The dApp Store API link is a commercial-access README rather than a public SDK implementation. No store submission capability is obtained by copying that document.
- solanaappkit has an empty Git repository response. bloqly has a one-file tree. These cannot provide the requested application components.
- SKR token documentation identifies the mint and staking program, but no network calls, owner/decimal checks, staking transactions or provider integration were performed.
- No listed source proves a Hivello replacement, universal DePIN hosting, contributor payments or independent physical-world verification. No Hivello legacy runtime is integrated.
- The community SKR balance path uses `Number(account.amount)`, reads only the ATA and caches by wallet. DEPROOF requires integer raw units, all qualified token accounts, explicit cluster/token-program context and observation freshness. Invalid-owner errors must not become a balance of zero. Missing stake configuration must remain unavailable.
- Official Kotlin minimal template returns actual authorization and detached message-signature results, but its sample account-first selection, placeholder identity domain and message signing are not a complete reviewed transaction workflow.

## Every repository

| Repository | Decision | License detected | Files included |
|---|---|---|---|
| [0xCaptain888/SkillDock](https://github.com/0xCaptain888/SkillDock) | OUT_OF_SCOPE | MIT | 0 |
| [0xJonaseb11/mobile-dapp.3.0](https://github.com/0xJonaseb11/mobile-dapp.3.0) | LICENSE_BLOCKED | UNKNOWN | 0 |
| [4R7I5T/BioLLM_Solana_dApp](https://github.com/4R7I5T/BioLLM_Solana_dApp) | LICENSE_BLOCKED | UNKNOWN | 0 |
| [AleisterMoltley/dotLab](https://github.com/AleisterMoltley/dotLab) | OUT_OF_SCOPE | MIT | 0 |
| [Durgeshrajbhar/solanaappkit](https://github.com/Durgeshrajbhar/solanaappkit) | EMPTY | UNKNOWN | 0 |
| [GokiProtocol/walletkit](https://github.com/GokiProtocol/walletkit) | GPL_REFERENCE | GPL-3.0 | 8 |
| [Hamiltok387lz/solana-mobile-dapp-scaffold](https://github.com/Hamiltok387lz/solana-mobile-dapp-scaffold) | LICENSE_BLOCKED | UNKNOWN | 0 |
| [MetaMask/dapps](https://github.com/MetaMask/dapps) | OUT_OF_SCOPE | ISC | 0 |
| [MetaMask/test-dapp](https://github.com/MetaMask/test-dapp) | OUT_OF_SCOPE | MIT | 0 |
| [OpenFuturePlatform/open-api](https://github.com/OpenFuturePlatform/open-api) | OUT_OF_SCOPE | Apache-2.0 | 0 |
| [Quantum-Software-Development/solana-atlas-quantum-hub](https://github.com/Quantum-Software-Development/solana-atlas-quantum-hub) | OUT_OF_SCOPE | MIT | 0 |
| [Saganize/Solwave-kt](https://github.com/Saganize/Solwave-kt) | REVIEW_REQUIRED | Apache-2.0 | 8 |
| [Th3ryks/solana-dapp-store-api](https://github.com/Th3ryks/solana-dapp-store-api) | NOT_SDK | UNKNOWN | 0 |
| [beeman/solana-mobile-monorepo](https://github.com/beeman/solana-mobile-monorepo) | REFERENCE | MIT | 6 |
| [bloqly/bloqly](https://github.com/bloqly/bloqly) | NOT_SDK | UNKNOWN | 0 |
| [dappnode/DAppNode](https://github.com/dappnode/DAppNode) | GPL_REFERENCE | GPL-3.0 | 4 |
| [dappnode/DAppNodeSDK](https://github.com/dappnode/DAppNodeSDK) | GPL_REFERENCE | GPL-3.0 | 8 |
| [dappuniversity/starter_kit](https://github.com/dappuniversity/starter_kit) | LICENSE_BLOCKED | UNKNOWN | 0 |
| [ethereum/dapp-bin](https://github.com/ethereum/dapp-bin) | OUT_OF_SCOPE | MIT | 0 |
| [hariFED/O-Chat](https://github.com/hariFED/O-Chat) | LICENSE_BLOCKED | UNKNOWN | 0 |
| [hironate/token-airdrop-contract](https://github.com/hironate/token-airdrop-contract) | OUT_OF_SCOPE | UNKNOWN | 0 |
| [lukasbrook/solana_mobile_vibe_kit](https://github.com/lukasbrook/solana_mobile_vibe_kit) | LICENSE_BLOCKED | UNKNOWN | 0 |
| [merigo-labs/solana-web3](https://github.com/merigo-labs/solana-web3) | OUT_OF_SCOPE | MIT | 0 |
| [reown-com/web-examples](https://github.com/reown-com/web-examples) | REFERENCE | Apache-2.0 | 8 |
| [rizzytoday/fullport](https://github.com/rizzytoday/fullport) | REFERENCE | MIT | 8 |
| [ronanyeah/pow-dapp](https://github.com/ronanyeah/pow-dapp) | LICENSE_BLOCKED | UNKNOWN | 0 |
| [ronanyeah/solana-connect](https://github.com/ronanyeah/solana-connect) | REFERENCE | MIT | 4 |
| [saicharanpogul/seeker-sdk](https://github.com/saicharanpogul/seeker-sdk) | REVIEW_REQUIRED | MIT | 12 |
| [sepivip/SeekerClaw](https://github.com/sepivip/SeekerClaw) | REFERENCE | MIT | 9 |
| [solana-foundation/create-solana-dapp](https://github.com/solana-foundation/create-solana-dapp) | REFERENCE | MIT | 8 |
| [solana-foundation/framework-kit](https://github.com/solana-foundation/framework-kit) | DEPRECATED_REFERENCE | UNKNOWN | 0 |
| [solana-labs/dapp-scaffold](https://github.com/solana-labs/dapp-scaffold) | ARCHIVED_REFERENCE | Apache-2.0 | 6 |
| [solana-mobile/solana-kotlin-compose-scaffold](https://github.com/solana-mobile/solana-kotlin-compose-scaffold) | LICENSE_BLOCKED | UNKNOWN | 0 |
| [solana-mobile/solana-mobile-doc-site](https://github.com/solana-mobile/solana-mobile-doc-site) | LICENSE_BLOCKED | UNKNOWN | 0 |
| [solana-mobile/solana-mobile-docs](https://github.com/solana-mobile/solana-mobile-docs) | PRIORITY | Apache-2.0 | 8 |
| [solana-mobile/solana-mobile-skills](https://github.com/solana-mobile/solana-mobile-skills) | REFERENCE | Apache-2.0 | 8 |
| [solana-mobile/solana-mobile-stack-sdk](https://github.com/solana-mobile/solana-mobile-stack-sdk) | ARCHIVED_REFERENCE | NOASSERTION | 0 |
| [solana-mobile/templates](https://github.com/solana-mobile/templates) | PRIORITY | Apache-2.0 | 16 |
| [transmute-industries/dapp](https://github.com/transmute-industries/dapp) | DEPRECATED_REFERENCE | UNKNOWN | 0 |
| [x402agent/SolanaOS](https://github.com/x402agent/SolanaOS) | OUT_OF_SCOPE | MIT | 0 |

### 0xCaptain888/SkillDock

Archived agent marketplace; autonomous purchasing and tokenized skills are not DEPROOF core. No implementation selected.

Branch: `main`. Tree: `13f70108fafe332f58f9d134895e2d3d99ebefe2`. Archived: `True`. Last push reported: `2026-03-31T14:43:42Z`. Qualification: **NOT_RUN**.

### 0xJonaseb11/mobile-dapp.3.0

Kotlin wallet connection example but no detected license. Do not redistribute source.

Branch: `main`. Tree: `2790d275cacf4c12a4867e3b9e0160f5cc8263b6`. Archived: `False`. Last push reported: `2024-06-30T22:08:23Z`. Qualification: **NOT_RUN**.

### 4R7I5T/BioLLM_Solana_dApp

No detected license; wallet creation/import and stored key service are outside noncustodial DEPROOF core.

Branch: `main`. Tree: `e1c2400d68206d432ac6d8fa5eaee5acea657c36`. Archived: `False`. Last push reported: `2026-04-30T04:01:41Z`. Qualification: **NOT_RUN**.

### AleisterMoltley/dotLab

Local AI game studio. Its host-enforced quality philosophy is interesting, but no DEPIN adapter or Android trust component selected.

Branch: `main`. Tree: `3aa4006db9e5d2c2d260706377f461635497e597`. Archived: `False`. Last push reported: `2026-08-14T09:49:45Z`. Qualification: **NOT_RUN**.

### Durgeshrajbhar/solanaappkit

Tree request returns 409 for empty repository; no implementation files available.

Branch: `main`. Tree: `None`. Archived: `False`. Last push reported: `2025-06-10T01:50:09Z`. Qualification: **NOT_RUN**.

### GokiProtocol/walletkit

Solana React wallet UI under GPL-3.0; optional web reference, not Android MWA.

Branch: `master`. Tree: `be0b834b2a12c06b8696a1e3b2c97dd992a7c6bd`. Archived: `False`. Last push reported: `2023-03-14T05:01:24Z`. Qualification: **NOT_RUN**.

### Hamiltok387lz/solana-mobile-dapp-scaffold

Third-party React Native scaffold without detected license. Use licensed official templates instead.

Branch: `main`. Tree: `d0a4f2edc0fa2605fc19bee70b53cd8f325b9137`. Archived: `False`. Last push reported: `2025-09-06T16:55:54Z`. Qualification: **NOT_RUN**.

### MetaMask/dapps

Archived Ethereum dApp collection; not the DEPROOF runtime.

Branch: `master`. Tree: `a8adba12ce14934c8a1f452a4cb33dcd1139b6fb`. Archived: `True`. Last push reported: `2025-05-13T17:29:57Z`. Qualification: **NOT_RUN**.

### MetaMask/test-dapp

Ethereum QA app, useful conceptually for explicit test cases but not the Solana wallet path.

Branch: `main`. Tree: `2252b59ca8a0d23a6186cc54731fcc9af29c5501`. Archived: `False`. Last push reported: `2026-10-02T11:11:30Z`. Qualification: **NOT_RUN**.

### OpenFuturePlatform/open-api

Kotlin backend/payment API patterns; not Android MWA or a supported DEPIN protocol.

Branch: `master`. Tree: `4661193eedaf68f56fc358146c03b061d7e90eec`. Archived: `False`. Last push reported: `2024-08-12T15:46:24Z`. Qualification: **NOT_RUN**.

### Quantum-Software-Development/solana-atlas-quantum-hub

Primarily documentation/research; no qualified DEPIN runtime selected.

Branch: `main`. Tree: `3db7bd36b57eb77717a304722c4100299fec02f7`. Archived: `False`. Last push reported: `2026-02-08T02:40:11Z`. Qualification: **NOT_RUN**.

### Saganize/Solwave-kt

Kotlin transaction integration alternative. Remote request models are not direct MWA qualification. Inspect service dependencies and authorization boundaries before considering.

Branch: `main`. Tree: `ea27a2b554ec01d337a35d8581d6d0df3f1bfe25`. Archived: `False`. Last push reported: `2024-01-18T11:44:04Z`. Qualification: **NOT_RUN**.

### Th3ryks/solana-dapp-store-api

One-file commercial access README; not an official submission SDK or downloadable implementation.

Branch: `main`. Tree: `344166e3559348106a453282786e7bfa9eaefb02`. Archived: `False`. Last push reported: `2026-03-24T14:30:15Z`. Qualification: **NOT_RUN**.

### beeman/solana-mobile-monorepo

Expo/mobile/web authorization patterns and optional backend topology. Do not make accounts or AI services mandatory for local evidence.

Branch: `main`. Tree: `8eeaa98690d14decdaec82058a5fa804613f1b05`. Archived: `False`. Last push reported: `2026-05-26T15:19:50Z`. Qualification: **NOT_RUN**.

### bloqly/bloqly

Single-file repository; no runtime source tree found.

Branch: `master`. Tree: `e1b09404af2aa4ac031f51f3742543e5c0608a52`. Archived: `False`. Last push reported: `2018-11-18T10:11:22Z`. Qualification: **NOT_RUN**.

### dappnode/DAppNode

Host Compose orchestration reference under GPL-3.0. Keep separate; it does not prove DEPROOF workload isolation or Solana provider support.

Branch: `master`. Tree: `289a8cd37d7bea4b45d5eba2718bdbb9c0aa5608`. Archived: `False`. Last push reported: `2026-09-13T08:05:51Z`. Qualification: **NOT_RUN**.

### dappnode/DAppNodeSDK

Package manifest and Compose tests under GPL-3.0. Preserve license obligations. Publishing/APM commands are not authorized or executed.

Branch: `master`. Tree: `abda22789ae9bcb470684c7dbfd5720568aa862e`. Archived: `False`. Last push reported: `2026-08-24T09:10:07Z`. Qualification: **NOT_RUN**.

### dappuniversity/starter_kit

Generic starter, no detected license. Not selected for Android DEPROOF.

Branch: `master`. Tree: `9c0e223c4f891e78620c3b6850816474df6939e3`. Archived: `False`. Last push reported: `2023-01-26T16:16:54Z`. Qualification: **NOT_RUN**.

### ethereum/dapp-bin

Archived Ethereum examples; not a Solana mobile foundation.

Branch: `master`. Tree: `578bb1dd60c0381ae5d4153aeab27f10025fde97`. Archived: `True`. Last push reported: `2024-06-14T22:35:51Z`. Qualification: **NOT_RUN**.

### hariFED/O-Chat

No detected license; chat application not selected for DEPIN evidence workflow.

Branch: `master`. Tree: `db8fe0ab4585bb8c74b30f1fb974be3a0287dad4`. Archived: `False`. Last push reported: `2025-07-02T05:19:31Z`. Qualification: **NOT_RUN**.

### hironate/token-airdrop-contract

Token-airdrop contract outside no-token-issuance DEPROOF scope; no source selected.

Branch: `master`. Tree: `c0f6968528234f083d678fd870482b34f31f59c2`. Archived: `False`. Last push reported: `2022-02-20T07:25:05Z`. Qualification: **NOT_RUN**.

### lukasbrook/solana_mobile_vibe_kit

Ionic/Capacitor SDK; README badge claims MIT but no license detected in tree. Badge is insufficient for extraction.

Branch: `main`. Tree: `6b51c995a60278ef84e885e007c6f1e394943516`. Archived: `False`. Last push reported: `2025-08-07T10:34:42Z`. Qualification: **NOT_RUN**.

### merigo-labs/solana-web3

Dart Solana RPC implementation; different language/runtime from Kotlin DEPROOF.

Branch: `master`. Tree: `927314b09bec1c7a26d282d6a171d069b93b16c8`. Archived: `False`. Last push reported: `2024-08-20T05:18:41Z`. Qualification: **NOT_RUN**.

### reown-com/web-examples

WalletConnect examples. Different transport and session model; optional future web support only.

Branch: `main`. Tree: `018ff93db1b0c2528396ac66165d09d126b87f81`. Archived: `False`. Last push reported: `2026-10-07T11:11:36Z`. Qualification: **NOT_RUN**.

### rizzytoday/fullport

React Native account/signing and SKR screen patterns. Portfolio scope is distinct from DEPROOF; no inherited balance, staking or device qualification.

Branch: `main`. Tree: `e1426b425fa8ef233c2c81cfa1d491cdf5fa3983`. Archived: `False`. Last push reported: `2026-02-21T22:32:26Z`. Qualification: **NOT_RUN**.

### ronanyeah/pow-dapp

Proof-of-work web example; no detected license and not a qualified useful-compute proof backend.

Branch: `master`. Tree: `3b7084e249aa71bac628a8f7ff6e7aca1dbc12ae`. Archived: `False`. Last push reported: `2024-07-13T11:10:09Z`. Qualification: **NOT_RUN**.

### ronanyeah/solana-connect

Standalone web wallet selection UI. Not a replacement for Android MWA or byte-bound approval.

Branch: `master`. Tree: `bff36ee9785b773abb01720d2231ba0c1881c44f`. Archived: `False`. Last push reported: `2025-02-27T19:43:49Z`. Qualification: **NOT_RUN**.

### saicharanpogul/seeker-sdk

Community SKR SDK. src/skr-token.ts converts bigint to Number, reads only ATA, caches by wallet without cluster, and treats invalid account owner as zero. Missing stake config returns empty state. Rewrite these paths; do not adopt financial output or staking layouts unverified.

Branch: `main`. Tree: `689b11d3933f79d7f0d311e14318ec51c646c38a`. Archived: `False`. Last push reported: `2026-03-25T13:30:06Z`. Qualification: **NOT_RUN**.

### sepivip/SeekerClaw

Android runtime state and bounded node-control test patterns. Agent tools and burner-wallet capabilities are outside DEPROOF core; do not import autonomous spending or arbitrary shell execution.

Branch: `main`. Tree: `a41c48b85e42e070362284602affa1ef116e5802`. Archived: `False`. Last push reported: `2026-09-24T07:27:49Z`. Qualification: **NOT_RUN**.

### solana-foundation/create-solana-dapp

Generator error-handling and tests. Do not run scaffolding over the existing DEPROOF checkout.

Branch: `main`. Tree: `e9c1bbafd9fee14b8feaa7329af1ab79454b3626`. Archived: `False`. Last push reported: `2026-09-19T19:20:22Z`. Qualification: **NOT_RUN**.

### solana-foundation/framework-kit

README explicitly says no longer maintained and recommends Kit plugins or Solana Kit. No detected repository license; source excluded.

Branch: `main`. Tree: `ec2a3e8815777328719b63addaba7ab43741ff6f`. Archived: `False`. Last push reported: `2026-10-01T19:22:51Z`. Qualification: **NOT_RUN**.

### solana-labs/dapp-scaffold

Archived Next.js wallet/signing scaffold. Legacy reference; not recommended as a fresh mobile foundation.

Branch: `main`. Tree: `48144509c643748c50fcabfaced48487ccb96b8d`. Archived: `True`. Last push reported: `2024-04-07T09:04:32Z`. Qualification: **NOT_RUN**.

### solana-mobile/solana-kotlin-compose-scaffold

Official but archived Kotlin scaffold; no detected license. Reference-only until permissions clarified; prefer current templates.

Branch: `main`. Tree: `f47072be1787baf511adc668b6d36f7c3dab1080`. Archived: `True`. Last push reported: `2024-07-15T11:08:18Z`. Qualification: **NOT_RUN**.

### solana-mobile/solana-mobile-doc-site

Archived documentation website with no detected license; prefer current solana-mobile-docs.

Branch: `main`. Tree: `c175cfcd02a8e0a980220b0b23ee968b631e5253`. Archived: `True`. Last push reported: `2026-02-05T18:09:24Z`. Qualification: **NOT_RUN**.

### solana-mobile/solana-mobile-docs

Official MWA and SKR documentation. Source for mint/program research; on-chain owner, decimals, layout and device flows still need qualification.

Branch: `main`. Tree: `d6d1d749ca92b6bd1398514b8cb8963865c04204`. Archived: `False`. Last push reported: `2026-10-01T04:07:22Z`. Qualification: **NOT_RUN**.

### solana-mobile/solana-mobile-skills

Official agent reference material. Included as inert documents, not installed or executed as instructions.

Branch: `main`. Tree: `cb22465fc4954f9b1a3ca0ce5cd57fd8f2c51171`. Archived: `False`. Last push reported: `2026-10-01T16:45:11Z`. Qualification: **NOT_RUN**.

### solana-mobile/solana-mobile-stack-sdk

Archived small umbrella repository, not a complete mobile SDK extraction. Metadata license NOASSERTION; source excluded.

Branch: `main`. Tree: `dcd59ad28e54472a41963566cbe6b3e2a93c334d`. Archived: `True`. Last push reported: `2024-05-08T16:27:14Z`. Qualification: **NOT_RUN**.

### solana-mobile/templates

Current official template examples; Kotlin Compose MWA connection and detached message signing are the closest fit. Template package identity and account-first assumptions need replacement.

Branch: `main`. Tree: `78ee28830644f63d9582363e97939e308068206b`. Archived: `False`. Last push reported: `2026-10-01T16:37:06Z`. Qualification: **NOT_RUN**.

### transmute-industries/dapp

README declares deprecated Ethereum project; no detected repository license. Source excluded.

Branch: `master`. Tree: `8d0c44d079264c6472d40dde3a5f3249915210e1`. Archived: `False`. Last push reported: `2017-10-31T17:09:17Z`. Qualification: **NOT_RUN**.

### x402agent/SolanaOS

Agent wallets/payment and trading-related tree. Avoid introducing custody or automatic spend paths. No implementation selected.

Branch: `main`. Tree: `37041b5d74e4da89b4f23b688588953911547651`. Archived: `False`. Last push reported: `2026-07-22T23:33:37Z`. Qualification: **NOT_RUN**.

## Scope and assurance

Builds, upstream tests, Android lint, emulator/device checks, live RPC, SKR staking, host isolation, provider acknowledgement and store submission: **NOT_RUN**. No DEPROOF registry ID is closed. No source has been merged, pushed, deployed, executed or installed. No token is issued. $DEPR remains a brand; SKR is a distinct asset and must not be depicted as a generic reward for unrelated providers.

License metadata is a screening signal, not a legal clearance of all nested dependencies. Included repository license files must travel with retained code; preserve source notices. GPL examples are separated. No-license files were inspected for research but excluded from redistribution. Review file-level and dependency licenses before actual adoption.
