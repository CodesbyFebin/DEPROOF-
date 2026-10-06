# Build Variants Configuration Guide (F054)

## Overview

Build variants enable building different versions of the app for different environments (staging, production) with different configurations. This guide covers:
- Product flavors (environment selection)
- Build types (debug, release)
- Variant-specific configurations
- Variant selection and building
- Testing different variants

## Build Variant Dimensions

### Product Flavors (Environment Dimension)

Deproof supports two environment flavors:

**Production Flavor**
- Application ID: `com.deproof.app`
- API Endpoint: `https://api.mainnet-beta.solana.com` (production Solana mainnet)
- Environment: `production`
- Debug logging: Disabled
- Feature flags: Disabled
- Crash reporting: Enabled
- Use case: Release builds for Google Play Store

**Staging Flavor**
- Application ID: `com.deproof.app.staging`
- API Endpoint: `https://api.devnet.solana.com` (Solana devnet)
- Environment: `staging`
- Debug logging: Enabled (verbose)
- Feature flags: Enabled (for testing new features)
- Crash reporting: Enabled
- Use case: Testing against devnet before production release

### Build Types

**Debug Build Type**
- Minification: Disabled
- Optimization: None
- Debuggable: Yes
- Signing: Debug keystore (unsigned)
- Use case: Development, debugging, local testing

**Release Build Type**
- Minification: Enabled (ProGuard)
- Resource shrinking: Enabled
- Debuggable: No
- Signing: Release keystore (if configured)
- Use case: Distribution, performance testing, production deployment

## Available Build Variants

Each combination of flavor + buildType creates a variant:

```
Production Flavor + Debug = productionDebug
Production Flavor + Release = productionRelease
Staging Flavor + Debug = stagingDebug
Staging Flavor + Release = stagingRelease
```

### Variant Characteristics

| Variant | App ID | API | Debug | Signing | Use Case |
|---------|--------|-----|-------|---------|----------|
| `productionDebug` | com.deproof.app | mainnet | Yes | Debug | Local dev |
| `productionRelease` | com.deproof.app | mainnet | No | Release | Play Store |
| `stagingDebug` | com.deproof.app.staging | devnet | Yes | Debug | Staging QA |
| `stagingRelease` | com.deproof.app.staging | devnet | No | Release | Release testing |

## Building Variants

### Build All Variants

```bash
./gradlew build
```

Generates APKs for all 4 variants in `app/build/outputs/apk/`

### Build Specific Variant

```bash
# Production release (for Play Store)
./gradlew assembleProductionRelease

# Staging debug (for QA testing)
./gradlew assembleStagingDebug

# All debug builds
./gradlew assembleDebug

# All release builds
./gradlew assembleRelease
```

### Build and Install Specific Variant

```bash
./gradlew installStagingDebug
./gradlew installProductionRelease
```

### Run Tests for Specific Variant

```bash
./gradlew testProductionDebugUnitTest
./gradlew testStagingDebugUnitTest
./gradlew testProductionReleaseUnitTest
```

## Variant-Specific Source Code

Android build system supports variant-specific source code directories:

### Directory Structure

```
app/src/
├── main/                          # Shared by all variants
│   ├── kotlin/
│   │   └── com/deproof/
│   │       ├── DepRoofApplication.kt
│   │       ├── presentation/
│   │       ├── domain/
│   │       └── data/
│   ├── res/
│   └── AndroidManifest.xml
├── production/                    # Production-specific
│   └── kotlin/com/deproof/
│       └── EnvironmentConfig.kt
├── staging/                       # Staging-specific
│   └── kotlin/com/deproof/
│       └── EnvironmentConfig.kt
├── debug/                         # All debug builds
│   └── kotlin/
├── release/                       # All release builds
│   └── kotlin/
└── productionRelease/             # Production release only
    └── kotlin/
```

### EnvironmentConfig Usage

Each variant has its own `EnvironmentConfig.kt`:

**Production:**
```kotlin
object EnvironmentConfig {
    const val ENVIRONMENT = "production"
    const val DEBUG_LOGGING = false
    const val API_TIMEOUT_SECONDS = 30
    const val RETRY_MAX_ATTEMPTS = 3
}
```

**Staging:**
```kotlin
object EnvironmentConfig {
    const val ENVIRONMENT = "staging"
    const val DEBUG_LOGGING = true
    const val API_TIMEOUT_SECONDS = 60
    const val RETRY_MAX_ATTEMPTS = 5
}
```

### Using EnvironmentConfig in Code

