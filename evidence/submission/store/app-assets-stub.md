# Deproof — App Store Assets (Stub)

**Status**: STUB — These assets must be created by the developer before dApp Store submission.

## Required Assets

### App Icon

- **Required format**: PNG, 512×512 px, no transparent background
- **Status**: STUB — The SVG logo is at `assets/depr.svg`. Export to 512×512 PNG.
- **Action required**: Export `assets/depr.svg` to `assets/icon-512.png` using Inkscape, GIMP, or equivalent.

```bash
# Example with Inkscape:
inkscape assets/depr.svg --export-png=assets/icon-512.png --export-width=512 --export-height=512
```

### Screenshots

The Solana dApp Store requires at least 2 screenshots (up to 8).

Recommended screenshots (record on API 28+ device or emulator):

1. **Workspace overview** — Show the multi-project DePIN adapter list
2. **SKR Payment Review** — Show the exact-message payment review screen with full transaction details
3. **Receipts list** — Show the portable contribution receipt list
4. **Wallet connection** — Show the wallet connection screen (physical device preferred)

**Dimensions**: 1080×1920 px (portrait) or 1920×1080 px (landscape)

**Action required**: Take screenshots using `adb shell screencap` or Android emulator screenshot tool.

```bash
# Take screenshot via ADB:
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png evidence/submission/store/screenshot-01.png
```

### Feature Graphic (Optional)

- **Dimensions**: 1024×500 px
- **Status**: Not created — optional for Solana dApp Store

## Asset Checklist

| Asset | Required | Status |
|-------|----------|--------|
| App icon 512×512 PNG | Yes | STUB |
| Screenshot 1 (workspace) | Yes | STUB |
| Screenshot 2 (payment review) | Yes | STUB |
| Screenshot 3 (receipts) | Recommended | STUB |
| Screenshot 4 (wallet) | Recommended | STUB |
| Feature graphic 1024×500 | Optional | Not created |

## Note

These assets cannot be auto-generated without a running emulator session or physical device. The stubs above explain exactly how to produce each asset. No placeholder images are fabricated.
