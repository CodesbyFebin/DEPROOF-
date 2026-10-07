#!/bin/bash
#
# PHASE 1: Local Device Testing - TEST_INSTRUCTIONS.sh
# =====================================================
# Executes observation parsing & proof generation on Android device
# Captures evidence for hackathon & Solana dApp Store submission
#
# Requirements:
# - Android device with USB debugging enabled
# - adb (Android Debug Bridge) available
# - app-release.apk transferred to device or available locally
#
# Time: ~5-10 minutes
# Output: Screenshots (8) + Demo video (90s) for submission
#

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
APK_PATH="${1:-}"
PACKAGE_NAME="com.deproof"

# Auto-detect APK if not provided
if [ -z "$APK_PATH" ]; then
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    elif [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
        APK_PATH="app/build/outputs/apk/release/app-release.apk"
    else
        APK_PATH="app-release.apk"
    fi
fi
SCREENSHOTS_DIR="./phase1-evidence/screenshots"
VIDEO_OUTPUT="./phase1-evidence/demo-video.mp4"

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}DEPROOF Phase 1: Local Device Testing${NC}"
echo -e "${BLUE}================================================${NC}"

# Step 1: Verify device connectivity
echo -e "\n${YELLOW}[Step 1/6] Checking device connectivity...${NC}"
if ! adb devices | grep -q "device$"; then
    echo -e "${RED}❌ No Android device found. Enable USB debugging and reconnect.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Device detected${NC}"

# Step 2: Verify APK availability
echo -e "\n${YELLOW}[Step 2/6] Verifying APK...${NC}"
if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}❌ APK not found at: $APK_PATH${NC}"
    echo "Available APK: app/build/outputs/apk/release/app-release.apk"
    exit 1
fi
APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo -e "${GREEN}✓ APK ready ($APK_SIZE)${NC}"

# Step 3: Install APK
echo -e "\n${YELLOW}[Step 3/6] Installing DEPROOF APK...${NC}"
adb install -r "$APK_PATH" > /dev/null 2>&1 || {
    echo -e "${RED}❌ Installation failed${NC}"
    exit 1
}
echo -e "${GREEN}✓ APK installed${NC}"

# Step 4: Create screenshot directory
echo -e "\n${YELLOW}[Step 4/6] Setting up capture directories...${NC}"
mkdir -p "$SCREENSHOTS_DIR"
echo -e "${GREEN}✓ Directories created${NC}"

# Step 5: Launch app and capture observations
echo -e "\n${YELLOW}[Step 5/6] Launching DEPROOF & parsing observations...${NC}"
echo ""
echo "Follow these steps on your device:"
echo "  1. App launches → see 'Device Observations'"
echo "  2. Observe: Storage Objects (📦), Storage Size (💾), Upstream Speed (📡)"
echo "  3. Scroll: View Assurance Level (LOCAL_OBSERVATION)"
echo "  4. Scroll: View Source Digest (SHA256 - first 32 chars)"
echo "  5. Scroll: View Reward Assets (AIOZ vs SKR)"
echo ""

adb shell am start -n "$PACKAGE_NAME/.MainActivity" 2>&1 | grep -i "warning\|error" || true

sleep 2

# Capture screenshots (sequence)
echo -e "${YELLOW}Capturing screenshots...${NC}"

for i in {1..8}; do
    SCREENSHOT_FILE="$SCREENSHOTS_DIR/screenshot-$(printf "%02d" $i).png"
    adb shell screencap -p "/sdcard/screenshot-$i.png" > /dev/null 2>&1
    adb pull "/sdcard/screenshot-$i.png" "$SCREENSHOT_FILE" > /dev/null 2>&1
    echo -e "${GREEN}  [$i/8] $SCREENSHOT_FILE${NC}"
    sleep 1
done

# Step 6: Record demo video (90 seconds)
echo -e "\n${YELLOW}[Step 6/6] Recording 90-second demo video...${NC}"
echo "Ensure the DEPROOF app is visible with:"
echo "  - DeviceStatsScreen showing all sections"
echo "  - Scroll through observations"
echo "  - Show source digest audit trail"
echo ""

# Note: screenrecord via adb requires manual timing
# For automated recording, consider using scrcpy with ffmpeg
adb shell screenrecord --time-limit 90 "/sdcard/demo.mp4" > /dev/null 2>&1 &
RECORD_PID=$!

echo -e "${YELLOW}Recording... (90 seconds)${NC}"
sleep 90
wait $RECORD_PID 2>/dev/null || true

# Pull video
adb pull "/sdcard/demo.mp4" "$VIDEO_OUTPUT" > /dev/null 2>&1
echo -e "${GREEN}✓ Video captured: $VIDEO_OUTPUT${NC}"

# Cleanup
adb shell rm -f "/sdcard/screenshot-*" "/sdcard/demo.mp4" 2>/dev/null || true

# Summary
echo -e "\n${BLUE}================================================${NC}"
echo -e "${GREEN}✅ Phase 1 Evidence Captured${NC}"
echo -e "${BLUE}================================================${NC}"

echo -e "\n${YELLOW}Deliverables:${NC}"
echo -e "  📸 Screenshots: $SCREENSHOTS_DIR/"
echo -e "     - screenshot-01.png through screenshot-08.png"
echo -e "  🎥 Demo Video: $VIDEO_OUTPUT"
echo -e "     - 90 seconds showing observation flow"

echo -e "\n${YELLOW}Next Steps:${NC}"
echo -e "  1. Review all 8 screenshots for clarity"
echo -e "  2. Verify demo video shows:"
echo -e "     ✓ Storage Objects, Size, Upstream Speed"
echo -e "     ✓ Assurance Level (LOCAL_OBSERVATION)"
echo -e "     ✓ Source Digest with audit trail"
echo -e "     ✓ Reward separation (AIOZ vs SKR)"
echo -e "  3. Submit to:"
echo -e "     • CLOCK IN hackathon (deadline: Oct 8, 23:59 UTC)"
echo -e "     • Solana dApp Store"

echo -e "\n${BLUE}================================================${NC}"
echo -e "${GREEN}Phase 1 Complete. Ready for submission.${NC}"
echo -e "${BLUE}================================================${NC}"
