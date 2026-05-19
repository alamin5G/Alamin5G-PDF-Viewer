package com.alamin5g.pdf.listener;

/**
 * Listener for page scroll events.
 * Called when the user scrolls through the PDF.
 */
public interface OnPageScrollListener {
    /**
     * Called when the current page or scroll position changes.
     * @param page Current page number (0-based)
     * @param positionOffset Scroll position offset (0.0 to 1.0)
     */
    void onPageScrolled(int page, float positionOffset);
}
