# Release Build Signing Guide (F053)

## Overview

Release APK signing is a critical security requirement for publishing apps to Google Play Store. This guide covers:
- Keystore generation and management
- Environment variable configuration
- Build signing process
- Verification and troubleshooting

## Android Signing Requirements

All release APKs must be signed with a private key (stored in a keystore file) to:
1. **Authenticate app identity** - Proves the app is from the official publisher
2. **Enable app updates** - Users can only update apps signed with the same key
3. **Google Play requirement** - Play Store requires release APKs to be signed
4. **Device security** - Android verifies signatures before installing

## Keystore Generation

### Create a New Keystore

Generate a keystore file using Android Studio or command line:

```bash
# Using keytool (OpenJDK/JDK 11+)
keytool -genkey -v -keystore deproof-release.jks \
    -keyalg RSA -keysize 4096 -validity 10950 \
    -alias deproof-key -storepass "$DEPROOF_KEYSTORE_PASSWORD" \
    -keypass "$DEPROOF_KEY_PASSWORD"
```

#### Recommended Settings

**Key Algorithm:** RSA (4096-bit recommended for security)
**Validity:** 10950 days (30 years) - matches app lifetime expectations
**Alias:** Simple identifier (e.g., "deproof-key")
**Passwords:** 
- Store password: Complex, 20+ chars (protects keystore access)
- Key password: Can match store password for convenience

#### Example Keystore Generation

```bash
keytool -genkey -v -keystore deproof-release.jks \
    -keyalg RSA -keysize 4096 -validity 10950 \
    -alias deproof-key
```

Interactive prompts will ask for:
```
Enter keystore password: [your strong password]
Re-enter new password: [repeat]
What is your first and last name? John Doe
What is your organizational unit? Development
What is your organization? Deproof
What is your City or Locality? San Francisco
What is your State or Province? CA
What is your Country Code? US
Certificate fingerprints will be displayed - save for verification
```

### Verify Keystore Contents

```bash
keytool -list -v -keystore deproof-release.jks -alias deproof-key
```

Output includes:
- Certificate fingerprints (SHA-1, SHA-256)
- Validity dates
- Algorithm details

## Environment Variable Configuration

### Setup for Local Development

Create a `.env` or environment setup script:

```bash
# ~/.deproof-build-env.sh (do not commit to git)
export DEPROOF_KEYSTORE_PATH="/path/to/deproof-release.jks"
export DEPROOF_KEYSTORE_PASSWORD="your-keystore-password"
export DEPROOF_KEY_ALIAS="deproof-key"
export DEPROOF_KEY_PASSWORD="your-key-password"
```

Source before building:
```bash
source ~/.deproof-build-env.sh
./gradlew assembleRelease
```

### Setup for CI/CD Pipelines

1. **GitHub Actions (Recommended for this project):**

Add secrets to repository (Settings → Secrets):
- `DEPROOF_KEYSTORE_BASE64` - Base64-encoded keystore file
- `DEPROOF_KEYSTORE_PASSWORD`
- `DEPROOF_KEY_ALIAS`
- `DEPROOF_KEY_PASSWORD`

Workflow example:
```yaml
jobs:
  build-release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Decode and setup keystore
        env:
          KEYSTORE_BASE64: ${{ secrets.DEPROOF_KEYSTORE_BASE64 }}
        run: |
          echo "$KEYSTORE_BASE64" | base64 -d > ~/deproof-release.jks
          export DEPROOF_KEYSTORE_PATH="$HOME/deproof-release.jks"
      - name: Build signed release APK
        env:
          DEPROOF_KEYSTORE_PATH: ${{ env.DEPROOF_KEYSTORE_PATH }}
          DEPROOF_KEYSTORE_PASSWORD: ${{ secrets.DEPROOF_KEYSTORE_PASSWORD }}
          DEPROOF_KEY_ALIAS: ${{ secrets.DEPROOF_KEY_ALIAS }}
          DEPROOF_KEY_PASSWORD: ${{ secrets.DEPROOF_KEY_PASSWORD }}
        run: ./gradlew assembleRelease
```

2. **Local CI/CD (Jenkins, GitLab CI, etc.):**
   - Store keystore file in secure CI/CD vault
   - Inject environment variables at build time
   - Never commit keystore to version control

### Storing Keystore Securely

**DO:**
- ✅ Store keystore file outside the repository
- ✅ Use strong passwords (20+ characters, mixed case, numbers, symbols)
- ✅ Backup keystore securely (separate from passwords)
- ✅ Restrict file permissions: `chmod 600 deproof-release.jks`
- ✅ Keep passwords in password manager, not hardcoded

