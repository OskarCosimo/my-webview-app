package com.my.webviewapplication.mobile;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Custom WebView that tracks vertical scroll position.
 * Allows listeners to know whether the WebView is scrolled to the top.
 */
public class CustomWebView extends WebView {

    /**
     * Callback interface for scroll state changes.
     */
    public interface OnScrollChangeListener {
        void onScrollChanged(int scrollY, boolean isAtTop);
    }

    private static final int SCROLL_BUFFER_PX = 5;

    @Nullable
    private OnScrollChangeListener scrollChangeListener;

    public CustomWebView(@NonNull Context context) {
        super(context);
    }

    public CustomWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Set listener for scroll changes.
     *
     * @param listener Callback invoked when scroll position changes
     */
    public void setOnScrollChangeListener(@Nullable OnScrollChangeListener listener) {
        this.scrollChangeListener = listener;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        boolean isAtTop = t <= SCROLL_BUFFER_PX;
        if (scrollChangeListener != null) {
            scrollChangeListener.onScrollChanged(t, isAtTop);
        }
    }

    /**
     * Check if WebView is currently scrolled to the top.
     *
     * @return true if scrollY is within top buffer, false otherwise
     */
    public boolean isAtTop() {
        return getScrollY() <= SCROLL_BUFFER_PX;
    }
}
