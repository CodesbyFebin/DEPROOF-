# Phase 1 Device Testing Guide

## Overview
Phase 1 captures evidence of the DEPROOF application running on Android, demonstrating:
- ✅ Observation parsing (AIOZ storage metrics)
- ✅ DeviceStatsScreen UI rendering
- ✅ Source digest audit trail
- ✅ Reward asset separation (AIOZ vs SKR)

## Prerequisites
- Android device with USB debugging enabled
- adb (Android Debug Bridge) installed and available
- USB cable connected to development machine
- `app-release.apk` built and ready

## Quick Start

### 1. Build Release APK
```bash
cd /home/user/deproof-
./gradlew assembleRelease -q
```

Output: `app/build/outputs/apk/release/app-release.apk` (≈21MB)

### 2. Run Test Script
```bash
./TEST_INSTRUCTIONS.sh app/build/outputs/apk/release/app-release.apk
```

### 3. Expected Output
The script will:
1. ✅ Verify device connectivity
2. ✅ Verify APK availability
3. ✅ Install APK on device
4. ✅ Create screenshot directories
5. ✅ Launch DEPROOF app
6. ✅ Capture 8 screenshots of DeviceStatsScreen
7. ✅ Record 90-second demo video

### 4. Evidence Location
- **Screenshots**: `./phase1-evidence/screenshots/screenshot-01.png` through `screenshot-08.png`
- **Demo Video**: `./phase1-evidence/demo-video.mp4`

## What to Show on Device

### Screenshot Sequence
1. **App Launch** → DEPROOF loading screen
2. **Header** → "AIOZ Observation" title with schema info
3. **Metadata Section** → Timestamp, Assurance (LOCAL_OBSERVATION), Endpoint
4. **Metrics** → Storage Objects, Storage Size, Upstream Speed
5. **Scroll Down** → Reward Asset (AIOZ), Reward Status
6. **SKR Tracking** → SKR Payment Status (separate from AIOZ rewards)
7. **Audit Trail** → Source Digest with SHA256
8. **Final View** → Complete observation data + disclaimer notice

## Submission Checklist
- [ ] All 8 screenshots are clear and readable
- [ ] Demo video shows complete observation flow
- [ ] Storage metrics visible (objects, size, speed)
- [ ] Assurance level displays as "LOCAL_OBSERVATION"
- [ ] Source digest shows audit trail
- [ ] Reward separation visible (AIOZ vs SKR)
- [ ] Screenshots saved to `phase1-evidence/screenshots/`
- [ ] Demo video saved to `phase1-evidence/demo-video.mp4`

## Submission Targets
1. **CLOCK IN Hackathon** (deadline: Oct 8, 23:59 UTC)
2. **Solana dApp Store**

## Troubleshooting

### Device Not Detected
```bash
adb devices  # Check if device shows
adb usb     # Reset USB connection
```

### APK Installation Fails
```bash
adb uninstall com.deproof  # Remove old version
./gradlew clean assembleRelease -q  # Rebuild
```

### Screenshots Not Capturing
```bash
adb shell cmd screencap -p /sdcard/test.png  # Test manually
adb pull /sdcard/test.png ./  # Test pull
```

## Notes
- Test runs take ~5-10 minutes
- Ensure stable USB connection during recording
- Keep app in foreground during video recording
- Video auto-stops at 90 seconds

## Status
- ✅ Phase 2B: Consumer integration complete
- ⏳ Phase 1: Device testing (deadline: Oct 8)
- 📋 Phase 2B: Network integration pending
- 🔮 Phase 3A: Solana integration pending