```kotlin
import com.deproof.EnvironmentConfig

class MyRepository {
    init {
        val isProduction = EnvironmentConfig.ENVIRONMENT == "production"
        val timeoutSeconds = EnvironmentConfig.API_TIMEOUT_SECONDS
        
        if (EnvironmentConfig.DEBUG_LOGGING) {
            Timber.d("Debug logging enabled: Using ${EnvironmentConfig.ENVIRONMENT} API")
        }
    }
}
```

## Variant-Specific Resources

### Create Variant-Specific Resource Directories

```bash
mkdir -p app/src/staging/res/values
mkdir -p app/src/production/res/values
mkdir -p app/src/stagingDebug/res/values
```

### Example: Staging-Specific Strings

**app/src/staging/res/values/strings.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Deproof [Staging]</string>
    <string name="api_endpoint">devnet</string>
</resources>
```

**app/src/production/res/values/strings.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Deproof</string>
    <string name="api_endpoint">mainnet</string>
</resources>
```

## BuildConfig Fields Per Variant

### Current Configuration

Default (all variants):
```kotlin
buildConfigField("String", "API_ENDPOINT", "\"https://api.mainnet-beta.solana.com\"")
buildConfigField("String", "ENVIRONMENT", "\"production\"")
```

Production flavor overrides:
```kotlin
buildConfigField("String", "API_ENDPOINT", "\"https://api.mainnet-beta.solana.com\"")
buildConfigField("String", "ENVIRONMENT", "\"production\"")
```

Staging flavor overrides:
```kotlin
buildConfigField("String", "API_ENDPOINT", "\"https://api.devnet.solana.com\"")
buildConfigField("String", "ENVIRONMENT", "\"staging\"")
```

### Using BuildConfig in Code

```kotlin
import com.deproof.BuildConfig

class ApiClient {
    private val apiEndpoint = BuildConfig.API_ENDPOINT
    private val environment = BuildConfig.ENVIRONMENT
    
    fun getServerUrl(): String {
        return if (environment == "staging") {
            "$apiEndpoint/staging/v1"
        } else {
            "$apiEndpoint/v1"
        }
    }
}
```

## Variant Manifests

Create variant-specific `AndroidManifest.xml` for environment-specific configuration:

```
app/src/staging/AndroidManifest.xml
app/src/production/AndroidManifest.xml
```

**Staging manifest example:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Staging-specific manifest entries -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    
    <application>
        <!-- Staging activity for testing -->
        <activity android:name="com.deproof.debug.DebugActivity" />
    </application>
</manifest>
```

Gradle merges manifests in order:
1. `app/src/main/AndroidManifest.xml` (base)
2. `app/src/{buildType}/AndroidManifest.xml`
3. `app/src/{flavor}/AndroidManifest.xml`
4. `app/src/{flavor}{BuildType}/AndroidManifest.xml`

## Managing Variant Dependencies

Add variant-specific dependencies in `build.gradle.kts`:

```kotlin
dependencies {
    // All variants
    implementation("androidx.compose.ui:ui:1.5.4")
    
    // Staging only
    stagingImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
    
    // Production release only
    productionReleaseImplementation("com.google.firebase:firebase-crashlytics-ktx:18.4.0")
    
    // Debug builds only
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")
}
```

## Variant Testing Strategy

### Unit Tests

```bash
# Test specific variant
./gradlew testStagingDebugUnitTest
./gradlew testProductionReleaseUnitTest

# Test all variants
./gradlew test
```

**Variant-specific test directories:**
```
app/src/test/
├── kotlin/
│   └── com/deproof/    # Shared tests
├── staging/
│   └── kotlin/         # Staging-only tests
└── production/
    └── kotlin/         # Production-only tests
```

### Instrumented Tests

```bash
./gradlew installStagingDebugAndroidTest
./gradlew connectedStagingDebugAndroidTest
```

### Test Configuration Per Variant

```kotlin
// app/src/test/kotlin/com/deproof/ApiClientTest.kt
class ApiClientTest {
    @Test
    fun testApiEndpoint() {
        val endpoint = BuildConfig.API_ENDPOINT
        assertTrue(endpoint.contains("solana.com"))
    }
}
```

## Variant Selection in IDE

### Android Studio

1. **Build → Select Build Variant** (at the bottom left)
2. Choose variant from dropdown:
   - `stagingDebug`
   - `stagingRelease`
   - `productionDebug`
   - `productionRelease`
3. IDE configures for selected variant
4. Run or debug with that variant

### IDE Benefits of Variant Selection

- Code completion reflects correct variant code
- Resources highlight correct variant values
- Run/debug uses selected variant APK
- Logcat filters correct variant logs

## Common Variant Use Cases

### Development Setup

For developing against Solana devnet with verbose logging:
```bash
./gradlew installStagingDebug
adb shell am start -n com.deproof.app.staging/.presentation.MainActivity
```

### QA Testing

For comprehensive testing before production release:
```bash
# Test staging release (closest to production)
./gradlew assembleStagingRelease

