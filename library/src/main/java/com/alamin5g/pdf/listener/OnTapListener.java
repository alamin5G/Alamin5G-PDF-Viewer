package com.alamin5g.pdf.listener;

import android.view.MotionEvent;

/**
 * Listener for tap events on the PDF view.
 * Allows handling single taps before the default behavior.
 */
public interface OnTapListener {
    /**
     * Called when the user taps on the PDF view.
     * @param e The motion event
     * @return true to consume the tap (prevent default behavior), false to allow default
     */
    boolean onTap(MotionEvent e);
}
