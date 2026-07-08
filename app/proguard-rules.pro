# MaidCleaner ProGuard Rules

# Keep Shizuku API classes
-keep class rikka.shizuku.** { *; }

# Keep Room entities
-keep class com.maidcleaner.data.local.entity.** { *; }

# Keep serializable models
-keepclassmembers class com.maidcleaner.data.model.** {
    *;
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile **;
}
