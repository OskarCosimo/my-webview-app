# My Webview App (MWA) for Android
A customizable Android WebView app with advanced features: primary and secondary browser with security, smart url handling, gesture support, native-web bridge, qr-code reader, picture in picture and much more. Built for developers who need more than basic WebView.

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Language](https://img.shields.io/badge/Language-Java-orange.svg)
![License](https://img.shields.io/badge/License-Apache2-blue.svg)
![Build](https://img.shields.io/badge/Build-Gradle-brightgreen.svg)

A powerful and feature-rich Android WebView application with 20+ built-in language translations, Firebase integration, QR code scanning, and extensive customization options.

</div>

---

## 📋 Table of Contents

- [Features](#-features)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Configuration](#-configuration)
  - [Config.java Options](#configjava-options)
  - [Firebase Setup](#firebase-setup)
  - [Package Refactoring](#package-refactoring)
- [Customization](#-customization)
  - [App Assets](#app-assets)
  - [Images and Icons](#images-and-icons)
  - [Translations](#translations)
- [Building the App](#-building-the-app)
- [Advanced Features](#-advanced-features)
- [Troubleshooting](#-troubleshooting)
- [License](#-license)

---

## ✨ Features

- 🌐 **WebView with Internet or Offline Mode** - Load content from a URL or local HTML files
- 🔒 **Security Settings** - HTTPS enforcement, domain whitelisting, SSL certificate validation
- 🌍 **20+ Languages** - Pre-translated string resources for global reach
- 🔔 **Firebase Push Notifications** - Integrated Firebase Cloud Messaging (FCM)
- 📱 **QR Code Scanner** - Built-in QR code scanning with camera integration
- 🎨 **Customizable UI** - Full control over colors, splash screen, progress indicators
- 🔄 **Pull-to-Refresh** - Swipe-down gesture to reload content
- 📡 **Offline Mode** - Automatic detection and custom offline page
- 🖼️ **Picture-in-Picture** - Video playback in PiP mode (Android 7.0+)
- 🚫 **Do Not Disturb Mode** - Block notifications while app is active
- 🔗 **Deep Linking** - Custom URL scheme handling
- 🌙 **Dark/Light Theme Detection** - Automatic theme parameter injection
- ↔️ **Swipe Navigation** - Gesture-based back/forward navigation
- 📊 **Progress Indicators** - Multiple styles (spinner, linear progress)
- 🔐 **Cookie & Cache Management** - Fine-grained control over data storage
- 📱 **Orientation Lock** - Control portrait/landscape modes per device type

---

## 🔧 Prerequisites

Before you begin, ensure you have:

- **Android Studio** (Arctic Fox or later recommended)
- **JDK 8** or higher
- **Gradle 7.0+**
- **Android SDK** with minimum API 23 (Android 5.1)
- **Google account** for Firebase setup
- Basic knowledge of Java and Android development

---

## 📦 Installation

### 1. Clone or Download the Repository

```bash
git clone https://github.com/yourusername/my-webview-app.git
cd my-webview-app
```

### 2. Open Project in Android Studio

1. Launch **Android Studio**
2. Click **File → Open**
3. Navigate to the project folder and click **OK**
4. Wait for Gradle to sync dependencies

### 3. Verify Gradle Sync

Ensure all dependencies are downloaded correctly. Check the **Build** tab at the bottom of Android Studio for any errors.

---

## ⚙️ Configuration

### Config.java Options

The `Config.java` file located at `app/src/main/java/com/my/webviewapplication/mobile/Config.java` contains all configuration options. You **must** customize this file before building your app.

#### 📍 Main Configuration

```java
// Choose between internet URL or local offline HTML
public static final String START_URL_TYPE = "internet"; // Options: "internet", "local"

// Local HTML file path (used only if START_URL_TYPE = "local")
public static final String LOCAL_START_URL = "file:///android_asset/app/index.html";

// Main URL to load (used only if START_URL_TYPE = "internet")
public static final String HOME_URL = "https://example.com/?apptype=androidmobile";
```

**How to Use:**
- Set `START_URL_TYPE = "internet"` to load a website from `HOME_URL`
- Set `START_URL_TYPE = "local"` to load offline HTML from assets folder
- Update `HOME_URL` with your website URL
- Place local HTML files in `app/src/main/assets/app/` folder

---

#### 🔒 Security Settings

```java
// Only allow HTTPS URLs (reject HTTP)
public static final boolean LOAD_ONLY_HTTPS = false;

// Show warning before loading HTTP URLs
public static final boolean WARN_IF_NOT_HTTPS = true;

// Domains that open inside main WebView (all others open in secondary browser)
public static final String[] ALLOWED_DOMAINS = {
    "example.com",
    "www.example.com"
};

// Domains authorized to trigger special actions (closetheapp://, qrcode://, etc.)
public static final String[] SPECIAL_URL_ALLOWED_DOMAINS = {
    "example.com",
    "www.example.com"
};

// External domains that ALWAYS open in secondary WebView
public static final String[] SECONDARY_WEBVIEW_DOMAINS = {
    "external-site.com"
};
```

**Security Best Practices:**
- Add only your trusted domains to `ALLOWED_DOMAINS`
- Use `SPECIAL_URL_ALLOWED_DOMAINS` to restrict which pages can close the app or trigger actions
- Enable `LOAD_ONLY_HTTPS = true` for production apps to enforce secure connections

---

#### 🎨 Secondary Browser Configuration

Customize the appearance of the secondary WebView (for external links):

```java
// Secondary browser colors (Hex format)
public static final String SECONDARY_BROWSER_BG_COLOR = "#DAA520";
public static final String SECONDARY_BROWSER_BUTTON_COLOR = "#B8860B";
public static final String SECONDARY_BROWSER_TEXT_COLOR = "#FFFFFF";

// UI dimensions (in dp)
public static final int SECONDARY_BROWSER_TOP_BAR_HEIGHT = 56;
public static final int SECONDARY_BROWSER_BUTTON_SIZE = 48;

// Title visibility and styling
public static final boolean SECONDARY_BROWSER_SHOW_TITLE = true;
public static final int SECONDARY_BROWSER_TITLE_SIZE = 18;

// Close button customization
public static final String SECONDARY_BROWSER_BUTTON_TEXT = "✕";
public static final int SECONDARY_BROWSER_BUTTON_TEXT_SIZE = 24;
```

---

#### 🎬 Splash Screen Configuration

```java
// Enable/disable splash screen
public static final boolean ENABLE_SPLASH_SCREEN = true;

// Splash duration in milliseconds (3 seconds = 3000)
public static final int SPLASH_TIMEOUT_MS = 3000;

// Wait for WebView to finish loading before dismissing splash
public static final boolean SPLASH_WAIT_FOR_WEBVIEW = false;
```

**Note:** Splash screen image is located at `app/src/main/res/drawable/splash_logo.png`

---

#### 🌐 WebView Configuration

```java
// Enable JavaScript (required for modern websites)
public static final boolean ENABLE_JAVASCRIPT = true;

// Enable DOM storage for web apps
public static final boolean ENABLE_DOM_STORAGE = true;

// Hardware acceleration for better performance
public static final boolean ENABLE_HARDWARE_ACCELERATION = true;

// Cache management
public static final boolean CLEAR_CACHE_ON_START = true;
public static final boolean CLEAR_CACHE_ON_EXIT = true;
public static final boolean USE_CACHE = true;

// Cookie management
public static final boolean CLEAR_COOKIES_ON_START = false;
public static final boolean CLEAR_COOKIES_ON_EXIT = false;

// Pinch-to-zoom
public static final boolean ENABLE_ZOOM = false;

// Custom User-Agent (leave empty for default)
public static final String CUSTOM_USER_AGENT = "";

// Reload WebView when app goes to background
public static final boolean STOP_APP_IN_BACKGROUND = false;

// Add theme parameter to URL (adds ?theme=dark or ?theme=light)
public static final boolean ENHANCE_URL_WITH_THEME = true;
```

---

#### 📱 Orientation Settings

```java
// Lock orientation on phones
public static final String PHONE_ORIENTATION = "none"; // Options: "portrait", "landscape", "auto", "none"

// Lock orientation on tablets
public static final String TABLET_ORIENTATION = "none"; // Options: "portrait", "landscape", "auto", "none"
```

**Options Explained:**
- `"portrait"` - Force portrait mode
- `"landscape"` - Force landscape mode
- `"auto"` - Auto-rotate based on device sensor
- `"none"` - Use device default settings

---

#### 📷 QR Code Scanner

```java
// Enable QR code scanning feature
public static final boolean ENABLE_QR_SCANNER = true;
```

**Usage:** Navigate to `qrcode://` from your website to trigger the scanner.

---

#### 🔔 Firebase Push Notifications

```java
// Enable Firebase Cloud Messaging
public static final boolean ENABLE_FIREBASE_PUSH = true;

// Add FCM token to URL query string (?fcmtoken=XXXXX)
public static final boolean ENHANCE_URL_WITH_FCM_TOKEN = true;
```

**Setup Required:** See [Firebase Setup](#firebase-setup) section.

---

#### 📡 Offline Mode

```java
// Enable offline mode detection
public static final boolean ENABLE_OFFLINE_MODE = true;

// Path to offline HTML page
public static final String OFFLINE_HTML_PATH = "file:///android_asset/offline/index.html";

// Network check interval (milliseconds)
public static final long NETWORK_MONITOR_INTERVAL = 3000;

// Verify connection 3 times before showing offline page
public static final boolean STRICT_OFFLINE_MODE = true;
```

**Note:** Place custom offline page in `app/src/main/assets/offline/index.html`

---

#### 🔗 Special URL Handlers

Your website can trigger app actions using special URL schemes:

```java
// URL schemes that trigger app actions
public static final String URL_CLOSE_APP = "closetheapp://";
public static final String URL_OPEN_QR_SCANNER = "qrcode://";
public static final String URL_OPEN_APP_SETTINGS = "openappsettings://";
public static final String URL_SHARE_CONTENT = "shareapp://";
public static final String URL_PICTURE_IN_PICTURE = "pictureinpicture://";
public static final String URL_TOGGLE_NOTIFICATIONS = "togglenotifications://";
```

**Example Usage in HTML:**

```html
<!-- Close app -->
<a href="closetheapp://">Exit App</a>

<!-- Open QR scanner -->
<a href="qrcode://">Scan QR Code</a>

<!-- Share content -->
<a href="shareapp://sharetext/Check this out!/&url=https://example.com">Share</a>

<!-- Enable Picture-in-Picture -->
<a href="pictureinpicture://">Enable PiP</a>

<!-- Toggle Do Not Disturb -->
<a href="togglenotifications://">Toggle DND Mode</a>
```

**Security Note:** Special URLs only work from domains listed in `SPECIAL_URL_ALLOWED_DOMAINS`.

---

#### 🔐 Permissions

```java
// Request camera permission (required for QR scanner)
public static final boolean REQUIRE_CAMERA = true;

// Request notification permission (required for push notifications)
public static final boolean REQUIRE_NOTIFICATIONS = true;
```

---

#### 🔄 Pull-to-Refresh Settings

```java
// Enable pull-to-refresh for main WebView
public static final boolean ENABLEPULLTOREFRESH = false;

// Enable pull-to-refresh for secondary WebView
public static final boolean ENABLEPULLTOREFRESHSECONDARY = true;

// Refresh spinner color (hex with alpha: 0xAARRGGBB)
public static final int PULLTOREFRESHSPINNERCOLOR = 0xFF1DA1F2;

// Refresh background color
public static final int PULLTOREFRESHBACKGROUNDCOLOR = 0xFFFFFFFF;

// Trigger distance in pixels (lower = more sensitive)
public static final int PULLTOREFRESHDISTANCE = 100;

// Timeout in milliseconds (force stop spinner if page doesn't load)
public static final long PULLTOREFRESHDISAPPEARTIMEOUT = 5000;
```

---

#### 📊 Progress Bar Configuration

```java
// Show loading progress indicator
public static final boolean SHOW_PROGRESS_BAR = true;

// Progress bar style options
public static final String PROGRESS_BAR_STYLE = "linear_progress"; 
// Options: "spinner_center", "spinner_corner", "linear_progress"

// Progress bar color (hex with alpha: 0xAARRGGBB)
public static final int PROGRESS_BAR_COLOR = 0xFFB8860B;
```

**Style Options:**
- `"spinner_center"` - Large spinner in center with semi-transparent background
- `"spinner_corner"` - Small spinner in top-right corner (non-intrusive)
- `"linear_progress"` - Linear progress bar at top of screen (material design)

---

#### 🎨 UI Settings

```java
// Fullscreen mode (hides navigation bar)
public static final boolean FULLSCREEN_MODE = false;

// Hide status bar (immersive mode)
public static final boolean HIDE_STATUS_BAR = false;

// Status bar background color (hex format)
public static final String STATUS_BAR_COLOR = "#DAA520";

// Status bar text color (true = dark text, false = light text)
public static final boolean DARK_STATUS_BAR_TEXT = false;

// Enable swipe left/right for back/forward navigation
public static final boolean ENABLE_SWIPE_NAVIGATION = false;
```

---

#### 🚫 Do Not Disturb Mode

```java
// Enable DND mode feature (blocks notifications from other apps)
public static final boolean ENABLE_DND_MODE = true;
```

**How it Works:**
- When enabled via `togglenotifications://` URL, all notifications from other apps are blocked
- DND mode persists across app sessions
- User can toggle it on/off via special URL or by navigating away from app

---

#### ⬅️ Back Button Behavior

```java
// Exit app when back pressed on home page
public static final boolean EXIT_ON_BACK_PRESSED_HOME = true;

// Show confirmation dialog before exiting
public static final boolean SHOW_EXIT_DIALOG = true;
```

---

### Firebase Setup

To use Firebase push notifications, you need to configure Firebase for your app.

#### Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **Add project**
3. Enter your project name and follow the setup wizard
4. Once created, click on **Android icon** to add an Android app

#### Step 2: Register Your App

1. Enter your **package name** (e.g., `com.yourcompany.yourappname`)
   - **Important:** This must match the package name after you perform refactoring (see below)
2. Enter **App nickname** (optional)
3. Enter **Debug signing certificate SHA-1** (optional, for Google Sign-In)
4. Click **Register app**

#### Step 3: Download google-services.json

1. Download the `google-services.json` file
2. Place it in `app/` directory (same level as `build.gradle`)
   ```
   my-webview-app/
   ├── app/
   │   ├── google-services.json  ← Place here
   │   ├── build.gradle
   │   └── src/
   ```
3. **Do NOT commit this file to version control** (it contains API keys)

#### Step 4: Verify Firebase Configuration

1. Open `app/build.gradle` and ensure this line exists:
   ```gradle
   apply plugin: 'com.google.gms.google-services'
   ```

2. Sync Gradle and rebuild the project

#### Step 5: Test Push Notifications

1. Build and run the app on a device
2. Go to Firebase Console → **Cloud Messaging**
3. Click **Send your first message**
4. Enter notification title and text
5. Select your app and send

---

### Package Refactoring

The app comes with the default package name `com.my.webviewapplication.mobile`. You **must** change this to your own package name.

#### Step 1: Refactor Package Name in Android Studio

1. Open Android Studio
2. In the **Project** view, switch to **Project** mode (dropdown at top-left)
3. Navigate to `app/src/main/java/com/my/webviewapplication/mobile`
4. **Right-click** on `mobile` folder → **Refactor** → **Rename**
5. Select **Rename package**
6. Enter your new package name segment (e.g., `yourapp`)
7. Click **Refactor**
8. Repeat for each package segment:
   - Rename `webviewapplication` to `yourcompany`
   - Rename `my` to `com` (or your domain)

**Alternative Method (Rename Multiple Segments at Once):**

1. Click on the package name in any Java file (e.g., `package com.my.webviewapplication.mobile;`)
2. Press **Shift + F6** (or right-click → **Refactor** → **Rename**)
3. Enter new full package name: `com.yourcompany.yourapp`
4. Check **Search in comments and strings**
5. Check **Search for text occurrences**
6. Click **Refactor**

#### Step 2: Update ApplicationId in build.gradle

1. Open `app/build.gradle`
2. Find this line:
   ```gradle
   applicationId "com.my.webviewapplication.mobile"
   ```
3. Change it to:
   ```gradle
   applicationId "com.yourcompany.yourapp"
   ```

#### Step 3: Update AndroidManifest.xml

1. Open `app/src/main/AndroidManifest.xml`
2. Verify all references to the old package are updated:
   ```xml
   <manifest xmlns:android="http://schemas.android.com/apk/res/android"
       package="com.yourcompany.yourapp">
   ```

#### Step 4: Sync Gradle

1. Click **File → Sync Project with Gradle Files**
2. Clean and rebuild: **Build → Clean Project**, then **Build → Rebuild Project**

#### Step 5: Update Firebase Configuration

**Important:** After refactoring, you **must** update Firebase with your new package name:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click **Project Settings** (gear icon)
4. Scroll to **Your apps** section
5. Either:
   - **Delete** the old app and add a new one with the new package name, OR
   - **Add a new Android app** with the new package name
6. Download the new `google-services.json` file
7. Replace the old file in `app/` directory

---

## 🎨 Customization

### App Assets

#### Offline HTML Content

To add offline web content that loads when `START_URL_TYPE = "local"`:

1. Navigate to `app/src/main/assets/app/`
2. Place your `index.html` and related files (CSS, JS, images) here
3. Folder structure example:
   ```
   app/src/main/assets/app/
   ├── index.html
   ├── style.css
   ├── script.js
   └── images/
       └── logo.png
   ```
4. Update `Config.java`:
   ```java
   public static final String LOCAL_START_URL = "file:///android_asset/app/index.html";
   ```

**In Android Studio:**
- Right-click `app/src/main/assets` → **New → Directory** → Name it `app`
- Drag and drop your HTML files into this folder

#### Offline Mode HTML

Customize the offline error page shown when internet is unavailable:

1. Navigate to `app/src/main/assets/offline/`
2. Edit `index.html` with your custom design
3. Add CSS/JS files in the same folder
4. Reference them in your HTML:
   ```html
   <link rel="stylesheet" href="style.css">
   <script src="script.js"></script>
   ```

---

### Images and Icons

#### App Icon (Launcher Icon)

1. Prepare icon images in multiple resolutions:
   - **mdpi:** 48x48 px
   - **hdpi:** 72x72 px
   - **xhdpi:** 96x96 px
   - **xxhdpi:** 144x144 px
   - **xxxhdpi:** 192x192 px

2. **Using Android Studio Image Asset Studio (Recommended):**
   - Right-click `app/src/main/res` → **New → Image Asset**
   - Select **Launcher Icons**
   - Choose **Image** as asset type
   - Upload your icon (preferably 512x512 PNG)
   - Customize background and foreground
   - Click **Next** → **Finish**

3. **Manual Method:**
   - Place icon files in respective folders:
     ```
     app/src/main/res/
     ├── mipmap-mdpi/ic_launcher.png
     ├── mipmap-hdpi/ic_launcher.png
     ├── mipmap-xhdpi/ic_launcher.png
     ├── mipmap-xxhdpi/ic_launcher.png
     └── mipmap-xxxhdpi/ic_launcher.png
     ```

#### Splash Screen Logo

1. Prepare logo image (PNG with transparency recommended)
2. Replace `app/src/main/res/drawable/splash_logo.png`
3. **In Android Studio:**
   - Navigate to `app/src/main/res/drawable/`
   - Delete existing `splash_logo.png`
   - Right-click `drawable` → **New → Image Asset**
   - Or simply drag and drop your file and name it `splash_logo.png`

**Size Recommendations:**
- Use at least 512x512 pixels for best quality
- PNG format with transparent background works best
- Logo will be centered on splash screen

#### Notification Icon

1. Prepare notification icon (white silhouette on transparent background)
2. Replace files in:
   ```
   app/src/main/res/
   ├── drawable-mdpi/ic_notification.png (24x24)
   ├── drawable-hdpi/ic_notification.png (36x36)
   ├── drawable-xhdpi/ic_notification.png (48x48)
   ├── drawable-xxhdpi/ic_notification.png (72x72)
   └── drawable-xxxhdpi/ic_notification.png (96x96)
   ```

**Icon Guidelines:**
- Must be white silhouette on transparent background
- Simple, flat design (no gradients)
- Used for Firebase push notifications

---

### Translations

The app includes 20 pre-translated languages. All text strings are defined in XML files located at:

```
app/src/main/res/
├── values/strings.xml (English - Default)
├── values-es/strings.xml (Spanish)
├── values-fr/strings.xml (French)
├── values-de/strings.xml (German)
├── values-it/strings.xml (Italian)
├── values-pt/strings.xml (Portuguese)
├── values-ru/strings.xml (Russian)
├── values-zh/strings.xml (Chinese)
├── values-ja/strings.xml (Japanese)
├── values-ko/strings.xml (Korean)
├── values-ar/strings.xml (Arabic)
├── values-hi/strings.xml (Hindi)
├── values-nl/strings.xml (Dutch)
├── values-pl/strings.xml (Polish)
├── values-tr/strings.xml (Turkish)
├── values-sv/strings.xml (Swedish)
├── values-da/strings.xml (Danish)
├── values-no/strings.xml (Norwegian)
├── values-fi/strings.xml (Finnish)
└── values-cs/strings.xml (Czech)
```

#### How to Edit Translations

1. Open Android Studio
2. Navigate to `app/src/main/res/values/strings.xml`
3. Edit the default English strings:
   ```xml
   <string name="app_name">My Webview App</string>
   <string name="loading">Loading...</string>
   ```
4. To edit other languages, open the corresponding `values-XX/strings.xml` file

#### How to Add New Languages

1. Right-click `app/src/main/res` → **New → Android Resource Directory**
2. Select **Resource type:** `values`
3. Select **Locale**
4. Choose language from dropdown (e.g., `Greek (el)`)
5. Click **OK**
6. Copy `values/strings.xml` to the new folder
7. Translate all strings to the new language

#### Important String Keys

```xml
<!-- App name (shown in launcher) -->
<string name="app_name">My Webview App</string>

<!-- Permission rationale -->
<string name="camera_permission_rationale">Camera access needed for QR scanner</string>
<string name="notification_permission_rationale">Enable notifications for updates</string>

<!-- Offline mode -->
<string name="offline_title">No Internet Connection</string>
<string name="offline_message">Please check your connection and try again</string>

<!-- Exit dialog -->
<string name="exit_dialog_title">Exit App?</string>
<string name="exit_dialog_message">Do you want to close the app?</string>

<!-- QR Scanner -->
<string name="scan_cancelled">Scan cancelled</string>

<!-- HTTP warning (when loading non-HTTPS sites) -->
<string name="http_warning_title">Insecure Connection</string>
<string name="http_warning_message">This site uses HTTP (not secure). Continue?</string>
```

**Tip:** Use Android Studio's **Translations Editor** for easier management:
- Right-click `strings.xml` → **Open Translations Editor**
- View and edit all languages in a table format

---

## 🔨 Building the App

### Debug Build (For Testing)

1. Connect your Android device via USB or start an emulator
2. Click **Run** button (green play icon) in Android Studio
3. Or use command line:
   ```bash
   ./gradlew assembleDebug
   ```
4. APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build (For Distribution)

#### Step 1: Generate Signing Key

1. In Android Studio: **Build → Generate Signed Bundle / APK**
2. Select **APK** or **Android App Bundle**
3. Click **Create new...** (keystore)
4. Fill in the form:
   - **Key store path:** Choose location (e.g., `my-release-key.jks`)
   - **Password:** Create strong password
   - **Alias:** Enter key alias (e.g., `my-key-alias`)
   - **Validity:** 25 years (recommended)
   - Fill in certificate information
5. Click **OK**

**Important:** Keep your keystore file and passwords safe! You'll need them for all future updates.

#### Step 2: Configure Signing in build.gradle

1. Open `app/build.gradle`
2. Add signing configuration:
   ```gradle
   android {
       ...
       signingConfigs {
           release {
               storeFile file("path/to/my-release-key.jks")
               storePassword "your_keystore_password"
               keyAlias "my-key-alias"
               keyPassword "your_key_password"
           }
       }

       buildTypes {
           release {
               signingConfig signingConfigs.release
               minifyEnabled true
               proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
           }
       }
   }
   ```

**Security Note:** Do NOT commit passwords to Git. Use environment variables or keystore.properties file.

#### Step 3: Build Release APK

1. **Build → Generate Signed Bundle / APK**
2. Select **APK**
3. Choose your keystore and enter passwords
4. Select **release** build variant
5. Click **Finish**

Or via command line:
```bash
./gradlew assembleRelease
```

Release APK will be at: `app/build/outputs/apk/release/app-release.apk`

#### Step 4: Optimize and Test

1. Test the release APK on multiple devices
2. Check app size (consider enabling code shrinking)
3. Verify all features work correctly
4. Test offline mode, permissions, and push notifications

---

## 🚀 Advanced Features

### JavaScript Interface

The app exposes a JavaScript interface for communication between web content and Android:

```javascript
// Check if interface is available
if (typeof AndroidInterface !== 'undefined') {
    // Get FCM token
    var fcmToken = AndroidInterface.getFcmToken();
    console.log("FCM Token:", fcmToken);
}
```

**Available Methods:**
- `getFcmToken()` - Returns Firebase Cloud Messaging token

### Service Worker Support

The app includes Service Worker support for Progressive Web Apps (PWA):

```javascript
// Register service worker in your web app
if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js')
        .then(reg => console.log('Service Worker registered', reg))
        .catch(err => console.error('Service Worker registration failed', err));
}
```

### Deep Linking

Configure deep links to open specific content when users click links:

1. Edit `AndroidManifest.xml`:
   ```xml
   <intent-filter android:autoVerify="true">
       <action android:name="android.intent.action.VIEW" />
       <category android:name="android.intent.category.DEFAULT" />
       <category android:name="android.intent.category.BROWSABLE" />

       <data android:scheme="https" />
       <data android:host="example.com" />
       <data android:pathPrefix="/app" />
   </intent-filter>
   ```

2. Links like `https://example.com/app/page` will open directly in your app

### File Upload Support

The app supports file uploads from web pages:

```html
<!-- HTML file input -->
<input type="file" accept="image/*" />
<input type="file" accept="video/*" capture="camera" />
```

### Geolocation API

Web pages can request location with user permission:

```javascript
navigator.geolocation.getCurrentPosition(
    (position) => {
        console.log("Lat:", position.coords.latitude);
        console.log("Lon:", position.coords.longitude);
    },
    (error) => console.error("Location error:", error)
);
```

---

## 🐛 Troubleshooting

### Common Issues

#### Issue: "google-services.json not found"

**Solution:**
1. Download `google-services.json` from Firebase Console
2. Place it in `app/` directory (not `app/src/`)
3. Sync Gradle: **File → Sync Project with Gradle Files**

---

#### Issue: App crashes on launch

**Solution:**
1. Check `Config.java` for invalid values (e.g., malformed URLs, invalid hex colors)
2. Verify `HOME_URL` is accessible
3. Check Android Studio **Logcat** for crash details
4. Common fixes:
   - Ensure `HOME_URL` starts with `http://` or `https://`
   - Verify hex colors start with `#` or `0xFF`
   - Check array syntax (comma-separated strings)

---

#### Issue: QR Scanner doesn't work

**Solution:**
1. Verify `Config.ENABLE_QR_SCANNER = true`
2. Check camera permission is granted
3. Ensure `Config.REQUIRE_CAMERA = true`
4. Test on physical device (camera doesn't work on emulators)

---

#### Issue: Push notifications not received

**Solution:**
1. Verify `Config.ENABLE_FIREBASE_PUSH = true`
2. Check `google-services.json` is properly configured
3. Verify package name matches in Firebase Console
4. Test on physical device (emulator may not receive notifications)
5. Check Firebase Console for error logs
6. Verify notification permission is granted

---

#### Issue: Special URLs (closetheapp://, etc.) don't work

**Solution:**
1. Check that current page domain is listed in `Config.SPECIAL_URL_ALLOWED_DOMAINS`
2. Verify URL scheme matches exactly (e.g., `closetheapp://` not `closeapp://`)
3. Test from an allowed domain page
4. Check Logcat for security warnings

---

#### Issue: White screen or blank page

**Solution:**
1. Check internet connection
2. Verify `Config.HOME_URL` is correct and accessible
3. Disable HTTPS-only mode temporarily: `Config.LOAD_ONLY_HTTPS = false`
4. Check WebView settings: `Config.ENABLE_JAVASCRIPT = true`
5. Clear cache: `Config.CLEAR_CACHE_ON_START = true`
6. Check for SSL certificate errors in Logcat

---

#### Issue: App orientation doesn't change

**Solution:**
1. Check `Config.PHONE_ORIENTATION` setting
2. Set to `"auto"` for auto-rotation
3. Verify device auto-rotate is enabled in system settings
4. Rebuild app after changing Config.java

---

#### Issue: Gradle sync failed

**Solution:**
1. Update Android Studio to latest version
2. Update Gradle wrapper: Edit `gradle/wrapper/gradle-wrapper.properties`
   ```properties
   distributionUrl=https\://services.gradle.org/distributions/gradle-7.5-all.zip
   ```
3. Clear Gradle cache:
   - **File → Invalidate Caches / Restart → Invalidate and Restart**
4. Check internet connection (Gradle needs to download dependencies)

---

### Debugging Tips

1. **Enable Verbose Logging:**
   - Open **Logcat** in Android Studio
   - Filter by app package name
   - Look for tags: `MainActivity`, `SplashActivity`, `Firebase`

2. **Test on Real Device:**
   - Many features (camera, notifications, sensors) don't work well on emulators
   - Enable **Developer Options** and **USB Debugging** on device

3. **Remote Debugging WebView:**
   - Open Chrome on desktop
   - Navigate to `chrome://inspect`
   - Connect your device
   - Click **Inspect** on your WebView

4. **Check Config Values:**
   - Add Log statements in Config.java:
     ```java
     Log.d("Config", "HOME_URL: " + HOME_URL);
     ```

---

## 📄 License

```
MIT License

Copyright (c) 2024 [Your Name/Company]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📧 Support

If you encounter any issues or have questions:

1. Check the [Troubleshooting](#-troubleshooting) section
2. Search existing GitHub Issues
3. Create a new Issue with detailed description
4. Include relevant logs from Android Studio Logcat

---

## 🌟 Acknowledgments

- Built with Android Studio and Java
- Firebase Cloud Messaging for push notifications
- ZXing library for QR code scanning
- Material Design components

---

<div align="center">

**Made with ❤️ for the Android community**

[⬆ Back to Top](#my-webview-app-for-android)

</div>
