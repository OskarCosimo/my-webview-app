package com.my.webviewapplication.mobile;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Manager class for handling various progress bar styles during WebView page loading.
 * Supports center spinner, corner spinner, and top linear progress bar.
 */
public class ProgressBarManager {

    private static final String TAG = "ProgressBarManager";

    public enum ProgressBarStyle {
        SPINNER_CENTER,
        SPINNER_CORNER,
        LINEAR_PROGRESS
    }

    private final Context context;
    @Nullable
    private final FrameLayout parentContainer;
    @Nullable
    private View progressView;
    @Nullable
    private ProgressBar progressBar;
    private ProgressBarStyle style;
    @ColorInt
    private int progressColor;

    /**
     * Constructor for ProgressBarManager.
     *
     * @param context         Activity or View context
     * @param parentContainer Parent FrameLayout where progress bar will be added
     * @param style           Visual style of the progress bar
     * @param progressColor   ARGB color integer for the progress indicator
     */
    public ProgressBarManager(@NonNull Context context,
                              @Nullable FrameLayout parentContainer,
                              @NonNull ProgressBarStyle style,
                              @ColorInt int progressColor) {
        this.context = context;
        this.parentContainer = parentContainer;
        this.style = style;
        this.progressColor = progressColor;
        Log.d(TAG, "ProgressBarManager initialized with style: " + style.name());
    }

    /**
     * Show the progress indicator if it is not already visible.
     */
    public void show() {
        if (parentContainer == null) {
            return;
        }
        if (progressView != null) {
            return;
        }

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

        if (progressView != null) {
            parentContainer.addView(progressView);
        }
    }

    /**
     * Hide and detach the progress indicator safely.
     */
    public void hide() {
        if (progressView != null) {
            if (progressView.getParent() instanceof ViewGroup) {
                ((ViewGroup) progressView.getParent()).removeView(progressView);
            } else if (parentContainer != null) {
                parentContainer.removeView(progressView);
            }
            progressView = null;
            progressBar = null;
        }
    }

    /**
     * Check if progress bar is currently visible.
     *
     * @return true if progress bar is attached and visible, false otherwise
     */
    public boolean isShowing() {
        return progressView != null && progressView.getVisibility() == View.VISIBLE;
    }

    private void createSpinnerCenter() {
        FrameLayout overlay = new FrameLayout(context);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        overlay.setBackgroundColor(Color.parseColor("#80000000"));

        progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        applyTint(progressBar, progressColor);

        float scale = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (72 * scale);

        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                sizePx,
                sizePx,
                Gravity.CENTER
        );

        progressBar.setLayoutParams(progressParams);
        overlay.addView(progressBar);
        progressView = overlay;
    }

    private void createSpinnerCorner() {
        FrameLayout container = new FrameLayout(context);
        container.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));

        progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        applyTint(progressBar, progressColor);

        float scale = context.getResources().getDisplayMetrics().density;
        int cornerSizePx = (int) (44 * scale);
        int paddingPx = (int) (16 * scale);

        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                cornerSizePx,
                cornerSizePx,
                Gravity.TOP | Gravity.END
        );
        progressParams.setMarginEnd(paddingPx);
        progressParams.topMargin = paddingPx;

        progressBar.setLayoutParams(progressParams);
        container.addView(progressBar);
        progressView = container;
    }

    private void createLinearProgress() {
        float scale = context.getResources().getDisplayMetrics().density;
        int barHeightPx = Math.max(1, (int) (4 * scale));

        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                barHeightPx,
                Gravity.TOP
        );
        container.setLayoutParams(containerParams);

        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        applyTint(progressBar, progressColor);

        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                barHeightPx
        );
        progressBar.setLayoutParams(progressParams);
        container.addView(progressBar);
        progressView = container;
    }

    private void applyTint(@NonNull ProgressBar bar, @ColorInt int color) {
        ColorStateList tint = ColorStateList.valueOf(color);
        bar.setIndeterminateTintList(tint);
        bar.setProgressTintList(tint);
    }

    /**
     * Update the progress bar style.
     *
     * @param newStyle The new style to apply
     */
    public void setStyle(@NonNull ProgressBarStyle newStyle) {
        if (this.style != newStyle) {
            boolean wasShowing = isShowing();
            hide();
            this.style = newStyle;
            if (wasShowing) {
                show();
            }
        }
    }

    /**
     * Update the progress bar color.
     *
     * @param newColor ARGB color integer
     */
    public void setColor(@ColorInt int newColor) {
        this.progressColor = newColor;
        if (progressBar != null) {
            applyTint(progressBar, newColor);
        }
    }
}
