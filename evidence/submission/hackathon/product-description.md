# Deproof — Product Description

**Built by CodesbyFebin**

## What Deproof Is

Deproof is a local-first contribution workspace for Solana Mobile. It connects user-owned node operations, exact-message transaction review, supported proof verification and portable contribution receipts. Its proposed SKR workflow links verified work to explicitly approved payments.

## Core Problem

DePIN contributors operate hardware across multiple networks but have no unified, on-device workspace for tracking what work was done, what was submitted, and what payments are being proposed. Receipts are scattered; payment approval is opaque; proof verification happens off-device if at all.

## What Deproof Provides

- **Multi-project DePIN workspace**: A single Android app aggregating contribution activity across Helium, Hivemapper, Nosana, Render, GEODNET, and latency-fixture adapters.
- **Exact-message transaction review**: Every SKR payment transaction is displayed to the user before signing — amount, destination, mint address, authorization scope — with no auto-approval.
- **Portable contribution receipts**: Structured evidence envelopes (schema `evidence-envelope-v2`) generated locally, exportable, and verifiable without a network connection.
- **SKR payment construction workflow**: Proposed payments are built as `SkrPaymentJob` objects (AUTHORIZATION=CONSTRUCTION_ONLY), displayed in `SkrPaymentReviewScreen`, and require explicit user confirmation before any signing step.
- **Go node-agent**: Companion node software implementing the same SKR payment construction logic in Go, with 46 unit tests covering construction, duplicate rejection, and serialization.
- **MWA wallet wiring**: `Wallet.kt` uses `com.solana.mobilewalletadapter.clientlib` stubs for build-time compatibility; full signing requires a physical device with Phantom, Solflare, or another MWA-compatible wallet.

## What Deproof Is Not

- Not a live DePIN oracle or proof assurance system. Proof verification is supported infrastructure; production proof assurance is not guaranteed.
- Not a mainnet payment executor. AUTHORIZATION=CONSTRUCTION_ONLY is enforced in both Go and Kotlin SKR paths.
- SKR ($DEPR) branding is distinct. No partnerships are claimed.

## Implementation Status Summary

| Component | Status |
|-----------|--------|
| Android app (Kotlin/Compose) | Builds and runs; 125 JVM unit tests pass |
| Go node-agent | Builds; 46 unit tests pass |
| SKR payment construction (Go + Kotlin) | Complete; 16/16 qualification checks pass |
| Multi-project DePIN workspace (6 adapters) | Complete; skrIntegration=BLOCKED on 5 adapters (expected) |
| MWA wallet stubs for emulator builds | Present; physical device required for real wallet signing |
| Proof verification | Supported infrastructure; not production proof assurance |
| On-chain verification (RPC) | BLOCKED — requires live Solana RPC |
| End-to-end payment submission | BLOCKED — requires devnet, funded wallet, physical device |
