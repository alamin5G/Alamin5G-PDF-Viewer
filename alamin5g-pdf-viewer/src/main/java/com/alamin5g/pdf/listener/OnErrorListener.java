package com.alamin5g.pdf.listener;

/**
 * Listener for error events
 */
public interface OnErrorListener {
    /**
     * Called when an error occurs
     * @param t The throwable that caused the error
     */
    void onError(Throwable t);
}
