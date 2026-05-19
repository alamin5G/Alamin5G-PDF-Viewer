package com.alamin5g.pdf.listener;

/**
 * Listener for page render events.
 * Called when a page has been rendered to bitmap.
 */
public interface OnRenderListener {
    /**
     * Called when a page has finished rendering.
     * @param page The page number that was rendered (0-based)
     * @param width The rendered width
     * @param height The rendered height
     */
    void onPageRendered(int page, float width, float height);
}
