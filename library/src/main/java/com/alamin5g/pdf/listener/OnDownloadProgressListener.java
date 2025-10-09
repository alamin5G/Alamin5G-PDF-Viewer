package com.alamin5g.pdf.listener;

/**
 * Listener for PDF download progress from remote URLs
 */
public interface OnDownloadProgressListener {
    /**
     * Called when download progress is updated
     * @param bytesDownloaded Number of bytes downloaded so far
     * @param totalBytes Total number of bytes to download (-1 if unknown)
     * @param progress Progress percentage (0-100, -1 if unknown)
     */
    void onDownloadProgress(long bytesDownloaded, long totalBytes, int progress);
}
