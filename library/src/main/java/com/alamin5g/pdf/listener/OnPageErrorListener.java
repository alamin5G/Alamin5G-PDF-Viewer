package com.alamin5g.pdf.listener;

/**
 * Listener for page-specific error events.
 * Called when an error occurs while rendering a specific page.
 */
public interface OnPageErrorListener {
    /**
     * Called when an error occurs while rendering a page.
     * @param page The page number that failed (0-based)
     * @param t The throwable that caused the error
     */
    void onPageError(int page, Throwable t);
}
