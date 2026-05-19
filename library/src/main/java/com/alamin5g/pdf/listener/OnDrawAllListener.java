package com.alamin5g.pdf.listener;

import android.graphics.Canvas;

/**
 * Listener for draw events before all pages are rendered.
 * Allows custom drawing on the entire PDF view.
 */
public interface OnDrawAllListener {
    /**
     * Called before all pages are drawn on the canvas.
     * @param canvas The canvas to draw on
     * @param width The width of the view
     * @param height The height of the view
     */
    void onLayerDrawn(Canvas canvas, float width, float height);
}
