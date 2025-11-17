package com.my.webviewapplication.mobile;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * Custom WebView that tracks scroll position
 * Allows external classes to check if WebView is scrolled to the top
 */
public class CustomWebView extends WebView {

    // Callback interface for scroll changes
    public interface OnScrollChangeListener {
        void onScrollChanged(int scrollY, boolean isAtTop);
    }

    private OnScrollChangeListener scrollChangeListener;
    // Small buffer to account for minor scroll position variations
    private static final int SCROLL_BUFFER = 5;

    public CustomWebView(Context context) {
        super(context);
    }

    public CustomWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Set listener for scroll changes
     * @param listener Callback when scroll position changes
     */
    public void setOnScrollChangeListener(OnScrollChangeListener listener) {
        this.scrollChangeListener = listener;
    }

    /**
     * Called when WebView scrolls
     * Notifies listener about scroll position changes
     */
    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        // Check if we're at the top of the page
        boolean isAtTop = t <= SCROLL_BUFFER;

        // Log for debugging
        android.util.Log.d("CustomWebView", "ScrollY: " + t + " | IsAtTop: " + isAtTop);

        // Notify listener
        if (scrollChangeListener != null) {
            scrollChangeListener.onScrollChanged(t, isAtTop);
        }
    }

    /**
     * Check if WebView is currently at the top
     * @return true if scrollY <= buffer, false otherwise
     */
    public boolean isAtTop() {
        return getScrollY() <= SCROLL_BUFFER;
    }
}