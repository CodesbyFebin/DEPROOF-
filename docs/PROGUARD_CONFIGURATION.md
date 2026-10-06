# ProGuard Configuration Guide (F052)

## Overview

ProGuard is configured for Deproof's release APK builds to:
- Optimize bytecode and reduce APK size
- Obfuscate code for intellectual property protection
- Maintain crash report stack trace readability

## Configuration File

**Location:** `app/proguard-rules.pro`

## Key Rules

### 1. General Preservation Rules

- **Source Files & Line Numbers:** Kept for crash reports
- **Enums:** All enum methods preserved
- **@Keep Annotation:** Methods/classes marked with `@androidx.annotation.Keep` are preserved

### 2. Kotlin & Coroutines

- Kotlin metadata preserved for reflection
- Coroutine classes retained for runtime behavior
- Serialization support maintained

### 3. AndroidX / Jetpack Libraries

- **Compose:** All UI framework classes preserved
- **Room:** DAO and Entity annotations maintained
- **Lifecycle:** ViewModel and LiveData support retained
- **DataStore:** Preference storage maintained

### 4. Deproof-Specific Rules

```
Domain Layer
├── DomainException hierarchy (preserved)
├── Retry mechanism (RetryConfig)
└── Result<T> sealed class

Data Layer
├── Repositories (RpcRepository, ReceiptRepository)
├── Room DAOs and Entities
└── EncryptedBackupManager

Presentation Layer
├── MainActivity & Activities
├── ViewModels
├── Navigation utilities
└── App Shortcuts Manager
```

### 5. Network & Serialization

- **OkHttp:** Complete library retained
- **Gson:** Serialization adapters and converters preserved
- **Annotations:** SerializedName kept for JSON mapping

### 6. Security

- Android Security Crypto library retained
- EncryptedBackupManager preserved
- Encryption utilities maintained

### 7. Code Removal

Debug logging is aggressively removed in release:
```proguard
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
```

Warning and Error logs retained for crash reporting.

## Optimization Settings

```proguard
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-optimizationpasses 5
```

- 5 optimization passes for deep optimization
- Selective disabling of certain simplifications to prevent bugs

## Testing Proguard Configuration

### 1. Build Release APK

```bash
./gradlew assembleRelease
```

### 2. Verify Essential Classes Remain

```bash
# Check if MainActivity is preserved
grep -r "MainActivity" app/build/outputs/mapping/release/mapping.txt

# Check if repositories are kept
grep -r "RpcRepository\|ReceiptRepository" app/build/outputs/mapping/release/mapping.txt
```

### 3. Test Deep Links in Release Build

```bash
adb install app/build/outputs/apk/release/app-release.apk
adb shell am start -a android.intent.action.VIEW -d "deproof://app/now" com.deproof.app
```

### 4. Verify Stack Traces

- Crash reports should show original method names (preserved via SourceFile attribute)
- Line numbers should match source code

### 5. Automated Checks

Run on release APK:
- Navigation to all screens works
- Deep links function correctly
- Exception handling preserved
- Repository calls successful

## Common Issues & Solutions

### Issue: App Crashes After ProGuard

**Causes:**
- Reflection on renamed classes
- Missing Keep rules
- Aggressive optimization

**Solution:**
1. Check mapping.txt for renamed classes
2. Add `-keep` rule for the offending class
3. Rebuild with `-dontshrink -dontoptimize` for debugging

### Issue: Obfuscated Stack Traces

**Cause:** By design - ProGuard removes identifying information

**Solution:** Use RetraceTool with mapping.txt:
```bash
retrace.sh mapping.txt crash_stacktrace.txt
```

### Issue: Network Library Errors

**Causes:** OkHttp or Gson classes obfuscated

**Solution:** Library-specific rules already included; verify both are present in proguard-rules.pro

## Best Practices

1. **Always Test Release Builds Locally**
   - Debug builds compile faster
   - Only release builds use ProGuard
   - Catch obfuscation issues early

2. **Keep Critical Classes Preserved**
   - Entry points (MainActivity, Application)
   - Exception hierarchies
   - Public APIs
   - Serializable models

3. **Monitor APK Size**
   ```bash
   bundletool analyze-bundle --bundle=app-release.aab --mode=size-total
   ```

4. **Maintain Mapping Files**
   - Save `mapping.txt` from each release
   - Use for stack trace retracing
   - Track optimization effectiveness

5. **Update Rules with Dependencies**
   - When upgrading libraries, review new classes
   - Add rules for new serializable types
   - Test thoroughly before shipping

## File Organization

```
proguard-rules.pro
├── General Rules (50 lines)
├── Kotlin Rules (40 lines)
├── AndroidX/Jetpack (60 lines)
├── App-Specific Rules (50 lines)
├── Third-Party Libraries (80 lines)
└── Deproof-Specific Rules (150 lines)
```

**Total:** ~400 configuration lines

## Maintenance Checklist

- [ ] Build release APK without errors
- [ ] Test deep links work
- [ ] Verify app starts on first launch
- [ ] Check no unhandled exceptions
- [ ] APK size acceptable
- [ ] Stack traces readable with mapping.txt
- [ ] All screens navigate correctly
- [ ] Repository calls successful
- [ ] Encryption/decryption works
- [ ] Network requests function

## References

- [ProGuard Manual](https://www.guardsquare.com/proguard/manual)
- [Android ProGuard Guide](https://developer.android.com/studio/build/shrink-code)
- [Compose Compiler Setup](https://developer.android.com/jetpack/androidx/releases/compose-compiler)
