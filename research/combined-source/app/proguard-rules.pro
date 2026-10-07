# Deproof ProGuard / R8 rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes EnclosingMethod,InnerClasses
-keep class com.deproof.** { *; }
-keep class com.example.domain.** { *; }
-keep class com.example.data.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn com.solana.**
