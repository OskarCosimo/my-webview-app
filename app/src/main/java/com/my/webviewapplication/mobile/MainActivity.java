package com.my.webviewapplication.mobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PictureInPictureParams;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ServiceWorkerClient;
import android.webkit.ServiceWorkerController;
import android.webkit.ServiceWorkerWebSettings;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.messaging.FirebaseMessaging;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Callback interface for HTTP warning dialog result.
 */
interface HTTPSWarningCallback {
    void onResult(boolean proceed);
}

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Distinct permission request codes to avoid collisions
    private static final int REQ_STARTUP_CAMERA = 100;
    private static final int REQ_STARTUP_NOTIFICATIONS = 101;
    private static final int REQ_WEBVIEW_CAMERA = 200;
    private static final int REQ_WEBVIEW_AUDIO = 201;
    private static final int REQ_WEBVIEW_GEOLOCATION = 202;
    private static final int REQ_QR_CAMERA = 203;

    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    private static final long STRICT_OFFLINE_CONFIRM_DELAY_MS = 2000L;

    private static final String PREF_NAME = "MWAPrefs";
    private static final String PREF_DND_ENABLED = "dnd_mode_enabled";
    //TODO : change the website with your website
    private static final String SHARER_ENDPOINT = "https://www.mywebsite.com/sharer/?url=";

    private CustomWebView mainWebView;
    private CustomWebView secondaryWebView;

    private FrameLayout webViewContainer;
    private FrameLayout secondaryContainer;
    private FrameLayout secondaryWebViewContainer;
    private View offlineView;

    private SwipeRefreshLayout swipeRefreshLayout;
    private SwipeRefreshLayout secondarySwipeRefreshLayout;
    private ProgressBarManager mainProgressBarManager;
    private ProgressBarManager secondaryProgressBarManager;

    private TextView secondaryUrlDisplay;
    private ImageView secondarySslIcon;
    private SslCertificate lastSecondarySSLCertificate;

    private boolean isOffline = false;
    private boolean hasLaunchedWithCustomIntent = false;
    private boolean wasSecondaryVisibleBeforePip = false;
    private String fcmToken = "";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isNetworkCallbackRegistered = false;
    private Runnable pendingOfflineRunnable;

    private GestureDetector gestureDetector;
    private OnBackPressedCallback onBackPressedCallback;

    private PermissionRequest pendingPermissionRequest;
    private GeolocationPermissions.Callback geolocationCallback;
    private String geolocationOrigin;
    private ValueCallback<Uri[]> mFilePathCallback;

    // Do Not Disturb mode tracking
    private boolean isDndModeActive = false;
    private int previousInterruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL;
    private boolean isRestoringDnd = false;

    private final ActivityResultLauncher<ScanOptions> qrScannerLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, R.string.scan_cancelled, Toast.LENGTH_SHORT).show();
                    return;
                }

                String scannedUrl = result.getContents().trim();
                if (scannedUrl.startsWith("http://") || scannedUrl.startsWith("https://")) {
                    loadUrlSafelyInApp(scannedUrl);
                } else if (scannedUrl.contains(".") && !scannedUrl.contains(" ")) {
                    loadUrlSafelyInApp("https://" + scannedUrl);
                } else {
                    Toast.makeText(this, "Invalid URL: " + scannedUrl, Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<Intent> fileChooserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (mFilePathCallback == null) {
                    return;
                }

                Uri[] results = null;
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        int count = clipData.getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = clipData.getItemAt(i).getUri();
                        }
                    } else if (data.getData() != null) {
                        results = new Uri[]{data.getData()};
                    }
                }

                mFilePathCallback.onReceiveValue(results);
                mFilePathCallback = null;
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initializeViews();
        mainWebView.addJavascriptInterface(new AndroidInterface(), "AndroidInterface");

        initializeProgressBars();
        setupUI();
        setupOrientation();
        initializeBackButtonHandler();

        if (Config.ENABLE_SWIPE_NAVIGATION) {
            initializeSwipeNavigation();
        }

        setupServiceWorker();
        checkPermissions();

        if (Config.ENABLE_FIREBASE_PUSH) {
            initializeFirebase();
        }

        // Check network and load startup content (deep link, share intent, or home URL)
        checkNetworkAndLoad();
        handleIntent(getIntent());
    }

    @Override
    protected void onPause() {
        super.onPause();

        CookieManager.getInstance().flush();

        if (isDndModeActive) {
            disableDndMode(false);
        }

        stopNetworkMonitoring();

        boolean inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode();
        if (!inPip && Config.STOP_APP_IN_BACKGROUND) {
            if (mainWebView != null) {
                mainWebView.onPause();
                mainWebView.pauseTimers();
            }
            if (secondaryWebView != null) {
                secondaryWebView.onPause();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mainWebView != null) {
            mainWebView.onResume();
            mainWebView.resumeTimers();

            boolean inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode();
            if (Config.STOP_APP_IN_BACKGROUND && !inPip && !hasLaunchedWithCustomIntent) {
                loadMainUrl();
            }
        }

        if (secondaryWebView != null) {
            secondaryWebView.onResume();
        }

        if (Config.ENABLE_DND_MODE && !isRestoringDnd) {
            isRestoringDnd = true;
            if (isDndPermissionGranted() && loadDndPreference() && !isDndModeActive) {
                enableDndMode(false);
            }
            isRestoringDnd = false;
        }

        if (!"local".equalsIgnoreCase(Config.START_URL_TYPE)) {
            startNetworkMonitoring();
            if (isOffline && isNetworkAvailable()) {
                restoreFromOffline();
            }
        }
    }

    private void initializeViews() {
        webViewContainer = findViewById(R.id.webview_container);
        secondaryContainer = findViewById(R.id.secondary_container);
        secondaryWebViewContainer = findViewById(R.id.secondary_webview_container);
        offlineView = findViewById(R.id.offline_view);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        if (Config.ENABLEPULLTOREFRESH && swipeRefreshLayout != null) {
            initializeSwipeRefresh();
        } else if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(false);
        }

        secondarySwipeRefreshLayout = findViewById(R.id.secondary_swipe_refresh_layout);
        if (Config.ENABLEPULLTOREFRESHSECONDARY && secondarySwipeRefreshLayout != null) {
            initializeSecondarySwipeRefresh();
        } else if (secondarySwipeRefreshLayout != null) {
            secondarySwipeRefreshLayout.setEnabled(false);
        }

        mainWebView = new CustomWebView(this);
        setupWebView(mainWebView, true);
        webViewContainer.addView(mainWebView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        mainWebView.setOnScrollChangeListener((scrollY, isAtTop) -> {
            if (swipeRefreshLayout != null && Config.ENABLEPULLTOREFRESH) {
                if (swipeRefreshLayout.isEnabled() != isAtTop) {
                    swipeRefreshLayout.setEnabled(isAtTop);
                }
            }
        });

        secondaryUrlDisplay = findViewById(R.id.secondary_url_display);
        secondarySslIcon = findViewById(R.id.secondary_ssl_icon);
        if (secondarySslIcon != null) {
            secondarySslIcon.setOnClickListener(v -> showSSLCertificateDialog());
        }

        LinearLayout secondaryTopBar = findViewById(R.id.secondary_top_bar);
        if (secondaryTopBar != null) {
            secondaryTopBar.setOnClickListener(v -> showSSLCertificateDialog());
        }

        if (secondaryContainer != null) {
            secondaryContainer.setVisibility(View.GONE);
        }

        View closeBtn = findViewById(R.id.close_secondary_webview);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> closeSecondaryWebView());
        }

        applySecondaryBrowserColors();
    }

    private void ensureSecondaryWebViewInitialized() {
        if (secondaryWebView != null) {
            return;
        }

        secondaryWebView = new CustomWebView(MainActivity.this);
        setupWebView(secondaryWebView, false);

        if (secondaryWebViewContainer != null) {
            secondaryWebViewContainer.addView(secondaryWebView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
        }

        secondaryWebView.setOnScrollChangeListener((scrollY, isAtTop) -> {
            if (secondarySwipeRefreshLayout != null && Config.ENABLEPULLTOREFRESHSECONDARY) {
                if (secondarySwipeRefreshLayout.isEnabled() != isAtTop) {
                    secondarySwipeRefreshLayout.setEnabled(isAtTop);
                }
            }
        });
    }

    private void initializeSwipeRefresh() {
        if (swipeRefreshLayout == null) {
            return;
        }

        swipeRefreshLayout.setEnabled(false);
        swipeRefreshLayout.setColorSchemeColors(Config.PULLTOREFRESHSPINNERCOLOR);
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(Config.PULLTOREFRESHBACKGROUNDCOLOR);
        swipeRefreshLayout.setDistanceToTriggerSync(Config.PULLTOREFRESHDISTANCE);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (mainWebView == null || !mainWebView.isAtTop()) {
                swipeRefreshLayout.setRefreshing(false);
                return;
            }

            String url = mainWebView.getUrl();
            if (TextUtils.isEmpty(url)) {
                swipeRefreshLayout.setRefreshing(false);
                return;
            }

            mainWebView.reload();
            mainWebView.postDelayed(() -> {
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }, Config.PULLTOREFRESHDISAPPEARTIMEOUT);
        });
    }

    private void initializeSecondarySwipeRefresh() {
        if (secondarySwipeRefreshLayout == null) {
            return;
        }

        secondarySwipeRefreshLayout.setEnabled(false);
        secondarySwipeRefreshLayout.setColorSchemeColors(Config.PULLTOREFRESHSPINNERCOLOR);
        secondarySwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Config.PULLTOREFRESHBACKGROUNDCOLOR);
        secondarySwipeRefreshLayout.setDistanceToTriggerSync(Config.PULLTOREFRESHDISTANCE);

        secondarySwipeRefreshLayout.setOnRefreshListener(() -> {
            if (secondaryWebView == null || !secondaryWebView.isAtTop()) {
                secondarySwipeRefreshLayout.setRefreshing(false);
                return;
            }

            String url = secondaryWebView.getUrl();
            if (TextUtils.isEmpty(url)) {
                secondarySwipeRefreshLayout.setRefreshing(false);
                return;
            }

            secondaryWebView.reload();
            secondaryWebView.postDelayed(() -> {
                if (secondarySwipeRefreshLayout != null && secondarySwipeRefreshLayout.isRefreshing()) {
                    secondarySwipeRefreshLayout.setRefreshing(false);
                }
            }, Config.PULLTOREFRESHDISAPPEARTIMEOUT);
        });
    }

    private void updateSSLCertificateIcon(@Nullable String url, @Nullable SslCertificate sslCertificate) {
        if (secondarySslIcon == null) {
            return;
        }

        try {
            if (!TextUtils.isEmpty(url) && !"about:blank".equals(url)) {
                lastSecondarySSLCertificate = sslCertificate;

                if (url.startsWith("https://")) {
                    if (sslCertificate != null) {
                        secondarySslIcon.setImageResource(android.R.drawable.ic_secure);
                        secondarySslIcon.setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                    } else {
                        secondarySslIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                        secondarySslIcon.setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_IN);
                    }
                    secondarySslIcon.setVisibility(View.VISIBLE);
                } else if (url.startsWith("http://")) {
                    secondarySslIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                    secondarySslIcon.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                    secondarySslIcon.setVisibility(View.VISIBLE);
                } else {
                    secondarySslIcon.setVisibility(View.GONE);
                    lastSecondarySSLCertificate = null;
                }
            } else {
                secondarySslIcon.setVisibility(View.GONE);
                lastSecondarySSLCertificate = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating SSL icon: " + e.getMessage());
            secondarySslIcon.setVisibility(View.GONE);
        }
    }

    private void checkHTTPSWarningAsync(@Nullable String url, @NonNull HTTPSWarningCallback callback) {
        if (!Config.WARN_IF_NOT_HTTPS) {
            callback.onResult(true);
            return;
        }

        if (url != null && url.startsWith("http://")) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(getString(R.string.http_warning_title))
                    .setMessage(getString(R.string.http_warning_message))
                    .setPositiveButton(getString(R.string.http_warning_continue),
                            (dialog, which) -> callback.onResult(true))
                    .setNegativeButton(getString(R.string.http_warning_cancel),
                            (dialog, which) -> callback.onResult(false))
                    .setCancelable(false)
                    .show();
        } else {
            callback.onResult(true);
        }
    }

    private void showSSLCertificateDialog() {
        String url = "about:blank";
        if (secondaryWebView != null && !TextUtils.isEmpty(secondaryWebView.getUrl())) {
            url = secondaryWebView.getUrl();
        }

        final String finalUrl = url;
        StringBuilder message = new StringBuilder();
        message.append("=== URL ===\n").append(finalUrl).append("\n\n");

        if (lastSecondarySSLCertificate == null) {
            message.append("=== Certificate ===\nNo certificate information available");
        } else {
            try {
                String issuedBy = lastSecondarySSLCertificate.getIssuedBy().getDName();
                String issuedTo = lastSecondarySSLCertificate.getIssuedTo().getDName();
                String validNotBefore = lastSecondarySSLCertificate.getValidNotBefore();
                String validNotAfter = lastSecondarySSLCertificate.getValidNotAfter();

                message.append("=== Certificate ===\n")
                        .append("Issued To:\n").append(issuedTo).append("\n\n")
                        .append("Issued By:\n").append(issuedBy).append("\n\n")
                        .append("Valid From:\n").append(validNotBefore).append("\n\n")
                        .append("Valid Until:\n").append(validNotAfter);
            } catch (Exception e) {
                message.append("=== Certificate ===\nError reading certificate information");
            }
        }

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Site Information")
                .setMessage(message.toString())
                .setPositiveButton("Copy URL", (dialog, which) -> {
                    ClipboardManager clipboard =
                            (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("URL", finalUrl);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(MainActivity.this, "URL copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private String getCurrentTheme() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES ? "dark" : "light";
    }

    private void applySecondaryBrowserColors() {
        try {
            LinearLayout topBar = findViewById(R.id.secondary_top_bar);
            TextView titleView = findViewById(R.id.secondary_title);
            MaterialButton closeButton = findViewById(R.id.close_secondary_webview);

            float density = getResources().getDisplayMetrics().density;

            if (topBar != null) {
                topBar.setBackgroundColor(Color.parseColor(Config.SECONDARY_BROWSER_BG_COLOR));
                ViewGroup.LayoutParams topBarParams = topBar.getLayoutParams();
                topBarParams.height = (int) (Config.SECONDARY_BROWSER_TOP_BAR_HEIGHT * density);
                topBar.setLayoutParams(topBarParams);
                topBar.setElevation(Config.SECONDARY_BROWSER_TOP_BAR_ELEVATION * density);
            }

            if (titleView != null) {
                titleView.setText(Config.SECONDARY_BROWSER_TITLE);
                titleView.setTextColor(Color.parseColor(Config.SECONDARY_BROWSER_TEXT_COLOR));
                titleView.setVisibility(Config.SECONDARY_BROWSER_SHOW_TITLE ? View.VISIBLE : View.GONE);
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, Config.SECONDARY_BROWSER_TITLE_SIZE);
            }

            if (secondaryUrlDisplay != null) {
                secondaryUrlDisplay.setTextColor(Color.parseColor(Config.SECONDARY_BROWSER_TEXT_COLOR));
            }

            if (closeButton != null) {
                closeButton.setTextColor(Color.parseColor(Config.SECONDARY_BROWSER_TEXT_COLOR));
                closeButton.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor(Config.SECONDARY_BROWSER_BUTTON_COLOR)));
                ViewGroup.LayoutParams buttonParams = closeButton.getLayoutParams();
                int buttonSize = (int) (Config.SECONDARY_BROWSER_BUTTON_SIZE * density);
                buttonParams.width = buttonSize;
                buttonParams.height = buttonSize;
                closeButton.setLayoutParams(buttonParams);
                closeButton.setText(Config.SECONDARY_BROWSER_BUTTON_TEXT);
                closeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, Config.SECONDARY_BROWSER_BUTTON_TEXT_SIZE);
                closeButton.setCornerRadius((int) (Config.SECONDARY_BROWSER_BUTTON_CORNER_RADIUS * density));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying secondary browser colors: " + e.getMessage());
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(@NonNull WebView webView, boolean isMain) {
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(Config.ENABLE_JAVASCRIPT);
        settings.setDomStorageEnabled(Config.ENABLE_DOM_STORAGE);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(Config.ENABLE_ZOOM);
        settings.setBuiltInZoomControls(Config.ENABLE_ZOOM);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        // Restrict file:// and content:// access on untrusted secondary browser
        boolean allowLocalFileAccess = isMain && "local".equalsIgnoreCase(Config.START_URL_TYPE);
        settings.setAllowFileAccess(allowLocalFileAccess);
        settings.setAllowContentAccess(isMain);

        // HTML5 Geolocation API support
        settings.setGeolocationEnabled(true);

        // Cache management
        if (isMain && Config.CLEAR_CACHE_ON_START) {
            webView.clearCache(true);
        }
        settings.setCacheMode(Config.USE_CACHE ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_NO_CACHE);
        settings.setDatabaseEnabled(true);

        // Cookies management
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (isMain && Config.CLEAR_COOKIES_ON_START) {
            cookieManager.removeAllCookies(null);
            cookieManager.flush();
        }
        cookieManager.setAcceptThirdPartyCookies(webView, isMain);

        // Harden mixed content policy
        settings.setMixedContentMode(Config.LOAD_ONLY_HTTPS
                ? WebSettings.MIXED_CONTENT_NEVER_ALLOW
                : WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        if (!Config.CUSTOM_USER_AGENT.isEmpty()) {
            settings.setUserAgentString(Config.CUSTOM_USER_AGENT);
        }

        if (Config.ENABLE_HARDWARE_ACCELERATION) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        webView.setWebViewClient(new MyWebViewClient(isMain));
        webView.setWebChromeClient(new MyWebChromeClient());
    }

    private void setupUI() {
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        if (Config.FULLSCREEN_MODE) {
            View statusBarBg = findViewById(R.id.statusBarBackground);
            if (statusBarBg != null) {
                statusBarBg.setVisibility(View.GONE);
            }
            removeStatusBarPadding();

            if (Config.HIDE_STATUS_BAR && insetsController != null) {
                insetsController.setSystemBarsBehavior(
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                insetsController.hide(WindowInsetsCompat.Type.systemBars());
            } else if (insetsController != null) {
                insetsController.show(WindowInsetsCompat.Type.systemBars());
            }
        } else {
            if (insetsController != null) {
                insetsController.show(WindowInsetsCompat.Type.systemBars());
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                try {
                    //noinspection deprecation
                    getWindow().setStatusBarColor(Color.parseColor(Config.STATUS_BAR_COLOR));
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid Config.STATUS_BAR_COLOR", e);
                }
            }
            setupStatusBarBackground();
        }

        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(Config.DARK_STATUS_BAR_TEXT);
        }
    }

    private void setupStatusBarBackground() {
        View rootView = findViewById(android.R.id.content);
        if (rootView == null) {
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );

            int statusBarHeight = insets.top;

            View statusBarBg = findViewById(R.id.statusBarBackground);
            if (statusBarBg != null) {
                statusBarBg.setVisibility(View.VISIBLE);
                statusBarBg.getLayoutParams().height = statusBarHeight;
                try {
                    statusBarBg.setBackgroundColor(Color.parseColor(Config.STATUS_BAR_COLOR));
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid Config.STATUS_BAR_COLOR", e);
                }
                statusBarBg.requestLayout();
            }

            View secondaryStatusBarBg = findViewById(R.id.secondaryStatusBarBackground);
            if (secondaryStatusBarBg != null) {
                secondaryStatusBarBg.setVisibility(View.VISIBLE);
                secondaryStatusBarBg.getLayoutParams().height = statusBarHeight;
                try {
                    secondaryStatusBarBg.setBackgroundColor(Color.parseColor(Config.STATUS_BAR_COLOR));
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid Config.STATUS_BAR_COLOR", e);
                }
                secondaryStatusBarBg.requestLayout();
            }

            if (webViewContainer != null) {
                webViewContainer.setPadding(insets.left, statusBarHeight, insets.right, insets.bottom);
            }

            if (offlineView != null) {
                offlineView.setPadding(insets.left, statusBarHeight, insets.right, insets.bottom);
            }

            LinearLayout secondaryTopBar = findViewById(R.id.secondary_top_bar);
            if (secondaryTopBar != null) {
                ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) secondaryTopBar.getLayoutParams();
                params.topMargin = statusBarHeight;
                params.leftMargin = insets.left;
                params.rightMargin = insets.right;
                secondaryTopBar.setLayoutParams(params);
            }

            if (secondarySwipeRefreshLayout != null) {
                ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) secondarySwipeRefreshLayout.getLayoutParams();
                int topBarHeightPx = (int) (Config.SECONDARY_BROWSER_TOP_BAR_HEIGHT
                        * getResources().getDisplayMetrics().density);
                params.topMargin = topBarHeightPx + statusBarHeight;
                params.leftMargin = insets.left;
                params.rightMargin = insets.right;
                params.bottomMargin = insets.bottom;
                secondarySwipeRefreshLayout.setLayoutParams(params);
            }

            return windowInsets;
        });

        ViewCompat.requestApplyInsets(rootView);
    }

    private void removeStatusBarPadding() {
        View statusBarBg = findViewById(R.id.statusBarBackground);
        if (statusBarBg != null) {
            statusBarBg.setVisibility(View.GONE);
        }

        View secondaryStatusBarBg = findViewById(R.id.secondaryStatusBarBackground);
        if (secondaryStatusBarBg != null) {
            secondaryStatusBarBg.setVisibility(View.GONE);
        }

        if (webViewContainer != null) {
            webViewContainer.setPadding(0, 0, 0, 0);
        }

        if (offlineView != null) {
            offlineView.setPadding(0, 0, 0, 0);
        }

        LinearLayout secondaryTopBar = findViewById(R.id.secondary_top_bar);
        if (secondaryTopBar != null) {
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) secondaryTopBar.getLayoutParams();
            params.topMargin = 0;
            params.leftMargin = 0;
            params.rightMargin = 0;
            secondaryTopBar.setLayoutParams(params);
        }

        if (secondarySwipeRefreshLayout != null) {
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) secondarySwipeRefreshLayout.getLayoutParams();
            int topBarHeightPx = (int) (Config.SECONDARY_BROWSER_TOP_BAR_HEIGHT
                    * getResources().getDisplayMetrics().density);
            params.topMargin = topBarHeightPx;
            params.leftMargin = 0;
            params.rightMargin = 0;
            params.bottomMargin = 0;
            secondarySwipeRefreshLayout.setLayoutParams(params);
        }
    }

    private void toggleDndMode() {
        if (!Config.ENABLE_DND_MODE) {
            Toast.makeText(this, R.string.dnd_mode_disabled_config, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isDndPermissionGranted()) {
            showDndPermissionDialog();
            return;
        }

        if (isDndModeActive) {
            disableDndMode(true);
            saveDndPreference(false);
        } else {
            enableDndMode(true);
            saveDndPreference(true);
        }
    }

    private void saveDndPreference(boolean enabled) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            prefs.edit().putBoolean(PREF_DND_ENABLED, enabled).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving DND preference: " + e.getMessage());
        }
    }

    private boolean loadDndPreference() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            return prefs.getBoolean(PREF_DND_ENABLED, false);
        } catch (Exception e) {
            Log.e(TAG, "Error loading DND preference: " + e.getMessage());
            return false;
        }
    }

    private void enableDndMode(boolean showToast) {
        try {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                return;
            }
            previousInterruptionFilter = notificationManager.getCurrentInterruptionFilter();
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
            isDndModeActive = true;
            if (showToast) {
                Toast.makeText(this, R.string.dnd_enabled, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error enabling DND mode: " + e.getMessage());
            if (showToast) {
                Toast.makeText(this, R.string.dnd_error_enable, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void disableDndMode(boolean showToast) {
        try {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                return;
            }
            notificationManager.setInterruptionFilter(previousInterruptionFilter);
            isDndModeActive = false;
            if (showToast) {
                Toast.makeText(this, R.string.dnd_disabled, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error disabling DND mode: " + e.getMessage());
            if (showToast) {
                Toast.makeText(this, R.string.dnd_error_disable, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDndPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dnd_permission_title)
                .setMessage(R.string.dnd_permission_message)
                .setPositiveButton(R.string.dnd_permission_open_settings, (dialog, which) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                    } catch (Exception e) {
                        Toast.makeText(this, R.string.dnd_error_open_settings, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.dnd_permission_cancel, null)
                .show();
    }

    private boolean isDndPermissionGranted() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return notificationManager != null && notificationManager.isNotificationPolicyAccessGranted();
    }

    private void setupServiceWorker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                ServiceWorkerController swController = ServiceWorkerController.getInstance();
                ServiceWorkerWebSettings swSettings = swController.getServiceWorkerWebSettings();
                swSettings.setCacheMode(Config.USE_CACHE ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_NO_CACHE);
                swSettings.setAllowContentAccess(true);
                swSettings.setAllowFileAccess(false);
                swSettings.setBlockNetworkLoads(false);

                swController.setServiceWorkerClient(new ServiceWorkerClient() {
                    @Override
                    public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
                        return null;
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error configuring Service Worker: " + e.getMessage());
            }
        }
    }

    private void setupOrientation() {
        String orientation = isTablet() ? Config.TABLET_ORIENTATION : Config.PHONE_ORIENTATION;
        switch (orientation.toLowerCase()) {
            case "portrait":
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case "landscape":
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case "auto":
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                break;
            case "none":
            default:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                break;
        }
    }

    private boolean isTablet() {
        return (getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    private void checkPermissions() {
        if (Config.REQUIRE_CAMERA && !isPermissionGranted(Manifest.permission.CAMERA)) {
            requestPermission(Manifest.permission.CAMERA, REQ_STARTUP_CAMERA);
        } else {
            checkNotificationPermission();
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (Config.REQUIRE_NOTIFICATIONS
                    && !isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)) {
                requestPermission(Manifest.permission.POST_NOTIFICATIONS, REQ_STARTUP_NOTIFICATIONS);
            }
        }
    }

    private boolean isPermissionGranted(@NonNull String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(@NonNull String permission, int requestCode) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
    }

    private void initializeFirebase() {
        if (!Config.ENABLE_FIREBASE_PUSH) {
            return;
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (TextUtils.isEmpty(token)) {
                        return;
                    }
                    fcmToken = token;
                    if (BuildConfig.IS_DEBUG_MODE) {
                        Log.d(TAG, "FCM Token obtained");
                    }

                    if (Config.ENHANCE_URL_WITH_FCM_TOKEN
                            && !"local".equalsIgnoreCase(Config.START_URL_TYPE)) {
                        runOnUiThread(() -> {
                            if (mainWebView == null) {
                                return;
                            }

                            if ("REDIRECT".equalsIgnoreCase(Config.FCM_TOKEN_TRANSMISSION_MODE)) {
                                if (!hasLaunchedWithCustomIntent) {
                                    loadMainUrl();
                                }
                            } else {
                                String safeToken = JSONObject.quote(fcmToken);
                                String jsCode = String.format(
                                        "if (typeof window.%s === 'function') { window.%s(%s); }",
                                        Config.FCM_JS_CALLBACK_FUNCTION,
                                        Config.FCM_JS_CALLBACK_FUNCTION,
                                        safeToken
                                );
                                mainWebView.evaluateJavascript(jsCode, null);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Fetching FCM registration token failed", e));
    }

    private void checkNetworkAndLoad() {
        if ("local".equalsIgnoreCase(Config.START_URL_TYPE)) {
            loadMainUrl();
            return;
        }

        if (isNetworkAvailable()) {
            loadMainUrl();
        } else {
            showOfflineMode();
        }
        startNetworkMonitoring();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }

        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        );
    }

    private void loadMainUrl() {
        String url;

        Intent intent = getIntent();
        String action = intent != null ? intent.getAction() : null;
        String type = intent != null ? intent.getType() : null;

        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (!TextUtils.isEmpty(sharedText)) {
                try {
                    String encodedUrl = URLEncoder.encode(sharedText, "UTF-8");
                    url = SHARER_ENDPOINT + encodedUrl;
                    hasLaunchedWithCustomIntent = true;
                } catch (UnsupportedEncodingException e) {
                    url = Config.HOME_URL;
                }
            } else {
                url = Config.HOME_URL;
            }
        } else {
            if ("local".equalsIgnoreCase(Config.START_URL_TYPE)) {
                url = Config.LOCAL_START_URL;
            } else {
                url = Config.HOME_URL;
                if (Config.ENABLE_FIREBASE_PUSH
                        && Config.ENHANCE_URL_WITH_FCM_TOKEN
                        && !TextUtils.isEmpty(fcmToken)) {
                    url += (url.contains("?") ? "&" : "?") + "fcmtoken=" + Uri.encode(fcmToken);
                }
                if (Config.ENHANCE_URL_WITH_THEME) {
                    url += (url.contains("?") ? "&" : "?") + "apptheme=" + getCurrentTheme();
                }
            }
        }

        mainWebView.loadUrl(url);
        webViewContainer.setVisibility(View.VISIBLE);
        offlineView.setVisibility(View.GONE);
        isOffline = false;
    }

    private void showOfflineMode() {
        if (!Config.ENABLE_OFFLINE_MODE) {
            return;
        }

        if (!isNetworkAvailable()) {
            offlineView.setVisibility(View.VISIBLE);
            webViewContainer.setVisibility(View.GONE);
            isOffline = true;
        }
    }

    private void restoreFromOffline() {
        isOffline = false;
        offlineView.setVisibility(View.GONE);
        webViewContainer.setVisibility(View.VISIBLE);

        if (Config.STOP_APP_IN_BACKGROUND || mainWebView == null || TextUtils.isEmpty(mainWebView.getUrl())) {
            loadMainUrl();
        }
    }

    private void startNetworkMonitoring() {
        if ("local".equalsIgnoreCase(Config.START_URL_TYPE) || isNetworkCallbackRegistered) {
            return;
        }

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                mainHandler.post(() -> {
                    if (pendingOfflineRunnable != null) {
                        mainHandler.removeCallbacks(pendingOfflineRunnable);
                        pendingOfflineRunnable = null;
                    }
                    if (isOffline && isNetworkAvailable()) {
                        restoreFromOffline();
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                mainHandler.post(() -> {
                    if (pendingOfflineRunnable != null) {
                        mainHandler.removeCallbacks(pendingOfflineRunnable);
                    }
                    pendingOfflineRunnable = () -> {
                        if (!isNetworkAvailable() && !isOffline) {
                            showOfflineMode();
                        }
                    };
                    long delay = Config.STRICT_OFFLINE_MODE ? STRICT_OFFLINE_CONFIRM_DELAY_MS : 0L;
                    mainHandler.postDelayed(pendingOfflineRunnable, delay);
                });
            }
        };

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } else {
                NetworkRequest request = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();
                connectivityManager.registerNetworkCallback(request, networkCallback);
            }
            isNetworkCallbackRegistered = true;
        } catch (Exception e) {
            Log.e(TAG, "Error registering network callback: " + e.getMessage());
        }
    }

    private void stopNetworkMonitoring() {
        if (pendingOfflineRunnable != null) {
            mainHandler.removeCallbacks(pendingOfflineRunnable);
            pendingOfflineRunnable = null;
        }
        if (connectivityManager != null && networkCallback != null && isNetworkCallbackRegistered) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callback: " + e.getMessage());
            }
            isNetworkCallbackRegistered = false;
        }
    }

    private void loadUrlSafelyInApp(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        if (isSpecialUrl(url)) {
            handleSpecialUrl(mainWebView, url);
            return;
        }

        if (Config.LOAD_ONLY_HTTPS && !url.startsWith("https://")) {
            Toast.makeText(this, "Only HTTPS connections are allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            return;
        }

        if (isExternalDomain(url)) {
            openInSecondaryWebView(url);
        } else if (mainWebView != null) {
            mainWebView.loadUrl(url);
        }
    }

    private class MyWebViewClient extends WebViewClient {
        private final boolean isMainWebView;

        MyWebViewClient(boolean isMain) {
            this.isMainWebView = isMain;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (url == null || url.startsWith("file://") || url.startsWith("data:") || url.startsWith("about:")) {
                return;
            }

            if (Config.SHOW_PROGRESS_BAR) {
                if (isMainWebView && mainProgressBarManager != null) {
                    mainProgressBarManager.show();
                } else if (!isMainWebView && secondaryProgressBarManager != null) {
                    secondaryProgressBarManager.show();
                }
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            CookieManager.getInstance().flush();

            if (isMainWebView) {
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                if (Config.SHOW_PROGRESS_BAR && mainProgressBarManager != null) {
                    mainProgressBarManager.hide();
                }
            } else {
                updateSecondaryUrlDisplay(url);
                updateSSLCertificateIcon(url, view.getCertificate());

                if (secondarySwipeRefreshLayout != null && secondarySwipeRefreshLayout.isRefreshing()) {
                    secondarySwipeRefreshLayout.setRefreshing(false);
                }
                if (Config.SHOW_PROGRESS_BAR && secondaryProgressBarManager != null) {
                    secondaryProgressBarManager.hide();
                }
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            if (!isMainWebView) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.ssl_error_title))
                        .setMessage(getString(R.string.ssl_error_message) + "\n\n"
                                + "Error: " + error.toString() + "\n\n"
                                + getString(R.string.ssl_error_question))
                        .setPositiveButton(getString(R.string.http_warning_continue), (dialog, which) -> {
                            handler.proceed();
                            updateSSLCertificateIcon(view.getUrl(), error.getCertificate());
                        })
                        .setNegativeButton(getString(R.string.http_warning_cancel), (dialog, which) -> {
                            handler.cancel();
                            updateSSLCertificateIcon(view.getUrl(), null);
                            if (secondaryProgressBarManager != null) {
                                secondaryProgressBarManager.hide();
                            }
                        })
                        .show();
            } else {
                handler.cancel();
                if (mainProgressBarManager != null) {
                    mainProgressBarManager.hide();
                }
            }
        }

        private boolean isCustomScheme(@NonNull String url) {
            for (String scheme : Config.CUSTOM_SCHEMES) {
                if (url.startsWith(scheme)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);

            if (request.isForMainFrame()) {
                if (isMainWebView && mainProgressBarManager != null) {
                    mainProgressBarManager.hide();
                } else if (!isMainWebView && secondaryProgressBarManager != null) {
                    secondaryProgressBarManager.hide();
                }
                if (isMainWebView && swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                } else if (!isMainWebView && secondarySwipeRefreshLayout != null && secondarySwipeRefreshLayout.isRefreshing()) {
                    secondarySwipeRefreshLayout.setRefreshing(false);
                }
            }

            String url = request.getUrl().toString();
            if (isCustomScheme(url) || url.startsWith("file://")) {
                return;
            }

            String path = request.getUrl().getPath();
            if (path != null) {
                String lowerPath = path.toLowerCase();
                if (lowerPath.endsWith(".mp4") || lowerPath.endsWith(".webm") || lowerPath.endsWith(".mkv")
                        || lowerPath.endsWith(".mp3") || lowerPath.endsWith(".m4a") || lowerPath.endsWith(".wav")
                        || lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".png")
                        || lowerPath.endsWith(".gif") || lowerPath.endsWith(".svg") || lowerPath.endsWith(".webp")) {
                    return;
                }
            }

            if (Config.ENABLE_OFFLINE_MODE && isMainWebView && request.isForMainFrame()) {
                if (!isNetworkAvailable()) {
                    showOfflineMode();
                }
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();

            if (handleSpecialUrl(view, url)) {
                return true;
            }

            if (Config.LOAD_ONLY_HTTPS
                    && !url.startsWith("https://")
                    && !url.startsWith("file://")
                    && !isCustomScheme(url)) {
                Toast.makeText(MainActivity.this, "Only HTTPS connections are allowed", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (isMainWebView && isExternalDomain(url)) {
                if ("POST".equalsIgnoreCase(request.getMethod())) {
                    return false;
                }
                openInSecondaryWebView(url);
                return true;
            }

            return false;
        }
    }

    private boolean handleSpecialUrl(@Nullable WebView sourceWebView, @Nullable String url) {
        if (!isSpecialUrl(url)) {
            return false;
        }

        String sourceUrl = sourceWebView != null ? sourceWebView.getUrl() : null;
        if (!isUrlFromAllowedDomains(sourceUrl, Config.SPECIAL_URL_ALLOWED_DOMAINS)) {
            Log.w(TAG, "SECURITY: Special URL blocked from unauthorized origin: " + sourceUrl);
            Toast.makeText(this, "Action not allowed from this page", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (url.startsWith(Config.URL_CLOSE_APP)) {
            finishAndRemoveTask();
            return true;
        }

        if (url.startsWith(Config.URL_OPEN_QR_SCANNER)) {
            openQRScanner();
            return true;
        }

        if (url.startsWith(Config.URL_OPEN_APP_SETTINGS)) {
            openAppSettings();
            return true;
        }

        if (url.startsWith(Config.URL_PICTURE_IN_PICTURE)) {
            startPictureInPictureMode();
            return true;
        }

        if (url.startsWith(Config.URL_SHARE_CONTENT)) {
            handleShareUrl(url);
            return true;
        }

        if (url.startsWith(Config.URL_TOGGLE_NOTIFICATIONS)) {
            toggleDndMode();
            return true;
        }

        return false;
    }

    private boolean isSpecialUrl(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        return url.startsWith(Config.URL_CLOSE_APP)
                || url.startsWith(Config.URL_OPEN_QR_SCANNER)
                || url.startsWith(Config.URL_OPEN_APP_SETTINGS)
                || url.startsWith(Config.URL_PICTURE_IN_PICTURE)
                || url.startsWith(Config.URL_SHARE_CONTENT)
                || url.startsWith(Config.URL_TOGGLE_NOTIFICATIONS);
    }

    private boolean isUrlFromAllowedDomains(@Nullable String url, @NonNull String[] allowedDomains) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        String host = Uri.parse(url).getHost();
        if (TextUtils.isEmpty(host)) {
            return false;
        }

        host = host.toLowerCase().trim();
        for (String allowedDomain : allowedDomains) {
            String domain = allowedDomain.toLowerCase().trim();
            if (host.equals(domain) || host.endsWith("." + domain)) {
                return true;
            }
        }
        return false;
    }

    private void handleShareUrl(@NonNull String url) {
        try {
            String urlWithoutScheme = url.replace(Config.URL_SHARE_CONTENT, "");
            String shareText = null;
            String shareUrl = null;

            if (urlWithoutScheme.startsWith("sharetext")) {
                String remaining = urlWithoutScheme.substring("sharetext".length());
                if (remaining.contains("&url=")) {
                    String[] parts = remaining.split("&url=", 2);
                    shareText = Uri.decode(trimSlashes(parts[0]));
                    if (parts.length > 1) {
                        shareUrl = Uri.decode(parts[1]);
                    }
                } else {
                    shareText = Uri.decode(trimSlashes(remaining));
                }
            } else {
                String queryString = urlWithoutScheme.startsWith("?")
                        ? urlWithoutScheme.substring(1)
                        : urlWithoutScheme;

                if (!queryString.isEmpty()) {
                    for (String param : queryString.split("&")) {
                        String[] keyValue = param.split("=", 2);
                        if (keyValue.length == 2) {
                            String key = keyValue[0].trim();
                            String value = Uri.decode(keyValue[1]);
                            if ("sharetext".equalsIgnoreCase(key)) {
                                shareText = value;
                            } else if ("url".equalsIgnoreCase(key)) {
                                shareUrl = value;
                            }
                        }
                    }
                }
            }

            StringBuilder content = new StringBuilder();
            if (!TextUtils.isEmpty(shareText)) {
                content.append(shareText);
            }
            if (!TextUtils.isEmpty(shareUrl)) {
                if (content.length() > 0) {
                    content.append("\n\n");
                }
                content.append(shareUrl);
            }
            if (content.length() == 0) {
                content.append(getString(R.string.app_name));
            }

            shareContent(content.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error parsing share URL: " + e.getMessage(), e);
            Toast.makeText(this, "Error sharing content", Toast.LENGTH_SHORT).show();
        }
    }

    private String trimSlashes(@NonNull String value) {
        String result = value;
        if (result.startsWith("/")) {
            result = result.substring(1);
        }
        if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private boolean isExternalDomain(@NonNull String url) {
        String host = Uri.parse(url).getHost();
        if (TextUtils.isEmpty(host)) {
            return true;
        }
        host = host.toLowerCase().trim();

        for (String domain : Config.SECONDARY_WEBVIEW_DOMAINS) {
            String normalized = domain.toLowerCase().trim();
            if (host.equals(normalized) || host.endsWith("." + normalized)) {
                return true;
            }
        }

        for (String domain : Config.ALLOWED_DOMAINS) {
            String normalized = domain.toLowerCase().trim();
            if (host.equals(normalized) || host.endsWith("." + normalized)) {
                return false;
            }
        }

        return true;
    }

    private void openInSecondaryWebView(@NonNull String url) {
        if (Config.LOAD_ONLY_HTTPS && !url.startsWith("https://")) {
            Toast.makeText(this, "Only HTTPS connections are allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        checkHTTPSWarningAsync(url, proceed -> {
            if (!proceed) {
                if (secondaryContainer != null && secondaryContainer.getVisibility() == View.VISIBLE) {
                    closeSecondaryWebView();
                }
                return;
            }

            ensureSecondaryWebViewInitialized();
            if (secondaryContainer != null) {
                secondaryContainer.setVisibility(View.VISIBLE);
            }
            updateSecondaryUrlDisplay(url);
            secondaryWebView.loadUrl(url);
        });
    }

    private void closeSecondaryWebView() {
        try {
            if (secondaryProgressBarManager != null) {
                secondaryProgressBarManager.hide();
            }
            if (Config.ENABLEPULLTOREFRESHSECONDARY && secondarySwipeRefreshLayout != null) {
                secondarySwipeRefreshLayout.setRefreshing(false);
                secondarySwipeRefreshLayout.setEnabled(false);
            }
            if (secondaryWebView != null) {
                if (secondaryWebViewContainer != null) {
                    secondaryWebViewContainer.removeView(secondaryWebView);
                }
                secondaryWebView.stopLoading();
                secondaryWebView.destroy();
                secondaryWebView = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing secondary WebView: " + e.getMessage());
            secondaryWebView = null;
        } finally {
            lastSecondarySSLCertificate = null;
            if (secondarySslIcon != null) {
                secondarySslIcon.setVisibility(View.GONE);
            }
            if (secondaryContainer != null) {
                secondaryContainer.setVisibility(View.GONE);
            }
        }
    }

    private void updateSecondaryUrlDisplay(@Nullable String url) {
        if (secondaryUrlDisplay == null) {
            return;
        }

        if (TextUtils.isEmpty(url) || "about:blank".equals(url)) {
            secondaryUrlDisplay.setText("about:blank");
            return;
        }

        String displayUrl = url;
        if (url.startsWith("https://")) {
            displayUrl = url.substring(8);
        } else if (url.startsWith("http://")) {
            displayUrl = url.substring(7);
        }
        secondaryUrlDisplay.setText(displayUrl);
    }

    private void openQRScanner() {
        if (!Config.ENABLE_QR_SCANNER) {
            return;
        }

        if (!isPermissionGranted(Manifest.permission.CAMERA)) {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
            requestPermission(Manifest.permission.CAMERA, REQ_QR_CAMERA);
            return;
        }

        launchQRScanner();
    }

    private void launchQRScanner() {
        try {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt(getString(R.string.scan_qr_code));
            options.setCameraId(0);
            options.setBeepEnabled(true);
            options.setBarcodeImageEnabled(false);
            options.setOrientationLocked(false);
            qrScannerLauncher.launch(options);
        } catch (Exception e) {
            Log.e(TAG, "QR Scanner error: " + e.getMessage(), e);
            Toast.makeText(this, "Error opening scanner: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean isGranted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        switch (requestCode) {
            case REQ_STARTUP_CAMERA:
                checkNotificationPermission();
                break;

            case REQ_STARTUP_NOTIFICATIONS:
                break;

            case REQ_QR_CAMERA:
                if (isGranted) {
                    launchQRScanner();
                } else {
                    Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
                }
                break;

            case REQ_WEBVIEW_CAMERA:
            case REQ_WEBVIEW_AUDIO:
                if (pendingPermissionRequest != null) {
                    if (isGranted) {
                        pendingPermissionRequest.grant(pendingPermissionRequest.getResources());
                    } else {
                        pendingPermissionRequest.deny();
                    }
                    pendingPermissionRequest = null;
                }
                break;

            case REQ_WEBVIEW_GEOLOCATION:
                if (geolocationCallback != null) {
                    geolocationCallback.invoke(geolocationOrigin, isGranted, false);
                    geolocationCallback = null;
                    geolocationOrigin = null;
                }
                break;
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    private void startPictureInPictureMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                PictureInPictureParams params = new PictureInPictureParams.Builder().build();
                if (!enterPictureInPictureMode(params)) {
                    Toast.makeText(this, "Picture in Picture not supported", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "PiP error: " + e.getMessage());
                Toast.makeText(this, "PiP error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Picture in Picture requires Android 8.0+", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);

        if (isInPictureInPictureMode) {
            if (secondaryContainer != null && secondaryContainer.getVisibility() == View.VISIBLE) {
                wasSecondaryVisibleBeforePip = true;
                secondaryContainer.setVisibility(View.GONE);
            } else {
                wasSecondaryVisibleBeforePip = false;
            }
            if (mainWebView != null) {
                mainWebView.onResume();
                mainWebView.resumeTimers();
            }
        } else {
            if (wasSecondaryVisibleBeforePip && secondaryContainer != null && secondaryWebView != null) {
                secondaryContainer.setVisibility(View.VISIBLE);
            }
            wasSecondaryVisibleBeforePip = false;
            if (mainWebView != null) {
                mainWebView.onResume();
                mainWebView.resumeTimers();
            }
        }
    }

    private void shareContent(@NonNull String content) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
    }

    private void initializeProgressBars() {
        if (!Config.SHOW_PROGRESS_BAR) {
            return;
        }

        ProgressBarManager.ProgressBarStyle style;
        switch (Config.PROGRESS_BAR_STYLE.toLowerCase()) {
            case "spinner_center":
                style = ProgressBarManager.ProgressBarStyle.SPINNER_CENTER;
                break;
            case "linear_progress":
                style = ProgressBarManager.ProgressBarStyle.LINEAR_PROGRESS;
                break;
            case "spinner_corner":
            default:
                style = ProgressBarManager.ProgressBarStyle.SPINNER_CORNER;
                break;
        }

        mainProgressBarManager = new ProgressBarManager(
                this,
                webViewContainer,
                style,
                Config.PROGRESS_BAR_COLOR
        );

        secondaryProgressBarManager = new ProgressBarManager(
                this,
                secondaryWebViewContainer,
                style,
                Config.PROGRESS_BAR_COLOR
        );
    }

    private void initializeSwipeNavigation() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null) {
                    return false;
                }
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();

                if (Math.abs(diffX) > Math.abs(diffY)
                        && Math.abs(diffX) > SWIPE_THRESHOLD
                        && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                        && diffX > 0) {

                    if (secondaryContainer != null && secondaryContainer.getVisibility() == View.VISIBLE) {
                        if (secondaryWebView != null && secondaryWebView.canGoBack()) {
                            secondaryWebView.goBack();
                        } else {
                            closeSecondaryWebView();
                        }
                        return true;
                    } else if (mainWebView != null && mainWebView.canGoBack()) {
                        mainWebView.goBack();
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void initializeBackButtonHandler() {
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (secondaryContainer != null && secondaryContainer.getVisibility() == View.VISIBLE) {
                    if (secondaryWebView != null && secondaryWebView.canGoBack()) {
                        secondaryWebView.goBack();
                    } else {
                        closeSecondaryWebView();
                    }
                    return;
                }

                if (mainWebView != null && mainWebView.canGoBack() && !Config.EXIT_ON_BACK_PRESSED_HOME) {
                    mainWebView.goBack();
                    return;
                }

                handleAppExit();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    private void handleAppExit() {
        if (Config.SHOW_EXIT_DIALOG) {
            showExitDialog();
        } else {
            finish();
        }
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.exit_app)
                .setMessage(R.string.exit_app_message)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, which) -> finish())
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private class MyWebChromeClient extends WebChromeClient {
        private View customView;
        private CustomViewCallback customViewCallback;

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            if (!isUserGesture && view == secondaryWebView) {
                return false;
            }

            ensureSecondaryWebViewInitialized();
            if (secondaryContainer != null) {
                secondaryContainer.setVisibility(View.VISIBLE);
            }

            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(secondaryWebView);
            resultMsg.sendToTarget();
            return true;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (Config.SHOW_PROGRESS_BAR && newProgress >= 100) {
                if (view == mainWebView && mainProgressBarManager != null) {
                    mainProgressBarManager.hide();
                } else if (view == secondaryWebView && secondaryProgressBarManager != null) {
                    secondaryProgressBarManager.hide();
                }
            }
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (customView != null) {
                onHideCustomView();
                return;
            }

            customView = view;
            customViewCallback = callback;

            FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
            decorView.addView(customView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));

            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(getWindow(), decorView);
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsetsCompat.Type.systemBars());
            }
        }

        @Override
        public void onHideCustomView() {
            if (customView == null) {
                return;
            }

            FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
            decorView.removeView(customView);
            customView = null;

            if (customViewCallback != null) {
                customViewCallback.onCustomViewHidden();
                customViewCallback = null;
            }

            setupUI();
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if (BuildConfig.IS_DEBUG_MODE) {
                Log.d(TAG, "Console: " + consoleMessage.message()
                        + " -- From line " + consoleMessage.lineNumber()
                        + " of " + consoleMessage.sourceId());
            }
            return true;
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            String[] resources = request.getResources();
            for (String resource : resources) {
                if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                    if (!isPermissionGranted(Manifest.permission.CAMERA)) {
                        if (pendingPermissionRequest != null) {
                            pendingPermissionRequest.deny();
                        }
                        pendingPermissionRequest = request;
                        requestPermission(Manifest.permission.CAMERA, REQ_WEBVIEW_CAMERA);
                        return;
                    }
                    request.grant(resources);
                    return;
                } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                    if (!isPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
                        if (pendingPermissionRequest != null) {
                            pendingPermissionRequest.deny();
                        }
                        pendingPermissionRequest = request;
                        requestPermission(Manifest.permission.RECORD_AUDIO, REQ_WEBVIEW_AUDIO);
                        return;
                    }
                    request.grant(resources);
                    return;
                }
            }
            request.deny();
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin,
                                                       GeolocationPermissions.Callback callback) {
            if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (geolocationCallback != null) {
                    geolocationCallback.invoke(geolocationOrigin, false, false);
                }
                geolocationCallback = callback;
                geolocationOrigin = origin;
                requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQ_WEBVIEW_GEOLOCATION);
            } else {
                callback.invoke(origin, true, false);
            }
        }

        @Override
        public boolean onShowFileChooser(WebView webView,
                                         ValueCallback<Uri[]> filePathCallback,
                                         FileChooserParams fileChooserParams) {
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePathCallback;

            Intent chooserIntent;
            try {
                chooserIntent = fileChooserParams != null
                        ? fileChooserParams.createIntent()
                        : new Intent(Intent.ACTION_GET_CONTENT).setType("*/*");
            } catch (Exception e) {
                chooserIntent = new Intent(Intent.ACTION_GET_CONTENT);
                chooserIntent.addCategory(Intent.CATEGORY_OPENABLE);
                chooserIntent.setType("*/*");
            }

            try {
                fileChooserLauncher.launch(chooserIntent);
                return true;
            } catch (ActivityNotFoundException e) {
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                    mFilePathCallback = null;
                }
                Toast.makeText(MainActivity.this, "No file manager available", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (Config.ENABLE_SWIPE_NAVIGATION && gestureDetector != null) {
            gestureDetector.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        String type = intent.getType();
        Uri data = intent.getData();

        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (!TextUtils.isEmpty(sharedText)) {
                try {
                    String encodedUrl = URLEncoder.encode(sharedText, "UTF-8");
                    String sharerUrl = SHARER_ENDPOINT + encodedUrl;
                    hasLaunchedWithCustomIntent = true;
                    if (mainWebView != null) {
                        mainWebView.loadUrl(sharerUrl);
                    }
                    return;
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "Error encoding shared content", e);
                }
            }
        }

        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            hasLaunchedWithCustomIntent = true;
            loadUrlSafelyInApp(data.toString());
            return;
        }

        if (intent.hasExtra("notification_url")) {
            String url = intent.getStringExtra("notification_url");
            if (!TextUtils.isEmpty(url)) {
                hasLaunchedWithCustomIntent = true;
                loadUrlSafelyInApp(url);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (isDndModeActive) {
            disableDndMode(false);
        }

        stopNetworkMonitoring();

        if (onBackPressedCallback != null) {
            onBackPressedCallback.remove();
            onBackPressedCallback = null;
        }

        gestureDetector = null;

        if (mainProgressBarManager != null) {
            mainProgressBarManager.hide();
            mainProgressBarManager = null;
        }
        if (secondaryProgressBarManager != null) {
            secondaryProgressBarManager.hide();
            secondaryProgressBarManager = null;
        }

        if (Config.CLEAR_CACHE_ON_EXIT) {
            if (mainWebView != null) {
                mainWebView.clearCache(true);
            }
            if (secondaryWebView != null) {
                secondaryWebView.clearCache(true);
            }
        }

        if (Config.CLEAR_COOKIES_ON_EXIT) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookies(null);
            cookieManager.flush();
        }

        if (mainWebView != null) {
            mainWebView.stopLoading();
            mainWebView.destroy();
            mainWebView = null;
        }

        if (secondaryWebView != null) {
            secondaryWebView.stopLoading();
            secondaryWebView.destroy();
            secondaryWebView = null;
        }

        super.onDestroy();
    }

    /**
     * JavaScript bridge exposed only to trusted origins checked on the UI thread.
     */
    private class AndroidInterface {

        @JavascriptInterface
        public void openFormInSecondaryWebView(String action, String postData) {
            MainActivity.this.runOnUiThread(() -> {
                if (mainWebView == null || !isUrlFromAllowedDomains(
                        mainWebView.getUrl(), Config.SPECIAL_URL_ALLOWED_DOMAINS)) {
                    Log.w(TAG, "SECURITY: Blocked JS bridge openFormInSecondaryWebView from unauthorized origin");
                    return;
                }

                if (TextUtils.isEmpty(action)) {
                    return;
                }

                String trimmedAction = action.trim();
                boolean isHttps = trimmedAction.startsWith("https://");
                boolean isHttp = trimmedAction.startsWith("http://");
                if (!isHttps && (!isHttp || Config.LOAD_ONLY_HTTPS)) {
                    Log.w(TAG, "SECURITY: Blocked invalid action scheme in openFormInSecondaryWebView");
                    return;
                }

                checkHTTPSWarningAsync(trimmedAction, proceed -> {
                    if (!proceed) {
                        return;
                    }

                    ensureSecondaryWebViewInitialized();
                    if (secondaryContainer != null) {
                        secondaryContainer.setVisibility(View.VISIBLE);
                    }
                    updateSecondaryUrlDisplay(trimmedAction);

                    byte[] payload = postData != null
                            ? postData.getBytes(StandardCharsets.UTF_8)
                            : new byte[0];
                    secondaryWebView.postUrl(trimmedAction, payload);
                });
            });
        }

        @JavascriptInterface
        public void setKeepScreenOn(boolean keepOn) {
            MainActivity.this.runOnUiThread(() -> {
                if (mainWebView == null || !isUrlFromAllowedDomains(
                        mainWebView.getUrl(), Config.SPECIAL_URL_ALLOWED_DOMAINS)) {
                    Log.w(TAG, "SECURITY: Blocked JS bridge setKeepScreenOn from unauthorized origin");
                    return;
                }

                if (keepOn) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            });
        }
    }
}
