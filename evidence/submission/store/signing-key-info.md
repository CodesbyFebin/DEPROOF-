# Deproof — Signing Key Information

**IMPORTANT**: This file does not contain any key material, passwords, or secrets.

## Keystore Location

The release signing keystore is stored OUTSIDE the repository:
- **Path**: `/home/codesbyfebin/keys/deproof-release.jks`
- **Alias**: `deproof-key`
- **Not tracked in git** (confirmed: not in `.git` history)

## Current Release APK Status

The release APK built in this session (`app-release-unsigned.apk`) is UNSIGNED because the environment variables `DEPROOF_KEYSTORE_PATH`, `DEPROOF_KEYSTORE_PASSWORD`, `DEPROOF_KEY_ALIAS`, and `DEPROOF_KEY_PASSWORD` were not set.

## To Sign the Release APK

```bash
export DEPROOF_KEYSTORE_PATH=/home/codesbyfebin/keys/deproof-release.jks
export DEPROOF_KEYSTORE_PASSWORD=<your-password>
export DEPROOF_KEY_ALIAS=deproof-key
export DEPROOF_KEY_PASSWORD=<your-key-password>
./gradlew :app:assembleRelease
```

This will produce `app/build/outputs/apk/release/app-release.apk` (signed).

## To Get Certificate Fingerprint

After signing, run:
```bash
keytool -list -v -keystore /home/codesbyfebin/keys/deproof-release.jks -alias deproof-key
```

Or from the signed APK:
```bash
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
```

The SHA-256 certificate fingerprint is required for the Solana dApp Store listing.

## dApp Store Requirement

The Solana dApp Store requires the APK to be signed with a release keystore. The debug keystore (`~/.android/debug.keystore`) must NOT be used for store submissions. Use the keystore at the path above.

## Security Notes

- Never commit the keystore file to the repository
- Never commit keystore passwords to the repository
- Store backup of keystore in a secure location separate from the development machine
- The certificate fingerprint (SHA-256) is safe to publish — it is not a secret