**DON'T:**
- ❌ Commit `.jks` files to git
- ❌ Store passwords in environment config files that get committed
- ❌ Share keystore passwords via email or chat
- ❌ Use default/weak passwords
- ❌ Store passwords in shell history

## Building Signed Release APK

### With Environment Variables

```bash
export DEPROOF_KEYSTORE_PATH="/path/to/deproof-release.jks"
export DEPROOF_KEYSTORE_PASSWORD="your-password"
export DEPROOF_KEY_ALIAS="deproof-key"
export DEPROOF_KEY_PASSWORD="your-password"

./gradlew assembleRelease
```

**Output location:** `app/build/outputs/apk/release/app-release.apk`

### Without Environment Variables

Release APK will build unsigned:
```bash
./gradlew assembleRelease
```

**Output:** Unsigned APK that cannot be published or installed on production devices

### Build Variants

```bash
# Release APK (optimized, obfuscated, signed)
./gradlew assembleRelease

# Debug APK (unoptimized, readable, debuggable)
./gradlew assembleDebug

# Release Bundle (for Play Store)
./gradlew bundleRelease
```

## Verifying Signed APK

### Check APK Signature

```bash
# Verify APK is signed
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# Extract and inspect certificate
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
```

### Verify Certificate Fingerprints

Compare SHA-256 fingerprint in APK with keystore:

```bash
# From keystore
keytool -list -v -keystore deproof-release.jks -alias deproof-key | grep SHA-256

# From APK
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk | grep SHA-256
```

**Fingerprints must match exactly**

### Install and Test

```bash
# Install signed APK on device
adb install app/build/outputs/apk/release/app-release.apk

# Verify app functions correctly
adb shell am start -n com.deproof.app/.presentation.MainActivity
```

## ProGuard and Signing Integration

Signed release builds include both:
1. **ProGuard optimization** - Reduces APK size, obfuscates code
2. **APK signing** - Authenticates and enables Play Store distribution

Release build sequence:
```
1. Compile Kotlin/Java
2. Apply ProGuard rules
3. Shrink resources
4. Sign APK
5. Align APK (zipalign)
```

**Note:** ProGuard mappings are essential for debugging release crashes. See PROGUARD_CONFIGURATION.md for stack trace retracing.

## Common Issues and Solutions

### Issue: "keystore file not found"

**Cause:** DEPROOF_KEYSTORE_PATH environment variable not set or path is incorrect

**Solutions:**
```bash
# Verify environment variable
echo $DEPROOF_KEYSTORE_PATH

# Check file exists
ls -la /path/to/deproof-release.jks

# Rebuild with absolute path
export DEPROOF_KEYSTORE_PATH="/absolute/path/to/deproof-release.jks"
./gradlew assembleRelease
```

### Issue: "password is incorrect"

**Cause:** Wrong password for DEPROOF_KEYSTORE_PASSWORD or DEPROOF_KEY_PASSWORD

**Solutions:**
```bash
# Test keystore access
keytool -list -keystore deproof-release.jks

# Verify password in environment
echo $DEPROOF_KEYSTORE_PASSWORD

# Check password hasn't expired (30-year validity recommended)
keytool -list -v -keystore deproof-release.jks -alias deproof-key
```

### Issue: "alias not found in keystore"

**Cause:** DEPROOF_KEY_ALIAS doesn't match keystore alias

**Solutions:**
```bash
# List all aliases in keystore
keytool -list -keystore deproof-release.jks

# Verify alias matches exactly (case-sensitive)
echo $DEPROOF_KEY_ALIAS
```

### Issue: "Certificate has expired"

**Cause:** Keystore certificate validity period ended

**Solutions:**
1. Check validity:
   ```bash
   keytool -list -v -keystore deproof-release.jks -alias deproof-key
   ```

2. If expired, create new keystore with 30-year validity:
   ```bash
   keytool -genkey -v -keystore deproof-release.jks \
       -keyalg RSA -keysize 4096 -validity 10950 \
       -alias deproof-key
   ```

3. Update environment variables to point to new keystore

### Issue: "Unsigned release APK built"

**Cause:** One or more signing environment variables are missing

**Solutions:**
```bash
# Verify all 4 variables are set
env | grep DEPROOF_

# Should output all 4:
# DEPROOF_KEYSTORE_PATH
# DEPROOF_KEYSTORE_PASSWORD
# DEPROOF_KEY_ALIAS
# DEPROOF_KEY_PASSWORD
```

