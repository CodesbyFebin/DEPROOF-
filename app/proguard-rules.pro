# Deproof ProGuard / R8 rules
-keepattributes *Annotation*
-keep class com.example.domain.** { *; }
-keep class com.example.data.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn com.solana.**
