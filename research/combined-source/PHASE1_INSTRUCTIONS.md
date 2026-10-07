# Phase 1: Local Device Testing
**Deadline: October 8, 2026 23:59 UTC**  
**Status: Ready to Execute**  
**Time Required: ~30-45 minutes**

---

## Overview

Phase 1 validates the DEPROOF application on a real Android device and captures evidence for submission to the CLOCK IN hackathon and Solana dApp Store.

## Prerequisites

### Hardware
- Android device (minimum Android 12, API 31+)
- USB cable for ADB connection
- Minimum 500MB free storage

### Software (on your machine)
- Android Debug Bridge (adb)
  ```bash
  # macOS
  brew install android-platform-tools
  
  # Linux
  sudo apt-get install android-tools-adb
  
  # Windows - download from https://developer.android.com/tools/releases/platform-tools
  ```

### Device Setup
1. Enable USB Debugging:
   - Settings → About Phone → Build Number (tap 7x)
   - Settings → Developer Options → USB Debugging (enable)
   - Connect to computer via USB

2. Verify connection:
   ```bash
   adb devices
   # Should show: <device_id>    device
   ```

---

## Step-by-Step Execution

### Step 1: Prepare APK

The APK has been built during Phase 2A compilation. Locate it:

```bash
# From project root
APK_PATH="deproof-/app/build/outputs/apk/release/app-release.apk"

# Verify it exists
ls -lh "$APK_PATH"
```

If APK is missing, rebuild:
```bash
cd deproof-
./gradlew assembleRelease
```

### Step 2: Run Test Script

Execute the automated test script:

```bash
# Make it executable
chmod +x deproof-/TEST_INSTRUCTIONS.sh

# Run it
cd deproof-
./TEST_INSTRUCTIONS.sh
```

**What the script does:**
1. Verifies device connectivity
2. Installs app-release.apk
3. Launches DEPROOF application
4. Captures 8 sequential screenshots
5. Records 90-second demo video
6. Outputs evidence to `./phase1-evidence/`

### Step 3: Manual Verification (if script fails)

If automated testing fails, follow these manual steps:

```bash
# Install APK
adb install -r deproof-/app/build/outputs/apk/release/app-release.apk

# Launch app
adb shell am start -n com.deproof/.MainActivity

# Capture screenshots manually
adb shell screencap -p /sdcard/screenshot-01.png
adb pull /sdcard/screenshot-01.png ./phase1-evidence/

# Record video (90 seconds)
adb shell screenrecord --time-limit 90 /sdcard/demo.mp4
adb pull /sdcard/demo.mp4 ./phase1-evidence/demo-video.mp4
```

---

## Evidence Collection

### 8 Required Screenshots

Capture these screens in sequence:

1. **App Launch** - Initial screen with "Device Observations" header
2. **Storage Objects** - 📦 icon showing item count
3. **Storage Size** - 💾 icon showing bytes with human-readable format
4. **Upstream Speed** - 📡 icon with unverified speed metric
5. **Assurance Level** - Colored card with "LOCAL_OBSERVATION" status
6. **Source Digest** - Colored card showing SHA256 (first 32 chars + "...")
7. **Audit Trail** - Text "Proves exact CLI stats parsed"
8. **Reward Assets** - AIOZ Rewards and SKR Payment values

### 90-Second Demo Video

Record continuous walkthrough showing:
- App launch and initial render
- Scroll through "Device Observations" section
- Display each stat item (storage, speed)
- Show Assurance Card with unverified status
- Display Source Digest with audit trail explanation
- Show Reward Separation card (AIOZ vs SKR)
- Return to top of screen

**Target: Smooth, legible, all text readable**

---

## Output Structure

After successful execution:

```
deproof-/phase1-evidence/
├── screenshots/
│   ├── screenshot-01.png  (App launch)
│   ├── screenshot-02.png  (Storage Objects)
│   ├── screenshot-03.png  (Storage Size)
│   ├── screenshot-04.png  (Upstream Speed)
│   ├── screenshot-05.png  (Assurance Level)
│   ├── screenshot-06.png  (Source Digest)
│   ├── screenshot-07.png  (Audit Trail)
│   └── screenshot-08.png  (Reward Assets)
└── demo-video.mp4  (90 seconds)
```

---

