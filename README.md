# Deproof - Android Transaction Review App

**See, understand, verify before you sign.**

Deproof is an Android app for reviewing Solana transactions before signing them with a mobile wallet. Built for the CLOCK IN Solana Mobile Hackathon.

## Features

### Core (Verified)
- ✓ Paste mainnet address, read SOL + official SKR balances from RPC
- ✓ Fetch last 10 signatures and confirm status
- ✓ Decode Token program instructions (TransferChecked, refuse dangerous ops)
- ✓ Refuse SOL stake operations (not SKR)
- ✓ Message hashing (SHA-256) and tampering detection (MESSAGE_CHANGED)
- ✓ Base58 encoding/decoding for keys
- ✓ Dark/light theme with mint accents

### Partial (Implemented, Untested Due to SDK)
- 🔶 RPC client for Mainnet Beta reads
- 🔶 Instruction decoder for all Token program discriminators
- 🔶 Review message canonicalization and double-hash binding
- 🔶 Navigation skeleton (Now, Review, Receipts screens)

### Later
- ⏳ Review screen (full instruction display + Approve/Reject)
- ⏳ Receipts screen (list + JSON export)
- ⏳ MWA wallet integration (connect + sign devnet memo)
- ⏳ Receipt persistence (Room database)
- ⏳ Evidence capture (file import + local ECDSA signing)
- ⏳ Grok AI query (optional safety notes)

## Build

### Requirements
- Java 17+
- Android SDK 34+
- Kotlin 1.9.10+

### Setup

```bash
cd deproof-app

# Set Android SDK location
export ANDROID_SDK_ROOT=/path/to/android/sdk
echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties

# Download SDK components (if needed)
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

### Build Debug APK

```bash
# Configure API keys
printf 'GEMINI_API_KEY=MY_GEMINI_API_KEY\nGROK_API_KEY=MY_GROK_API_KEY\n' > .env
printf 'GEMINI_API_KEY=MY_GEMINI_API_KEY\nGROK_API_KEY=MY_GROK_API_KEY\n' > app/.env

# Build
./gradlew assembleDebug --rerun-tasks

# Test
./gradlew testDebugUnitTest

# Output APK
# app/build/outputs/apk/debug/app-debug.apk
```

## Specification

- **Package:** `com.example`
- **ApplicationId:** `com.aistudio.deproof.sdwk`
- **Namespace:** `com.example`
- **Min SDK:** 24
- **Target SDK:** 34
- **Language:** Kotlin (no Java)
- **UI:** Jetpack Compose + Material 3
- **Database:** Room (for receipts, when implemented)
- **Wallet:** MWA (Mobile Wallet Adapter) 2.0.7

## Truth Lock Constants

| Constant | Value |
| --- | --- |
| Token Program | `TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA` |
| Official SKR Mint | `SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3` |
| SKR Decimals | **6** (not 9) |
| SOL Stake Program | `Stake11111111111111111111111111111111111111` |
| Mainnet RPC | `https://api.mainnet-beta.solana.com` |
| Devnet RPC | `https://api.devnet.solana.com` |

## Project Structure

```
app/src/main/java/com/example/
  MainActivity.kt                   # Navigation + three-screen layout
  util/Formatting.kt               # formatSol, formatSkr, isPubkey, hashing
  chain/Models.kt                  # RPC response models
  chain/Rpc.kt                     # Mainnet RPC client
  domain/Decode.kt                 # Instruction decoder
  domain/ReviewBinding.kt          # Message hash + tamper detection
  ui/theme/Theme.kt                # Dark/light colors
  ui/screens/NowScreen.kt          # Account info + signatures
  ui/screens/ReviewScreen.kt       # LATER: Verdict + approve/reject
  ui/screens/ReceiptsScreen.kt     # LATER: Receipt list + export

app/src/test/java/com/example/
  ShipTest.kt                      # 34 unit tests

docs/
  spec-resolution.md               # Spec conflicts resolved
  status.md                        # Build + feature status
```

## Unit Tests (34 tests)

**Run locally:**
```bash
./gradlew testDebugUnitTest
```

**Coverage:**
- Address validation (4 tests)
- Number formatting (5 tests)
- Instruction decoding (8 tests)
- Message hashing & tampering (6 tests)
- Wallet session logic (1 test)
- Receipt schema (1 test)
- Grok integration (1 test)
- Helper functions (7 tests)

## Design System

### Colors
**Dark mode (default):**
- Background: Obsidian `#0B1020`
- Surface: Navy `#121A2B`
- Text: `#E7EEF8`
- Primary: Mint `#3DDC97`
- Accent: Cyan `#7FE7F0`

**Light mode:**
- Background: Pearl `#F7F4EF`
- Surface: White
- Text: Ink `#1A1714`
- Primary: Mint `#1F8A62`
- Accent: Cyan `#0099CC`

### Screens
1. **Now:** Paste address, see balances + last 10 signatures
2. **Review:** (LATER) Decode transaction, show verdict, Approve/Reject
3. **Receipts:** (LATER) Local list of signed/rejected transactions, export JSON

## Honesty

Deproof does not:
- ✗ Invent SOL balances
- ✗ Fabricate wallet signatures
- ✗ Confuse SOL stake with SKR transfers
- ✗ Claim "safe to sign" without user review
- ✗ Submit transactions without user approval
- ✗ Cache balances after RPC error

This app helps you verify transactions. **You make the final call.**

## Author

**Febin Francis** — CodesbyFebin  
CLOCK IN Solana Mobile Hackathon 2026

## License

(TBD)

---

**Status:** Foundation complete. Core features verified. Ready for local build + SDK testing.
