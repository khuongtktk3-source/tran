# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Anti-Cracking & Decompilation obfuscation rules for Macro Automation engine
-keepattributes Signature,Exceptions,InnerClasses,EnclosingMethod,Deprecated,Annotation
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Moshi/Room models intact so database queries map smoothly
-keep class com.example.model.** { *; } 
-keep class com.example.data.database.** { *; }

-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}

# Heavily obfuscate engine algorithms and simulation execution
-repackageclasses 'com.example.secure.obf'
-allowaccessmodification

# Remove diagnostic logs in release builds to prevent string reverse engineering
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Maintain Jetpack Compose & standard libraries compatibility
-keep class androidx.compose.** { *; }