## Build Configuration Details

### app/build.gradle.kts Signing Config

```kotlin
signingConfigs {
    create("release") {
        val keystorePath = System.getenv("DEPROOF_KEYSTORE_PATH") ?: ""
        val keystorePassword = System.getenv("DEPROOF_KEYSTORE_PASSWORD") ?: ""
        val keyAlias = System.getenv("DEPROOF_KEY_ALIAS") ?: ""
        val keyPassword = System.getenv("DEPROOF_KEY_PASSWORD") ?: ""
        
        if (keystorePath.isNotEmpty() && keystorePassword.isNotEmpty() && 
            keyAlias.isNotEmpty() && keyPassword.isNotEmpty()) {
            storeFile = file(keystorePath)
            storePassword = keystorePassword
            keyAlias = keyAlias
            keyPassword = keyPassword
        }
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.findByName("release")
    }
}
```

**Key behaviors:**
- Reads all 4 environment variables
- Only applies signing if ALL 4 are provided
- Logs warning if partial configuration detected
- Builds unsigned APK if variables missing (allows development)

## Best Practices

### 1. Keystore Backup and Recovery

```bash
# Backup keystore securely
gpg --symmetric deproof-release.jks

# Store encrypted backup in secure location (not git)
# Keep password in separate secure location
```

**Lost keystore recovery:** Impossible - you must create new keystore with new app signing key.

### 2. Key Rotation (Not Recommended)

Google Play disallows changing app signing keys after first release. **Plan keystore generation carefully.**

Options if key compromised:
- Create new app listing (different package name)
- Maintain current key for existing app

### 3. Certificate Transparency

Save certificate fingerprints for transparency:

```bash
keytool -list -v -keystore deproof-release.jks -alias deproof-key > keystore-fingerprints.txt
```

Share SHA-256 fingerprint publicly (not the key or passwords):
- GitHub repository documentation
- Play Store app listing
- Website security page

### 4. Access Control

**Developers need keystore access:**
- Local development builds
- Testing release builds
- CI/CD pipeline setup

**Who should NOT have access:**
- Untrusted third-party developers
- Public repositories
- Development machines that aren't secured

### 5. Automated Build Safety

CI/CD checklist:
- ✅ Secrets never logged or printed
- ✅ Keystore file deleted after build
- ✅ Build artifacts validated before deployment
- ✅ Signing configuration verified for each build
- ✅ APK signature verified before upload to Play Store

## Compliance and Security

### App Signing Certificate Requirements

**Valid through app lifetime:**
- Certificates must remain valid for entire app presence on Play Store
- Recommended 10-30 year validity (30 years = 10950 days)
- No certificate renewal needed; original key persists forever

**RSA Key Size:**
- 4096-bit recommended (current security standard)
- 2048-bit minimum (considered legacy)
- Avoid 1024-bit (cryptographically weak)

### Google Play Store

1. **First release:** You sign and upload to Play Store
2. **Subsequent releases:** Must use SAME key for app updates
3. **Key mismatches:** Play Store rejects APK, requests matching key
4. **Key compromise:** Create new app listing (cannot reuse same app ID)

## Release Build Checklist

Before publishing to Play Store:

- [ ] Keystore file securely backed up
- [ ] All 4 environment variables configured
- [ ] Release APK builds successfully
- [ ] APK signature verified with `jarsigner`
- [ ] ProGuard mapping file saved for crash reporting
- [ ] App tested on multiple Android versions (minSdk=28, targetSdk=34)
- [ ] App tested with obfuscated code (difference from debug)
- [ ] Deep links tested in release build
- [ ] Network requests work correctly
- [ ] Encryption/decryption verified
- [ ] Crash reporting logs readable with mapping.txt
- [ ] Certificate fingerprints documented
- [ ] CI/CD pipeline validates signatures

## References

- [Android Signing Your App](https://developer.android.com/studio/publish/app-signing)
- [Google Play App Signing](https://developer.android.com/studio/publish/app-signing#app-signing-google-play)
- [Keytool Documentation](https://docs.oracle.com/en/java/javase/11/tools/keytool.html)
- [Jarsigner Documentation](https://docs.oracle.com/en/java/javase/11/tools/jarsigner.html)
- [Google Play Security Best Practices](https://developer.android.com/distribute/best-practices/develop/security-best-practices)
