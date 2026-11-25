# R8 Full Mode Keep Rules for MagicMusic

# Kotlin Metadata & Serialization
-keepclassmembers class * {
    @kotlin.Metadata <fields>;
}
-keepclassmembers class **$Companion {
    public *;
}

# Moshi (Spotify Web API DTOs)
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.example.core.accounts.**Dto { *; }
-keep class com.example.data.** { *; }

# Retrofit & OkHttp
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_snuff.IgnoreJRERequirement
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# AppAuth PKCE & Chrome Custom Tabs
-keep class net.openid.appauth.** { *; }
-dontwarn net.openid.appauth.**

# Google Tink + Keystore
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn java.lang.invoke.StringConcatFactory

# Room Entities & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.Embedded <fields>;
    @androidx.room.Relation <fields>;
}

# Credential Manager & Google ID
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Media3 / ExoPlayer & WorkManager
-keep class androidx.media3.** { *; }
-keep class com.example.core.library.LibraryScanWorker { *; }

# Strip debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
