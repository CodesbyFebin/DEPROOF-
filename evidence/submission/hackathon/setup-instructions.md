# Deproof — Setup Instructions

**Built by CodesbyFebin**

## Requirements

- Android device: API 28+ (Android 9+), or emulator with API 28+
- ADB installed and device connected
- Java 11+ (for Gradle)
- Android SDK (for build from source)
- Go 1.21+ (for node-agent)

## Install Debug APK (Quickest Path)

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

APK SHA-256: `99b062903a7aef396f619be85ead8eabfd19f056f847bf325c5ac5e87e9ff2aa`

## Build from Source

```bash
git clone https://github.com/CodesbyFebin/DEPROOF-.git
cd DEPROOF-
git checkout c95c4f1

# Build debug APK
./gradlew :app:assembleDebug

# Run JVM unit tests
./gradlew :app:testDebugUnitTest

# Build node-agent
cd node-agent && go build ./...

# Run Go unit tests
go test ./...
```

## Build Signed Release APK

```bash
export DEPROOF_KEYSTORE_PATH=/path/to/deproof-release.jks
export DEPROOF_KEYSTORE_PASSWORD=<password>
export DEPROOF_KEY_ALIAS=deproof-key
export DEPROOF_KEY_PASSWORD=<password>
./gradlew :app:assembleRelease
```

## Wallet Interaction (Physical Device Required)

Wallet connection, signing, and SKR payment review require:
1. Physical Android device (not emulator)
2. Phantom, Solflare, or another MWA-compatible wallet app installed
3. Wallet funded on Solana devnet for test flows

On an emulator, the wallet connection screen will display but wallet operations will fail with `MWA_WALLET_CALLBACK_NOT_IMPLEMENTED`.

## Running the Node Agent

```bash
cd node-agent
./deproof-node --help
```

## Reviewing SKR Qualification Results

```bash
cat evidence/qualification/skr-integration.json
```

All 16 automated checks are PASS. Three manual gates (on-chain RPC, devnet payment, oracle) remain BLOCKED.
