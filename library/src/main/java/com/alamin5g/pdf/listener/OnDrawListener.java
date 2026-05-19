package com.alamin5g.pdf.listener;

import android.graphics.Canvas;

/**
 * Listener for draw events before each page is rendered.
 * Allows custom drawing on top of PDF pages.
 */
public interface OnDrawListener {
    /**
     * Called before each page is drawn on the canvas.
     * @param canvas The canvas to draw on
     * @param page The page number (0-based)
     * @param width The width of the page
     * @param height The height of the page
     */
    void onLayerDrawn(Canvas canvas, int page, float width, float height);
}
