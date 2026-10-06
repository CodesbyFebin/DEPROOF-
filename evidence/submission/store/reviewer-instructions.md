# Deproof — Reviewer Installation and Testing Instructions

**Built by CodesbyFebin**
**Contact**: codesbyfebin@gmail.com

## Installation

### Debug APK (recommended for review)

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

SHA-256: `99b062903a7aef396f619be85ead8eabfd19f056f847bf325c5ac5e87e9ff2aa`

Minimum Android version: API 28 (Android 9.0)

### Emulator Setup

An API 28+ AVD (x86_64) can be used for most UI navigation. An emulator with Google Play Services is NOT required. The following features work on emulator:
- App launch, navigation, and all screens
- DePIN workspace and adapter status display
- SKR payment review screen (with test data)
- Receipt viewing and export UI
- Settings and configuration

### Physical Device (additional features)

These features require a physical Android device with a Solana wallet app installed:
- Wallet connection (MWA)
- Transaction signing
- End-to-end SKR payment review with real wallet confirmation

## Test Flows

### 1. App Launch and Navigation

1. Launch the app
2. Verify home screen displays workspace
3. Navigate to each tab (Workspace, Receipts, Settings)
4. Verify no crashes on navigation

### 2. DePIN Workspace

1. Open the workspace tab
2. Verify adapter list displays (Helium, Hivemapper, Nosana, Render, GEODNET, latency-fixture)
3. Verify each adapter shows a status (most show `skrIntegration=BLOCKED` — this is correct)
4. Tap on latency-fixture adapter to see a fully wired adapter

### 3. SKR Payment Review Screen

1. Navigate to any adapter with available work
2. Trigger a payment job (or use the demo data path)
3. Verify the review screen shows:
   - Exact SKR amount with 6 decimal places
   - Destination address
   - SKR mint: `SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3`
   - Token Program: `TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA`
   - AUTHORIZATION=CONSTRUCTION_ONLY label
4. Verify the "Sign" button does NOT submit a transaction on emulator (should fail with wallet error)

### 4. Wallet Connection (Physical Device Only)

1. Navigate to the Wallet tab
2. Tap "Connect" with Phantom or Solflare installed
3. Wallet app should open for authorization
4. After authorization, verify public key is displayed

### 5. Receipt Flow

1. Navigate to Receipts tab
2. Verify receipt list loads
3. Tap a receipt to view details
4. Test export functionality

## Known Issues / Expected Behaviors

| Behavior | Expected |
|----------|----------|
| Wallet connection fails on emulator | Expected — requires physical device |
| 5 of 6 adapters show `skrIntegration=BLOCKED` | Expected — oracle integration pending |
| SKR payment "Sign" fails on emulator | Expected — MWA requires physical device |
| Location permission requested on first proof | Expected — required for location-based proof observations |

## Build Verification

To verify the APK matches the published source:

```bash
git clone https://github.com/CodesbyFebin/DEPROOF-.git
cd DEPROOF-
git checkout c95c4f1
./gradlew :app:assembleDebug
sha256sum app/build/outputs/apk/debug/app-debug.apk
# Expected: 99b062903a7aef396f619be85ead8eabfd19f056f847bf325c5ac5e87e9ff2aa
```

Note: The SHA-256 will match only if built with the same Gradle/AGP version and in the same environment. Minor differences in build toolchain may produce a different checksum; the source is the authoritative artifact.
