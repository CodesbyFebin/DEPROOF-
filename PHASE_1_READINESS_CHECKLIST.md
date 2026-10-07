# Phase 1 Readiness Checklist
## October 7, 2026, 11:05 UTC

---

## ✅ Pre-Build Requirements

- [x] MainActivity.kt created and integrated
- [x] DeviceStatsScreen properly imported and displayed
- [x] AndroidManifest.xml updated with correct activity reference
- [x] All Phase 2B code compiled successfully
- [x] No compilation errors or warnings
- [x] Test suite passes (100+ test cases)

---

## ⏳ Build Status

### Debug APK Build
- **Command**: `./gradlew assembleDebug -q`
- **Status**: In Progress (started 10:58 UTC)
- **Expected Time**: ~5-10 minutes
- **Output Path**: `app/build/outputs/apk/debug/app-debug.apk`
- **Expected Size**: ~25MB
- **Advantage**: No signing required, suitable for testing

### Release APK Build
- **Command**: `./gradlew assembleRelease -q`
- **Status**: Failed - Keystore password required
- **Issue**: Signing credentials not available in environment
- **Resolution**: Use debug APK for device testing (preferred)

---

## 📋 Test Infrastructure

### Script & Documentation
- [x] TEST_INSTRUCTIONS.sh created and executable
  - Auto-detects debug/release APK
  - Handles device connectivity checks
  - Performs APK installation
  - Captures 8 screenshots
  - Records 90-second demo video
  
- [x] PHASE_1_TESTING_GUIDE.md created
  - Setup instructions for device
  - Submission requirements
  - Troubleshooting guide
  - Deadline and timeline
  
- [x] DEVELOPMENT_STATUS.md created
  - Overall project status
  - Phase timeline
  - Component status
  - Quality metrics

### Evidence Directory
- [x] `phase1-evidence/` directory created
- [x] `phase1-evidence/screenshots/` subdirectory created
- [x] Ready to store 8 screenshot files
- [x] Ready to store 90-second demo video

---

## 🎯 Test Execution Requirements

### Hardware Requirements
- Physical Android device (API level 24+)
- USB debugging enabled
- USB cable connected to development machine
- ≥500MB free storage on device
- Screen lock disabled (or pin/pattern available)

### Software Requirements
- adb (Android Debug Bridge) installed
- Bash shell for test script
- FFmpeg or Android screenrecord capability (built-in)

### Device Setup Steps
1. Connect Android device via USB
2. Enable Developer Options (tap Build Number 7 times)
3. Enable USB Debugging in Developer Options
4. Allow USB debugging permission when prompted
5. Accept any RSA key fingerprint dialogs
6. Verify with `adb devices` (device should show as "device", not "unauthorized")

---

## 📸 Expected Deliverables

### Screenshots (8 Total)
1. **App Launch Screen**: Initial DeviceStatsScreen view
2. **Provider Header**: "AIOZ Observation" title and schema
3. **Metadata Section**: Timestamp, assurance level, verification status
4. **Reward Tracking**: AIOZ reward status, SKR payment status
5. **Metrics Display**: Storage objects, storage size, upstream speed
6. **Audit Trail**: Source digest, SHA256 hash preview
7. **Scrolled View**: Full content scroll demonstration
8. **Final Evidence**: Complete observation with disclaimer

### Demo Video (90 Seconds)
- Screen recording of complete observation flow
- Should show:
  - ✓ App launch
  - ✓ Full DeviceStatsScreen visible
  - ✓ Scroll through all sections
  - ✓ Visibility of storage metrics
  - ✓ Visibility of assurance level
  - ✓ Visibility of audit trail
  - ✓ Reward separation notice

---

## 🔄 Test Execution Workflow

### Step 1: Prepare Device
```bash
# On development machine, verify device connectivity
adb devices

# Expected output:
# List of attached devices
# device-serial-number    device
```

### Step 2: Start Test Script
```bash
cd /home/user/deproof-
./TEST_INSTRUCTIONS.sh

# Script will:
# 1. Verify device connectivity ✓
# 2. Verify APK availability ✓
# 3. Install APK on device ✓
# 4. Launch app ✓
# 5. Capture 8 screenshots ✓
# 6. Record 90-second video ✓
```

### Step 3: Device Actions During Test
During test script execution, ensure:
- Device screen is active and unlocked
- App is visible and not covered
- No interrupting notifications
- Allow any permission prompts
- Device stays connected via USB

### Step 4: Review Evidence
```bash
# After test completes
ls -l phase1-evidence/screenshots/
ls -lh phase1-evidence/demo-video.mp4

# Verify:
# - 8 PNG files present
# - 1 MP4 video file present
# - All files have reasonable sizes
```

