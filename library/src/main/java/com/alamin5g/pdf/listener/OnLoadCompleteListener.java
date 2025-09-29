package com.alamin5g.pdf.listener;

/**
 * Listener for PDF loading completion events
 */
public interface OnLoadCompleteListener {
    /**
     * Called when PDF loading is complete
     * @param nbPages Total number of pages in the PDF
     */
    void loadComplete(int nbPages);
}
