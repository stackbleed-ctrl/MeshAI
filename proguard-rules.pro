# MeshAI ProGuard / R8 rules

# ---- Keep application entry points ----
-keep class com.meshai.MeshAIApp { *; }
-keep class com.meshai.ui.MainActivity { *; }
-keep class com.meshai.service.** { *; }
-keep class com.meshai.tools.call.** { *; }
-keep class com.meshai.tools.notification.** { *; }

# ---- Hilt ----
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.**

# ---- Kotlinx Serialization ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    *** serializer(...);
    <fields>;
}

# ---- Google Nearby Connections ----
-keep class com.google.android.gms.nearby.** { *; }
-dontwarn com.google.android.gms.**

# ---- MediaPipe ----
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# ---- Meshrabiya ----
-keep class com.ustadmobile.meshrabiya.** { *; }
-dontwarn com.ustadmobile.meshrabiya.**

# ---- OkHttp / Retrofit ----
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# ---- Timber ----
-dontwarn org.slf4j.**

# ---- Coroutines ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ---- Keep serialized data classes (mesh protocol) ----
-keep class com.meshai.mesh.MeshMessage { *; }
-keep class com.meshai.agent.AgentNode { *; }
-keep class com.meshai.agent.AgentTask { *; }

# ---- General Android ----
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
