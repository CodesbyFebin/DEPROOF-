#!/usr/bin/env bash
# scripts/sign-apk.sh — Build and sign the Deproof release APK.
#
# Usage:
#   source .signing.env   # or export vars manually
#   bash scripts/sign-apk.sh
#
# Required env vars (never commit these; .signing.env is gitignored):
#   DEPROOF_KEYSTORE_PATH      absolute path to the .jks keystore
#   DEPROOF_KEY_ALIAS          key alias inside the keystore
#   DEPROOF_KEYSTORE_PASSWORD  keystore password
#   DEPROOF_KEY_PASSWORD       key password
#
# Output:
#   app/build/outputs/apk/release/app-release.apk  (signed)
#   sha256 checksum printed to stdout

set -euo pipefail

REQUIRED_VARS=(
  DEPROOF_KEYSTORE_PATH
  DEPROOF_KEY_ALIAS
  DEPROOF_KEYSTORE_PASSWORD
  DEPROOF_KEY_PASSWORD
)

for v in "${REQUIRED_VARS[@]}"; do
  if [[ -z "${!v:-}" ]]; then
    echo "ERROR: $v is not set. Export it or run: source .signing.env" >&2
    exit 1
  fi
done

if [[ ! -f "$DEPROOF_KEYSTORE_PATH" ]]; then
  echo "ERROR: keystore not found at $DEPROOF_KEYSTORE_PATH" >&2
  exit 1
fi

echo "Building release APK..."
./gradlew :app:assembleRelease

APK="app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$APK" ]]; then
  echo "ERROR: APK not found at $APK after build" >&2
  exit 1
fi

echo ""
echo "=== APK Checksum ==="
sha256sum "$APK"

echo ""
echo "=== Signing Certificate ==="
keytool -printcert -jarfile "$APK" 2>/dev/null | grep -E "SHA256:|Owner:|Issuer:|Valid from:"

echo ""
echo "Done. APK: $APK"
