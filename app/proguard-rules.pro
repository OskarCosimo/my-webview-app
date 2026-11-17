# app/proguard-rules.pro
# Add project specific ProGuard / R8 rules here

# Preserve source file names and line numbers for readable crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep JavascriptInterface annotation and exposed bridge methods for WebView
-keepattributes JavascriptInterface
-keepclassmembers class com.my.webviewapplication.mobile.MainActivity$AndroidInterface {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Android core components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep custom View constructors
-keep public class com.myetv.tv.mobile.CustomWebView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# WebViewClient callbacks
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public void *(android.webkit.WebView, java.lang.String);
    public boolean *(android.webkit.WebView, java.lang.String);
    public boolean *(android.webkit.WebView, android.webkit.WebResourceRequest);
}

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# ZXing
-keep class com.google.zxing.** { *; }
