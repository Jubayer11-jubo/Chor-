# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep source file and line numbers for crash stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*

# Keep CHOR Firestore data models for serialization/deserialization
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** {
    *;
}

# Firebase Firestore & Google Identity rules
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
