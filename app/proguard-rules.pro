# SmartisanMusic Revived — ProGuard/R8 rules
# Keep Room entities used via DAO reflection
-keep class com.smartisanos.music.data.** { *; }
-dontwarn com.smartisanos.music.data.**

# Keep WebView JS interface classes (if needed)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep resource classes
-keep class **.R
-keep class **.R$* { *; }

# Keep enum classes (used in serialization/DataStore)
-keepclassmembers enum * { *; }

# Keep Kotlin coroutines internals
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Preserve line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# NetEase crypto/data classes (reflection by JSON parsing)
-keep class com.smartisanos.music.data.online.** { *; }

# Keep custom View classes
-keep class com.smartisanos.music.ui.widgets.** { *; }
-keep class com.smartisanos.widget.** { *; }
-keep class smartisanos.widget.** { *; }