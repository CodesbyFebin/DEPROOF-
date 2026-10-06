# Deproof Master Build Guide

## Quick Start

```bash
# 1. Initialize project
bash deproof-init.sh

# 2. Run preflight check
bash deproof-master-build.sh preflight

# 3. Build P1 + P3 parallel
MAX_PARALLEL=2 bash deproof-master-build.sh build

# 4. View results
cat .deproof-runs/*/reports/BUILD_SUMMARY.md
```

## Prerequisites

### macOS (Homebrew)
```bash
brew install openjdk@11 kotlin go android-sdk node gradle
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
```

### Linux (Ubuntu/Debian)
```bash
apt-get install openjdk-11-jdk-headless kotlin golang-go nodejs npm gradle
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export ANDROID_SDK_ROOT=$HOME/android-sdk
```

## Build Commands

### Preflight Check
```bash
bash deproof-master-build.sh preflight
```

### Full Build (P1 + P3 Parallel)
```bash
MAX_PARALLEL=2 REPAIR_ROUNDS=2 bash deproof-master-build.sh build
```

### Continue from Checkpoint
```bash
bash deproof-master-build.sh continue
```

### Verify Gates
```bash
bash deproof-master-build.sh verify
```

## Configuration

```bash
export PROJECT_ROOT=.
export MAX_PARALLEL=4
export REPAIR_ROUNDS=3
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
```

## Output Structure

```
.deproof-runs/
├── 20261006T120000Z-12345/
│   ├── logs/
│   │   ├── android-build.log
│   │   ├── android-tests.log
│   │   ├── node-agent.log
│   │   ├── prover-worker.log
│   │   └── web.log
│   ├── reports/
│   │   └── BUILD_SUMMARY.md
│   └── evidence/
│       ├── app-debug.apk
│       └── apk.sha256
```

## Troubleshooting

### Android SDK Not Found
```bash
export ANDROID_SDK_ROOT="/path/to/android-sdk"
bash deproof-master-build.sh preflight
```

### Gradle Build Fails
```bash
./gradlew clean
./gradlew assembleDebug --rerun-tasks
tail -100 .deproof-runs/*/logs/android-build.log
```

### Unit Tests Fail
```bash
./gradlew testDebugUnitTest --rerun-tasks
./gradlew testDebugUnitTest -i 2>&1 | grep -A 5 "FAILED"
```

## Installation on Device

```bash
# List devices
adb devices

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Verify installation
adb shell pm list packages | grep deproof
```

## Support

**Built by:** CodesbyFebin
**License:** MIT
**Blockchain:** Solana (mainnet-beta)

---
For detailed specifications, see: `docs/blueprint/DEPROOF_FINAL_MASTER_PROMPT.md`
