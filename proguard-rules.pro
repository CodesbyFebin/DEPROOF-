# ProGuard rules for Deproof
# Release APK optimization and obfuscation

# ===== General Rules =====

# Keep source file names and line numbers (for crash reports)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep methods with @Keep annotation
-keep @androidx.annotation.Keep class *
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ===== Kotlin Rules =====

-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

-keep class kotlin.** { *; }
-keep interface kotlin.** { *; }
-dontwarn kotlin.**

# Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlin serialization
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keep class **$serializer { *; }
-dontwarn kotlinx.serialization.**

# ===== Android X / Jetpack =====

-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# Compose
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# Room Database
-keep class androidx.room.** { *; }
-keepclasseswithmembers class * {
    @androidx.room.Dao <methods>;
}
-keepclasseswithmembers class * {
    @androidx.room.Entity <fields>;
}
-keepclasseswithmembers class * {
    @androidx.room.PrimaryKey <fields>;
}

# LiveData / ViewModel
-keep class androidx.lifecycle.** { *; }

# ===== App-Specific Rules =====

# Keep all app classes
-keep class com.deproof.** { *; }
-keepclassmembers class com.deproof.** {
    <init>(...);
    public <methods>;
    public <fields>;
}

# Keep ViewModels (if using reflection)
-keep class * extends androidx.lifecycle.ViewModel {
    public <init>();
}

# Keep Room DAOs
-keep interface com.deproof.data.local.db.** {
    <methods>;
}

# Keep data classes with Parcelable
-keep class com.deproof.domain.model.** {
    public <fields>;
}

# Keep sealed classes
-keep class com.deproof.** extends java.lang.Object {
    <init>(...);
}

# ===== Network Libraries =====

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Gson
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.SerializedName <fields>;
}
-dontwarn com.google.gson.**

# ===== Mobile Wallet Adapter =====

-keep class com.solanomobile.** { *; }
-dontwarn com.solanomobile.**

# ===== Security / Crypto =====

-keep class android.security.** { *; }
-keep class javax.crypto.** { *; }

# ===== Third-Party Libraries =====

# Timber (logging)
-keep class com.jakewharton.timber.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ===== Remove Logging (Optional) =====

# Remove all Log.d, Log.v, Log.i calls in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** println(...);
}

# Keep Log.w and Log.e for error reporting
-keepclassmembers class android.util.Log {
    public static *** e(...);
    public static *** w(...);
}

# ===== Optimization Options =====

-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*,!code/allocation/variable
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# ===== Don't Warn Rules =====

-dontwarn android.os.**
-dontwarn androidx.**
-dontwarn com.google.android.**

# ===== Keep App Entry Points =====

# Keep MainActivity
-keep class com.deproof.MainActivity {
    public <init>(...);
}

# Keep Application class
-keep class com.deproof.DeproofApp {
    public <init>(...);
}

# ===== Debugging Information =====

# Keep line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep InnerClasses and EnclosingMethod attributes
-keepattributes InnerClasses,EnclosingMethod

# Keep RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Keep annotations used by frameworks
-keepattributes *Annotation*

# ===== Platform-Specific Rules =====

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom application classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.app.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference

# ===== Suppress Specific Warnings =====

-dontwarn java.lang.invoke.**
-dontwarn java.util.concurrent.Flow**
-dontwarn javax.naming.**
-dontwarn sun.misc.**
-dontwarn sun.reflect.**
