# Deproof — Pitch Presentation

**CLOCK IN Hackathon · October 8, 2026**
**Built by CodesbyFebin**

---

## Slide 1: Problem

DePIN contributors run hardware for Helium, Hivemapper, Nosana, Render, and other networks.

But:
- Their contribution receipts live on third-party dashboards
- Payment proposals are opaque — you don't see what you're signing
- There is no unified on-device workspace
- Proof verification happens off-device, if at all

**Result**: Contributors trust platforms they cannot inspect.

---

## Slide 2: Solution — Deproof

**Deproof** is a local-first contribution workspace for Solana Mobile.

It connects:
- User-owned node operations
- Exact-message transaction review
- Supported proof verification
- Portable contribution receipts

The proposed SKR workflow links verified work to explicitly approved payments.

---

## Slide 3: How It Works

```
Node work happens
       ↓
Go node-agent observes and constructs SkrPaymentJob
       ↓
Android app displays EXACT payment details for review
  (amount · destination · mint · token program · decimals)
       ↓
User reviews and authorizes on their own device
       ↓
Portable receipt generated locally
```

**AUTHORIZATION=CONSTRUCTION_ONLY**: No mainnet spending without explicit user action.

---

## Slide 4: What's Built

| Component | Status |
|-----------|--------|
| Android app (Kotlin/Compose) | ✓ Builds; 125 JVM tests PASS |
| Go node-agent | ✓ Builds; 46 tests PASS |
| SKR payment construction | ✓ 16/16 qualification checks PASS |
| Multi-project DePIN workspace | ✓ 6 adapters (Helium, Hivemapper, Nosana, Render, GEODNET, latency-fixture) |
| Exact-message review screen | ✓ Full transaction detail display |
| Portable receipts | ✓ Evidence envelope schema v2 |
| MWA wallet wiring | ✓ Physical device required for signing |

---

## Slide 5: What Deproof Is Not (Honest Limitations)

- **Not a live oracle**: Adapter data requires real oracle integration (BLOCKED pending)
- **Not production proof assurance**: Proof infrastructure is present; assurance is not guaranteed
- **Not a mainnet payment system**: CONSTRUCTION_ONLY enforced
- **Not claiming partnerships**: SKR and $DEPR are distinct; no guaranteed rewards

These limitations are documented, gated, and labeled in the app.

---

## Slide 6: Stack

- **Android**: Kotlin + Jetpack Compose, AGP 9.1.1, Kotlin 2.4.20
- **Wallet**: Solana Mobile Wallet Adapter (MWA v2 stubs; physical device for production)
- **Node**: Go 1.21, custom SKR payment construction
- **Contracts**: JSON Schema v2 for evidence envelopes, receipts, SKR payment jobs
- **Tests**: 125 Kotlin JVM + 46 Go unit tests

---

## Slide 7: For Solana Mobile

Deproof is purpose-built for Saga / dApp Store:
- Local-first: no cloud dependency for core workflows
- User-owned receipts: portable evidence envelopes
- Hardware wallet integration: MWA v2 for transaction review
- Honest status: BLOCKED labels over fake success

**Source**: https://github.com/CodesbyFebin/DEPROOF-.git @ c95c4f1
