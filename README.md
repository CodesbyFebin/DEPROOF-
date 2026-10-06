# Deproof: Solana Mobile Verification dApp

Open-source DePIN verification workspace for Solana Mobile with MWA support.

## Quick Start

```bash
bash deproof-init.sh
bash deproof-master-build.sh preflight
MAX_PARALLEL=2 bash deproof-master-build.sh build
cat .deproof-runs/*/reports/BUILD_SUMMARY.md
```

## Features (P1 MVP)

- **Now Screen:** SOL/SKR balance reading, transaction history
- **Review Screen:** TransferChecked decoding, message binding, tamper detection
- **Receipts:** Local persistence, JSON export, clipboard copy
- **MWA Integration:** Real wallet signing (Phantom, Backpack)
- **Evidence-Based Gating:** APK build, unit tests, checksums

## Architecture

- **P1:** Android MVP (Kotlin, Compose)
- **P2:** Workflows & Evidence
- **P3:** Node agent, Prover, Website (Parallel)
- **P4:** Public release
- **P5:** Extended ecosystem

## License

MIT

---
Built with Deproof Master Build System
