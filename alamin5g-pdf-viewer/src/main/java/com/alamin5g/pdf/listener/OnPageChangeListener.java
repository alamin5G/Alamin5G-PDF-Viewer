package com.alamin5g.pdf.listener;

/**
 * Listener for page change events
 */
public interface OnPageChangeListener {
    /**
     * Called when the current page changes
     * @param page Current page number (0-based)
     * @param pageCount Total number of pages
     */
    void onPageChanged(int page, int pageCount);
}
