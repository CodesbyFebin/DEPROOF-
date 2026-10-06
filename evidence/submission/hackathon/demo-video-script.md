# Deproof — Demo Video Script

**CLOCK IN Hackathon Submission**
**Built by CodesbyFebin**

---

## Overview (30 seconds)

**Scene**: App launch on Android device or emulator

**Narration**:
> "Deproof is a local-first contribution workspace for Solana Mobile. It lets DePIN contributors track their node work, review proposed payments in exact detail, and generate portable receipts — all on-device, without trusting a centralized dashboard."

Show: App launch screen, workspace list view.

---

## Scene 1: Multi-Project DePIN Workspace (45 seconds)

**Screen**: Workspace tab showing multiple DePIN adapters

**Narration**:
> "The workspace aggregates contribution data across multiple DePIN networks. Currently integrated: Helium, Hivemapper, Nosana, Render, and GEODNET. Each adapter shows its SKR integration status. Most are BLOCKED pending on-chain verification — that's by design. We don't pretend to have data we don't have."

Show: Adapter list with skrIntegration status labels.

**Key point to demonstrate**: The `skrIntegration=BLOCKED` labels are intentional honest status, not errors.

---

## Scene 2: SKR Payment Review Screen (60 seconds)

**Screen**: Navigate to SKR payment review

**Narration**:
> "When a node reports completed work, Deproof constructs a proposed SKR payment job. This screen shows you exactly what would be signed: the destination address, the exact amount in SKR with full decimal precision, the mint address, and the token program. Nothing is hidden."

Show:
- Payment amount with 6 decimal places
- Destination wallet address
- SKR mint: `SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3`
- Token Program: `TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA`
- AUTHORIZATION=CONSTRUCTION_ONLY label

**Narration continued**:
> "AUTHORIZATION=CONSTRUCTION_ONLY means this build constructs the transaction for review — it does not submit to mainnet. No tokens are spent without explicit approval on a live network."

---

## Scene 3: Portable Contribution Receipt (45 seconds)

**Screen**: Receipt view for a contribution

**Narration**:
> "After node work is verified, Deproof generates a portable receipt — a structured evidence envelope you can export and verify offline. The receipt contains the observation data, timestamps, and a local signing key attestation. It travels with you, not with the platform."

Show: Receipt detail view, export option.

---

## Scene 4: Wallet Connection (30 seconds — emulator limitation noted)

**Screen**: Wallet connection screen

**Narration**:
> "The wallet screen shows available MWA-compatible wallets. On a physical device with Phantom or Solflare installed, you can connect and authorize the app. On this emulator, the wallet list shows but connection requires physical hardware — that's an honest limitation we document clearly."

Show: Wallet screen with wallet list and "No wallets found" or connection attempt failing with descriptive error.

---

## Scene 5: Go Node Agent (30 seconds)

**Screen**: Terminal

**Narration**:
> "Deproof includes a Go node agent that runs on the contributing node itself. It implements the same SKR payment construction logic as the Android app, with 46 unit tests covering construction, duplicate rejection, and serialization."

Show: `go test ./... -v` output with all tests passing.

---

## Known Limitations to Acknowledge On Camera

1. **Wallet signing**: Requires physical device. Emulator will show wallet UI but connection fails.
2. **Live oracle data**: Not connected to live DePIN oracle endpoints. Demonstration uses local data.
3. **On-chain proof verification**: Infrastructure is present; production proof assurance requires live RPC.
4. **SKR payments**: CONSTRUCTION_ONLY. No mainnet spending occurs in this demo.

---

## Recording Checklist

- [ ] Record on emulator (API 28+) for reproducibility
- [ ] Note emulator limitations on screen during wallet scene
- [ ] Show actual test output for Go and Kotlin suites
- [ ] Display `evidence/qualification/skr-integration.json` PASS summary
- [ ] Do NOT attempt live mainnet transactions
