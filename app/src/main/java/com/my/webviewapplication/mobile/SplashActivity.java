package com.my.webviewapplication.mobile;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import pl.droidsonroids.gif.GifImageView;

/**
 * Splash Screen Activity.
 * Displays a video or animated GIF on app startup for Config.SPLASH_TIMEOUT_MS.
 */
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    private Handler splashHandler;
    private VideoView videoView;
    private boolean hasNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Config.ENABLE_SPLASH_SCREEN) {
            goToMainActivity();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        configureStatusBar();
        applyBackgroundColor();

        if (!setupSplashMedia()) {
            goToMainActivity();
            return;
        }

        splashHandler = new Handler(Looper.getMainLooper());
        long delayMs = Config.SPLASH_WAIT_FOR_WEBVIEW
                ? Math.min(Config.SPLASH_TIMEOUT_MS, 1500L)
                : Math.max(0L, Config.SPLASH_TIMEOUT_MS);

        scheduleMainActivityTransition(delayMs);
    }

    private void configureStatusBar() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(Config.DARK_STATUS_BAR_TEXT);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            try {
                //noinspection deprecation
                getWindow().setStatusBarColor(Color.parseColor(Config.STATUS_BAR_COLOR));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid color format in Config.STATUS_BAR_COLOR", e);
            }
        }
    }

    private void applyBackgroundColor() {
        View rootLayout = findViewById(R.id.splash_root_layout);
        if (rootLayout != null) {
            try {
                rootLayout.setBackgroundColor(Color.parseColor(Config.SPLASH_BG_COLOR));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid color format in Config.SPLASH_BG_COLOR", e);
            }
        }
    }

    private boolean setupSplashMedia() {
        GifImageView gifImageView = findViewById(R.id.splash_gif);
        videoView = findViewById(R.id.splash_video);

        if ("video".equalsIgnoreCase(Config.SPLASH_MEDIA_TYPE)) {
            if (gifImageView != null) {
                gifImageView.setVisibility(View.GONE);
            }
            if (videoView == null) {
                return false;
            }

            int videoResId = getResources().getIdentifier(
                    Config.SPLASH_VIDEO_NAME, "raw", getPackageName());
            if (videoResId == 0) {
                Log.e(TAG, "Video resource not found: " + Config.SPLASH_VIDEO_NAME);
                return false;
            }

            videoView.setVisibility(View.VISIBLE);
            videoView.setZOrderMediaOverlay(true);

            String videoPath = "android.resource://" + getPackageName() + "/" + videoResId;
            videoView.setVideoURI(Uri.parse(videoPath));

            videoView.setOnPreparedListener(mp -> {
                mp.setLooping(Config.SPLASH_VIDEO_LOOP);
                Log.d(TAG, "Splash video prepared");
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Splash video playback error (what=" + what + ", extra=" + extra + ")");
                goToMainActivity();
                return true;
            });

            videoView.start();
            return true;
        } else {
            if (videoView != null) {
                videoView.setVisibility(View.GONE);
            }
            if (gifImageView == null) {
                return false;
            }

            int gifResId = getResources().getIdentifier(
                    Config.SPLASH_GIF_NAME, "drawable", getPackageName());
            if (gifResId == 0) {
                Log.e(TAG, "GIF resource not found: " + Config.SPLASH_GIF_NAME);
                return false;
            }

            gifImageView.setVisibility(View.VISIBLE);
            gifImageView.setImageResource(gifResId);
            return true;
        }
    }

    private void scheduleMainActivityTransition(long delayMs) {
        if (delayMs <= 0L) {
            goToMainActivity();
            return;
        }
        splashHandler.postDelayed(this::goToMainActivity, delayMs);
    }

    private void goToMainActivity() {
        if (hasNavigated) {
            return;
        }
        hasNavigated = true;

        if (splashHandler != null) {
            splashHandler.removeCallbacksAndMessages(null);
        }

        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                    OVERRIDE_TRANSITION_OPEN,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
            );
        } else {
            //noinspection deprecation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Override
    protected void onDestroy() {
        if (splashHandler != null) {
            splashHandler.removeCallbacksAndMessages(null);
            splashHandler = null;
        }
        if (videoView != null) {
            videoView.stopPlayback();
            videoView = null;
        }
        super.onDestroy();
    }
}