# Verify against devnet
adb install -r app/build/outputs/apk/staging/release/app-staging-release.apk
```

### Production Release

For building production release for Google Play:
```bash
# Build production release
./gradlew assembleProductionRelease

# Verify signing
jarsigner -verify app/build/outputs/apk/production/release/app-release.apk

# Upload to Play Store
```

### Simultaneous Device Testing

Install both production and staging on same device:
```bash
./gradlew installProductionDebug
./gradlew installStagingDebug

# Both apps coexist (different package IDs)
adb shell cmd package list packages | grep deproof
# Output:
# package:com.deproof.app
# package:com.deproof.app.staging
```

## Build Variant Troubleshooting

### Issue: "No such variant: productionRelease"

**Cause:** Task name is case-sensitive

**Solution:**
```bash
# Correct variant names are camelCase
./gradlew assembleProductionRelease    # Correct
./gradlew assembleproductionrelease    # Wrong - fails
./gradlew assembleProduction_Release   # Wrong - fails
```

### Issue: "Duplicate resource in variant"

**Cause:** Multiple source sets provide same resource

**Solutions:**
1. Remove duplicate resource from one source set
2. Use variant-specific source directories:
   - Base: `app/src/main/res/`
   - Flavor: `app/src/{flavor}/res/`
   - Build type: `app/src/{buildType}/res/`
   - Variant: `app/src/{flavor}{BuildType}/res/`

### Issue: "EnvironmentConfig not found"

**Cause:** Missing variant-specific file or wrong package

**Solutions:**
```bash
# Verify file exists for both variants
ls -la app/src/production/kotlin/com/deproof/EnvironmentConfig.kt
ls -la app/src/staging/kotlin/com/deproof/EnvironmentConfig.kt

# Verify package name matches
grep "package com.deproof" app/src/production/kotlin/com/deproof/EnvironmentConfig.kt
```

### Issue: "Wrong BuildConfig values for variant"

**Cause:** BuildConfig not regenerated after variant change

**Solutions:**
```bash
# Clean build
./gradlew clean

# Rebuild current variant
./gradlew assembleDebug

# Or rebuild specific variant
./gradlew assembleStagingDebug --rerun-tasks
```

## Build Variant Workflow

### Typical Development Workflow

```bash
# 1. Select staging variant in Android Studio
# Build → Select Build Variant → stagingDebug

# 2. Develop and test against devnet
./gradlew runStagingDebug

# 3. Run tests for staging
./gradlew testStagingDebugUnitTest

# 4. Build staging release to verify obfuscation
./gradlew assembleStagingRelease

# 5. Test staging release on device
adb install app/build/outputs/apk/staging/release/app-staging-release.apk

# 6. When ready for production:
# Switch to productionDebug variant
# Test with mainnet API endpoint

# 7. Build production release
./gradlew assembleProductionRelease

# 8. Verify and upload to Play Store
```

### CI/CD Variant Building

```bash
# GitHub Actions / GitLab CI
./gradlew build                           # All variants

# Or specific:
./gradlew assembleProductionRelease       # For Play Store
./gradlew assembleStagingRelease          # For staging testing
```

## Best Practices

1. **Keep Variants Minimal**
   - Only create variants for genuinely different configurations
   - Avoid variant explosion (too many combinations)
   - Current setup (2 flavors × 2 build types = 4 variants) is ideal

2. **Manage Variant Code Carefully**
   - Shared code in `main` source set
   - Variant-specific only in variant directories
   - Use `BuildConfig` and `EnvironmentConfig` to parameterize behavior

3. **Test All Variants**
   - Run full test suite for each variant before release
   - Automate variant testing in CI/CD
   - Test production variant most rigorously

4. **Document Variant Differences**
   - Maintain README of API endpoint differences
   - Document feature flags per variant
   - Keep EnvironmentConfig values current

5. **Consistent Variant Selection**
   - Development: stagingDebug (for fast iteration)
   - Pre-release: stagingRelease (closest to production)
   - Production: productionRelease (for Play Store)

## References

- [Android Build Variants Documentation](https://developer.android.com/studio/build/build-variants)
- [Product Flavors Guide](https://developer.android.com/studio/build/build-variants#product-flavors)
- [BuildConfig Best Practices](https://developer.android.com/studio/build/gradle-tips#build_properties)
- [Gradle Configuration Documentation](https://developer.android.com/studio/build#buildConfig)
