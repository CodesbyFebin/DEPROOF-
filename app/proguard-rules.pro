# Proguard rules for Deproof
# Keep all classes in the app package
-keep class com.deproof.** { *; }
-keep class com.deproof.**.** { *; }

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Keep debug symbols
-keepattributes LocalVariableTable,LocalVariableTypeTable

# Keep Solana wallet classes
-keep class ** { *; }
