package com.my.webviewapplication.mobile;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.graphics.Color;
import android.util.Log;

/**
 * Manager class for handling various progress bar styles during WebView page loading
 * Supports multiple visual styles: spinning circle center, corner spinner, linear progress bar
 */
public class ProgressBarManager {

    private static final String TAG = "ProgressBarManager";

    // Progress bar style constants
    public enum ProgressBarStyle {
        SPINNER_CENTER,        // Spinning circle in center of screen
        SPINNER_CORNER,        // Spinning circle in top-right corner
        LINEAR_PROGRESS        // Linear progress bar at top of screen
    }

    private Context context;
    private FrameLayout parentContainer;
    private View progressView;
    private ProgressBar progressBar;
    private ProgressBarStyle style;
    private int colorRes;

    /**
     * Constructor for ProgressBarManager
     * @param context Application context
     * @param parentContainer Parent FrameLayout where progress bar will be added
     * @param style The visual style of the progress bar
     * @param colorRes Color resource for the progress bar
     */
    public ProgressBarManager(Context context, FrameLayout parentContainer,
                              ProgressBarStyle style, int colorRes) {
        this.context = context;
        this.parentContainer = parentContainer;
        this.style = style;
        this.colorRes = colorRes;
        Log.d(TAG, "ProgressBarManager initialized with style: " + style.name());
    }

    /**
     * Show the progress indicator
     * Creates and displays the appropriate progress bar based on configured style
     */
    public void show() {
        if (progressView != null) {
            Log.w(TAG, "Progress bar already visible");
            return;
        }

        Log.d(TAG, "Showing progress bar - Style: " + style.name());

        switch (style) {
            case SPINNER_CENTER:
                createSpinnerCenter();
                break;
            case SPINNER_CORNER:
                createSpinnerCorner();
                break;
            case LINEAR_PROGRESS:
                createLinearProgress();
                break;
        }

        if (progressView != null && parentContainer != null) {
            parentContainer.addView(progressView);
            Log.d(TAG, "Progress bar added to parent container");
        }
    }

    /**
     * Hide the progress indicator
     * Removes the progress bar from view
     */
    public void hide() {
        if (progressView != null && parentContainer != null) {
            Log.d(TAG, "Hiding progress bar - Style: " + style.name());
            parentContainer.removeView(progressView);
            progressView = null;
            progressBar = null;
        }
    }

    /**
     * Check if progress bar is currently visible
     * @return true if progress bar is visible, false otherwise
     */
    public boolean isShowing() {
        return progressView != null && progressView.getVisibility() == View.VISIBLE;
    }

    /**
     * Create spinning circle progress bar in center of screen
     * Displays a rotating circle in the middle of the page
     */
    private void createSpinnerCenter() {
        progressView = new FrameLayout(context);
        progressView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Semi-transparent background to darken the page
        progressView.setBackgroundColor(Color.parseColor("#80000000")); // 50% transparent black

        // Create ProgressBar in center
        progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);

        // Set color
        progressBar.getIndeterminateDrawable().setColorFilter(
                colorRes,
                android.graphics.PorterDuff.Mode.SRC_IN
        );

        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                100, // Width in dp converted to pixels
                100, // Height in dp converted to pixels,
                android.view.Gravity.CENTER
        );

        // Convert dp to pixels
        float scale = context.getResources().getDisplayMetrics().density;
        progressParams.width = (int) (100 * scale);
        progressParams.height = (int) (100 * scale);

        progressBar.setLayoutParams(progressParams);
        ((FrameLayout) progressView).addView(progressBar);

        Log.d(TAG, "Spinner center created");
    }

    /**
     * Create spinning circle progress bar in top-right corner
     * Displays a smaller rotating circle in the corner of the page
     */
    private void createSpinnerCorner() {
        progressView = new FrameLayout(context);
        progressView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));

        // Create ProgressBar in corner
        progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);

        // Set color
        progressBar.getIndeterminateDrawable().setColorFilter(
                colorRes,
                android.graphics.PorterDuff.Mode.SRC_IN
        );

        float scale = context.getResources().getDisplayMetrics().density;
        int cornerSize = (int) (50 * scale); // 50dp size

        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                cornerSize,
                cornerSize
        );

        // Position in top-right corner with 16dp padding
        int padding = (int) (16 * scale);
        progressParams.rightMargin = padding;
        progressParams.topMargin = padding;
        progressParams.gravity = android.view.Gravity.TOP | android.view.Gravity.RIGHT;

        progressBar.setLayoutParams(progressParams);
        ((FrameLayout) progressView).addView(progressBar);

        Log.d(TAG, "Spinner corner created");
    }

    /**
     * Create linear progress bar at top of screen
     * Displays a horizontal progress bar at the top of the page
     */
    private void createLinearProgress() {
        progressView = new FrameLayout(context);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );

        float scale = context.getResources().getDisplayMetrics().density;
        int barHeight = (int) (4 * scale); // 4dp height

        containerParams.height = barHeight;
        containerParams.gravity = android.view.Gravity.TOP;
        progressView.setLayoutParams(containerParams);

        // Create linear ProgressBar
        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);

        // Set color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            progressBar.getIndeterminateDrawable().setColorFilter(
                    colorRes,
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
        } else {
            progressBar.getProgressDrawable().setColorFilter(
                    colorRes,
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
        }

        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                barHeight
        );

        progressBar.setLayoutParams(progressParams);
        ((FrameLayout) progressView).addView(progressBar);

        Log.d(TAG, "Linear progress bar created");
    }

    /**
     * Update the progress bar style
     * @param newStyle The new style to apply
     */
    public void setStyle(ProgressBarStyle newStyle) {
        if (this.style != newStyle) {
            Log.d(TAG, "Changing progress bar style from " + this.style.name() + " to " + newStyle.name());
            boolean wasShowing = isShowing();
            hide();
            this.style = newStyle;
            if (wasShowing) {
                show();
            }
        }
    }

    /**
     * Update the progress bar color
     * @param newColor The new color resource
     */
    public void setColor(int newColor) {
        this.colorRes = newColor;
        if (progressBar != null) {
            Log.d(TAG, "Updating progress bar color");
            if (progressBar.getIndeterminateDrawable() != null) {
                progressBar.getIndeterminateDrawable().setColorFilter(
                        newColor,
                        android.graphics.PorterDuff.Mode.SRC_IN
                );
            }
            if (progressBar.getProgressDrawable() != null) {
                progressBar.getProgressDrawable().setColorFilter(
                        newColor,
                        android.graphics.PorterDuff.Mode.SRC_IN
                );
            }
        }
    }
}