# Deproof Specification Resolution

This document records the resolution of conflicts between two source specifications for the Deproof Android app build.

## Conflicts Resolved

| Conflict | Resolution | Reasoning |
| --- | --- | --- |
| Product name Deproof vs Clearance | Public name is **Deproof**. Receipt field `submittedByClearance` stays `false` until a real broadcast. | Deproof is the authoritative product name for this hackathon submission. |
| Native SOL stake treated as SKR stake | **Forbidden.** Label SOL stake operations distinctly. Do not sign. | SOL stake program (Stake11111111111111111111111111111111111111) is fundamentally different from SKR token transfers. Misrepresenting one as the other creates financial risk. |
| Fake wallet if MWA fails | **Forbidden.** Missing wallet is an error state. Do not invent signature. | User must have real MWA session to sign. Fabricating signatures violates the core trust model. |
| SKR staking program layouts | Official docs name `SKRskrmtL83pcL4YqLWt6iPefDqwXQWHSw9S9vz94BZ`, but no instruction layouts published. | Verdict: `SKR stake layout not verified. Do not sign.` until verified in official Solana Mobile documentation. Skip signing until layouts are public. |
| 48-hour cooldown and 10% inflation | Quotes from Solana Mobile docs read 2026-10-06. Local timer is observation only, not withdraw eligibility proof. | These are informational claims. Deproof displays them as quotes with source + date, never as verified on-chain state. Actual eligibility requires querying the pool contract. |
| Review hash: newline fields vs serialized tx message | **Both.** Card displays newline-canonical message hash. Also hash serialized Solana message bytes for wallet. Approve requires both unchanged. | Dual hashing ensures the human-readable review and the wallet's transaction bytes stay in sync. One changed, approval fails. |
| u64 as Kotlin Long | Long is signed (-2^63 to +2^63-1). Values above Long.MAX_VALUE use BigInteger. Never truncate or use Float. | u64 max is 2^64-1 (18,446,744,073,709,551,615). Kotlin Long cannot represent this without data loss. Use BigInteger for edge cases, format as exact decimal string. |
| "120 fully working features" | **Forbidden.** Report verified, implemented-unverified, later, and not-run counts honestly. No marketing exaggeration. | This build reports exactly which features are verified working, which are stubs, and which are out of scope. Honesty is the product. |

## Build Configuration

**Namespace:** `com.example`  
**ApplicationId:** `com.aistudio.deproof.sdwk`  
**Launch Activity:** `com.aistudio.deproof.sdwk/com.example.MainActivity`  
**Stack:** Kotlin + Jetpack Compose + Material 3  
**Min SDK:** 24  
**MWA Client:** `com.solanamobile:mobile-wallet-adapter-clientlib-ktx:2.0.7` (or latest 2.0.x)

## Truth Lock Constants

| Constant | Value |
| --- | --- |
| Token Program | `TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA` |
| Official SKR Mint | `SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3` |
| SKR Decimals | **6** (confirmed from mint account, not variable) |
| Native SOL Stake Program | `Stake11111111111111111111111111111111111111` |
| SKR Staking Program (quoted) | `SKRskrmtL83pcL4YqLWt6iPefDqwXQWHSw9S9vz94BZ` |
| Memo Program | `MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr` |
| System Program | `11111111111111111111111111111111` |

## Token Instruction Discriminators (SPL Token Program)

| Discriminator | Instruction | Action |
| --- | --- | --- |
| 3 | Transfer | Refuse: unchecked variant |
| 4 | Approve | Refuse: unauthorized |
| 5 | Revoke | Refuse: authority revocation |
| 6 | SetAuthority | Refuse: dangerous |
| 7 | MintTo | Refuse: mint operation |
| 8 | Burn | Refuse: burn operation |
| 9 | CloseAccount | Refuse: close operation |
| 12 | **TransferChecked** | **Payable only if: discriminator 12, 4 accounts, official mint, decimals=6, amount parsed** |
| 13 | ApproveChecked | Refuse: checked approve |
| 14 | BurnChecked | Refuse: checked burn |
| Unknown | Unknown | Refuse: `Unknown token instruction <n>. Do not sign.` |

## SOL Stake Discriminators (u32 Little-Endian)

| Discriminator | Operation | Label |
| --- | --- | --- |
| 0 | Initialize | `SOL stake: initialize` |
| 1 | Authorize | `SOL stake: authorize` |
| 2 | Delegate | `SOL stake: delegate` |
| 3 | Split | `SOL stake: split` |
| 4 | Withdraw | `SOL stake: withdraw` |
| 5 | Deactivate | `SOL stake: deactivate` |
| 7 | Merge | `SOL stake: merge` |

**Rule:** All SOL stake operations are labeled distinctly and refused. Never call a deactivate a SKR unstake.

## Build Status

**Date:** 2026-10-05  
**Spec Version:** 1.0 (authoritative)  
**Implementation Phase:** Foundation (Now screen + account reading)