### Step 5: Quality Check
- [ ] Screenshot 1: Clear app launch
- [ ] Screenshot 2: Provider header visible
- [ ] Screenshot 3: Timestamp and metadata visible
- [ ] Screenshot 4: Reward tracking visible
- [ ] Screenshot 5: Storage metrics visible
- [ ] Screenshot 6: Audit trail visible
- [ ] Screenshot 7: Full content scroll visible
- [ ] Screenshot 8: Complete observation visible
- [ ] Video: 90 seconds of screen recording
- [ ] Video: Shows complete observation flow

---

## ✨ Quality Expectations

### Visual Quality
- Screenshots should be at least 1080x1920 resolution
- All text should be legible
- Color scheme should show:
  - Obsidian background (#0A0E27)
  - Mint headers (#00FF9F)
  - Cyan accents (#00D9FF)
  - Gray labels (#7A8199)

### Data Visibility
- Storage Objects: ≥1 object visible
- Storage Size: Human-readable format (KB/MB/GB)
- Upstream Speed: Numeric value with units
- Assurance Level: "LOCAL_OBSERVATION"
- Source Digest: SHA256 (first 16 chars + "...")
- Rewards: AIOZ and SKR separate

### Video Quality
- Frame rate: ≥30 FPS
- Duration: ≥80 seconds, ≤90 seconds
- Audio: Optional (may be silent)
- Resolution: ≥1080p ideal

---

## 📤 Submission Targets

### CLOCK IN Hackathon
- **Deadline**: October 8, 2026, 23:59 UTC
- **Time Remaining**: ~37 hours
- **Submission URL**: (TBD - check hackathon platform)
- **Required Files**: Screenshots + demo video + project description

### Solana dApp Store
- **Deadline**: No specific deadline mentioned
- **Submission URL**: (TBD - check Solana dApp Store docs)
- **Required Files**: Screenshots + demo video + APK + project metadata

---

## 🚨 Critical Timeline

| Time | Task | Status |
|------|------|--------|
| 10:58 UTC | Debug APK build started | ⏳ In Progress |
| 11:10 UTC (est) | Debug APK build complete | ⏳ Waiting |
| 11:15 UTC (est) | Device test execution | 📋 Ready |
| 11:30 UTC (est) | Evidence capture complete | 📋 Ready |
| 11:45 UTC (est) | Quality review complete | 📋 Ready |
| 12:00 UTC+ | Submission ready | 📋 Ready |
| **Oct 8 23:59 UTC** | **DEADLINE** | 🚨 Critical |

**Time Remaining**: ~37 hours (ample time if APK builds successfully)

---

## ⚠️ Risk Mitigation

### If Debug APK Build Fails
- Check: Kotlin compilation (should have succeeded in Phase 2B)
- Check: MainActivity.kt syntax errors
- Check: AndroidManifest.xml syntax
- Action: Fix compilation error and retry

### If Device Not Detected
- Check: USB cable connection
- Check: Device USB debugging enabled
- Action: `adb kill-server && adb start-server`
- Action: Reconnect USB cable

### If APK Installation Fails
- Action: `adb uninstall com.deproof`
- Action: Retry installation
- Check: Device storage (≥500MB free)

### If Screenshots Don't Capture
- Action: Test manually: `adb shell screencap -p /sdcard/test.png`
- Action: Test pull: `adb pull /sdcard/test.png`
- Check: Device storage permissions

### If Video Recording Fails
- Action: Test manually: `adb shell screenrecord /sdcard/test.mp4`
- Check: Device storage space
- Alternative: Use scrcpy + ffmpeg for recording

---

## 📊 Success Criteria

### Mandatory Criteria
- ✅ App launches without crashes
- ✅ DeviceStatsScreen displays correctly
- ✅ All metrics visible (storage objects, size, speed)
- ✅ Assurance level displays ("LOCAL_OBSERVATION")
- ✅ Source digest visible
- ✅ Rewards separated (AIOZ vs SKR)
- ✅ 8 screenshots captured
- ✅ 90-second demo video recorded
- ✅ Evidence files present and valid

### Submission Criteria
- ✅ Screenshots clear and legible
- ✅ Video shows complete flow
- ✅ Project description prepared
- ✅ Files uploaded to submission platform
- ✅ Submission receipt confirmed

---

## 📝 Notes

- Phase 1 is purely evidence capture for hackathon submission
- No network connectivity required for Phase 1
- All observations are local/mock data
- Real Flux monitoring and Solana integration come in later phases
- Debug APK is preferred for testing (no signing issues)
- Test execution takes ~5-10 minutes with device connected

---

## 🎉 Phase 1 Completion Criteria

Phase 1 is complete when:
1. ✅ Evidence captured (8 screenshots + 1 video)
2. ✅ Evidence quality verified
3. ✅ Files submitted to CLOCK IN hackathon
4. ✅ Submission receipt confirmed
5. ✅ Files optionally submitted to Solana dApp Store

**Status**: Ready to execute - awaiting APK build completion
