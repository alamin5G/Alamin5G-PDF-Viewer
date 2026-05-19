package com.alamin5g.pdf.listener;

import android.view.MotionEvent;

/**
 * Listener for long press events on the PDF view.
 */
public interface OnLongPressListener {
    /**
     * Called when the user performs a long press on the PDF view.
     * @param e The motion event
     */
    void onLongPress(MotionEvent e);
}