## Quality Checklist

Before submission, verify:

- [ ] All 8 screenshots captured
- [ ] Screenshots are clear and readable
- [ ] Demo video is 90 seconds
- [ ] Demo video shows smooth scrolling
- [ ] Text is legible in all screenshots
- [ ] All UI elements visible (icons, colors, metrics)
- [ ] Source digest visible (SHA256)
- [ ] Assurance level shows "LOCAL_OBSERVATION"
- [ ] Reward separation cards visible
- [ ] No errors in app logs

Verify no errors:
```bash
adb logcat | grep -i "error\|exception"
```

---

## Submission Targets

### 1. CLOCK IN Hackathon

**URL:** https://clock-in.hackathon.com  
**Deadline:** October 8, 2026 23:59 UTC  
**Submission:**
- Project: DEPROOF (Solana × SKR DePIN)
- Phase: 1 - Local Device Testing
- Deliverables:
  - 8 screenshots (phase1-evidence/screenshots/)
  - 90-second demo video (phase1-evidence/demo-video.mp4)

### 2. Solana dApp Store

**URL:** https://store.solana.dapp  
**Requirements:**
- APK file (app-release.apk)
- Screenshots (use phase1-evidence/)
- Demo video (phase1-evidence/demo-video.mp4)
- Description:
  ```
  DEPROOF: Solana × SKR DePIN proof generation.
  
  Phase 1 demonstrates:
  - Local AIOZ CLI observation parsing with strict validation
  - SHA256 source digest tracking for audit trail
  - Device telemetry display (storage, speed, assurance)
  - Reward asset separation (AIOZ vs SKR)
  ```

---

## Troubleshooting

### Issue: "No Android device found"
```bash
# Check connection
adb devices

# If device shows "unauthorized", approve on device
# Then retry

# If still failing, restart adb daemon
adb kill-server
adb start-server
adb devices
```

### Issue: "Installation failed"
```bash
# Check existing app
adb shell pm list packages | grep com.deproof

# Uninstall if present
adb uninstall com.deproof

# Retry install
adb install -r app-release.apk
```

### Issue: "Screenshot not found"
```bash
# Check device storage
adb shell ls -lh /sdcard/

# Grant storage permissions
adb shell pm grant com.deproof android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.deproof android.permission.WRITE_EXTERNAL_STORAGE
```

### Issue: "Video recording failed"
- Manual option:
  ```bash
  adb shell screenrecord --time-limit 90 /sdcard/demo.mp4
  adb pull /sdcard/demo.mp4 ./phase1-evidence/
  ```

---

## Timeline

| Phase | Deadline | Status |
|-------|----------|--------|
| Phase 1: Device Testing | Oct 8, 23:59 UTC | **🔴 IN PROGRESS** |
| Phase 2B: W3bstream Research | Oct 15 | Scheduled (auto) |
| Phase 2C: Web Companion (opt) | Oct 22 | Scheduled (auto) |
| Phase 3A: Multi-device + Security | Nov 1 | Scheduled (auto) |
| Phase 3B: Play Store Launch | Nov 16 | Scheduled (auto) |

---

## Success Criteria

✅ Phase 1 Complete when:
1. APK installed and launches on device
2. All 8 screenshots captured in phase1-evidence/
3. 90-second demo video captured
4. Both submitted to CLOCK IN hackathon
5. Solana dApp Store submission initiated

---

## Phases 2B-3B (Autonomous Execution)

After Phase 1 submission, subsequent phases execute automatically:

- **Oct 15 (Phase 2B)**: W3bstream research + prover coordination
- **Oct 22 (Phase 2C)**: Optional web companion (React + create-solana-dapp)
- **Nov 1 (Phase 3A)**: Multi-device aggregation + security hardening
- **Nov 16 (Phase 3B)**: Google Play Store submission

No manual intervention needed. Status updates will be posted to this session.

---

## Notes

- Phase 1 is validation-focused, not production
- "LOCAL_OBSERVATION" assurance level is expected (device-side only)
- Source digest proves exact input bytes (audit trail)
- Reward separation confirms AIOZ and SKR tracked independently
- All code compiles locally (verified Phase 2A)

**Questions?** Check PR #4 status or review DeviceStatsScreen.kt / AIZStatsParser.kt implementation.
