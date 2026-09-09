# ProGuard rules for App Control - Gestor Inteligente de Aplicaciones Android

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Room data classes must not be obfuscated (used in queries and serialization)
-keep class com.appcontrol.core.database.entity.** { *; }
-keep class com.appcontrol.core.database.dao.** { *; }
-keep class com.appcontrol.domain.model.** { *; }

# ── DataStore ─────────────────────────────────────────────────────────────────
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-dontwarn com.google.protobuf.**

# ── Jetpack Compose ──────────────────────────────────────────────────────────
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keepclassmembers class * {
    @androidx.compose.Composable <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ── Kotlin Serialization ─────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-keepclasseswithmembers class <1> {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.appcontrol.**$$serializer { *; }
-keepclassmembers class com.appcontrol.** {
    *** Companion;
}
-keepclasseswithmembers class com.appcontrol.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Navigation Compose ───────────────────────────────────────────────────────
-keepnames class androidx.navigation.** { *; }
-keepclassmembers class * extends androidx.navigation.Navigator { *; }
-keep class androidx.navigation.compose.** { *; }

# ── WorkManager ──────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keepclassmembers class * {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.appcontrol.worker.** { *; }
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.rxjava3.RxWorker

# ── Keep Annotations ─────────────────────────────────────────────────────────
-keep @androidx.annotation.Keep class *
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-keepattributes *Annotation*

# ── General Android Rules ────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-repackageclasses ''

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── Coroutines ───────────────────────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ── Android Lifecycle ────────────────────────────────────────────────────────
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }
-keep class * extends androidx.lifecycle.LiveData { *; }

# ── Accompanist / Coil ──────────────────────────────────────────────────────
-dontwarn coil.**
-keep class coil.** { *; }
-dontwarn com.google.accompanist.**

# ── Introspection ────────────────────────────────────────────────────────────
-keepattributes RuntimeVisibleAnnotations

# ── Koin ─────────────────────────────────────────────────────────────────────
-keep class org.koin.** { *; }
-keepclassmembers class * {
    public <init>(...);
}

# ── System UI Controller ─────────────────────────────────────────────────────
-keep class com.google.accompanist.systemuicontroller.** { *; }
