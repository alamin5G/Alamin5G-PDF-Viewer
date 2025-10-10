package com.alamin5g.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.alamin5g.pdf.listener.OnLoadCompleteListener;
import com.alamin5g.pdf.listener.OnPageChangeListener;
import com.alamin5g.pdf.listener.OnErrorListener;
import com.alamin5g.pdf.listener.OnDownloadProgressListener;

/**
 * Complete PDF View using Android's native PdfRenderer for 16KB compatibility
 * Based on AndroidPdfViewer library features
 */
public class PDFView extends FrameLayout {
    
    private static final String TAG = "PDFView";
    
    // PDF rendering
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor fileDescriptor;
    private int currentPage = 0;
    private int totalPages = 0;
    
    // Configuration
    private boolean enableSwipe = true;
    private boolean swipeHorizontal = false;
    private boolean enableDoubletap = true;
    private boolean enableAntialiasing = true;
    private boolean nightMode = false;
    private boolean useBestQuality = true;
    private int spacing = 0;
    private int defaultPage = 0;
    private int[] pages;
    private FitPolicy fitPolicy = FitPolicy.WIDTH;
    
    // Additional configuration options
    private boolean enableAnnotationRendering = true;
    private View scrollHandle = null;
    private boolean autoSpacing = false; // add dynamic spacing to fit each page
    private FitPolicy pageFitPolicy = FitPolicy.WIDTH; // mode to fit pages in the view
    private boolean fitEachPage = false; // fit each page to the view
    
    // Zoom and pan
    private Matrix matrix;
    private float scaleFactor = 1.0f;
    private float minZoom = 1.0f;
    private float midZoom = 1.75f;
    private float maxZoom = 3.0f;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private float lastTouchX, lastTouchY;
    private boolean isDragging = false;
    
    // Fit policies
    public enum FitPolicy {
        WIDTH, HEIGHT, BOTH
    }
    
    // Rendering
    private ExecutorService executorService;
    private Bitmap currentBitmap;
    private Paint paint;
    private ColorMatrix colorMatrix;
    private ColorMatrixColorFilter colorFilter;
    
    // Caching
    private android.util.LruCache<Integer, Bitmap> pageCache;
    private static final int DEFAULT_CACHE_SIZE = 10; // Default cache size
    private int cacheSize = DEFAULT_CACHE_SIZE; // Configurable cache size
    
    // Listeners
    private OnLoadCompleteListener onLoadCompleteListener;
    private OnPageChangeListener onPageChangeListener;
    private OnErrorListener onErrorListener;
    private OnDownloadProgressListener onDownloadProgressListener;
    
    // Page rendering (pages variable already declared above)
    
    public PDFView(Context context) {
        super(context);
        init();
    }
    
    public PDFView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public PDFView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        setWillNotDraw(false);
        
        // Enable hardware acceleration
        setLayerType(LAYER_TYPE_HARDWARE, null);
        
        // Initialize rendering components
        matrix = new Matrix();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        colorMatrix = new ColorMatrix();
        colorFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorFilter);
        
        // Initialize gesture detectors
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        gestureDetector = new GestureDetector(getContext(), new GestureListener());
        
        // Initialize thread pool for rendering
        executorService = Executors.newSingleThreadExecutor();
        
        // Initialize page cache
        pageCache = new android.util.LruCache<Integer, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(Integer key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024; // Size in KB
            }
            
            @Override
            protected void entryRemoved(boolean evicted, Integer key, Bitmap oldValue, Bitmap newValue) {
                // Only recycle if it's not the current bitmap being displayed
                if (evicted && oldValue != null && !oldValue.isRecycled() && oldValue != currentBitmap) {
                    Log.d(TAG, "Recycling cached bitmap for page: " + key);
                    oldValue.recycle();
                }
            }
        };
        
        // Enable touch events
        setOnTouchListener(new OnTouchListener() {
    @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);
                gestureDetector.onTouchEvent(event);
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastTouchX = event.getX();
                        lastTouchY = event.getY();
                        isDragging = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (scaleFactor > 1.0f) {
                            float deltaX = event.getX() - lastTouchX;
                            float deltaY = event.getY() - lastTouchY;
                            
                            matrix.postTranslate(deltaX, deltaY);
                            invalidate();
                            
                            lastTouchX = event.getX();
                            lastTouchY = event.getY();
                            isDragging = true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (!isDragging && scaleFactor <= 1.0f) {
                            // Single tap - could be used for other actions
                        }
                        break;
                }
            return true;
        }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        Log.d(TAG, "onDraw called - currentBitmap: " + (currentBitmap != null ? "exists" : "null") + 
                   ", isRecycled: " + (currentBitmap != null ? currentBitmap.isRecycled() : "N/A") +
                   ", canvas size: " + canvas.getWidth() + "x" + canvas.getHeight());
        
        // Critical fix: Check if bitmap is not null AND not recycled
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            try {
                canvas.save();
                canvas.concat(matrix);
                
                // Draw bitmap at origin (0,0) - matrix already includes translation and spacing
                Log.d(TAG, "Drawing bitmap with matrix transform, size: " + 
                           currentBitmap.getWidth() + "x" + currentBitmap.getHeight() +
                           ", scaleFactor: " + scaleFactor);
                
                canvas.drawBitmap(currentBitmap, 0, 0, paint);
                canvas.restore();
            } catch (Exception e) {
                Log.e(TAG, "Error drawing bitmap: " + e.getMessage(), e);
                // Clear the problematic bitmap
                if (currentBitmap != null && currentBitmap.isRecycled()) {
                    currentBitmap = null;
                }
                canvas.restore(); // Ensure canvas state is restored
            }
        } else {
            Log.w(TAG, "Cannot draw - bitmap is null or recycled");
        }
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "View size changed: " + w + "x" + h);
        
        // If we have a PDF loaded but no bitmap (due to previous zero dimensions), render now
        if (pdfRenderer != null && currentBitmap == null && w > 0 && h > 0) {
            Log.d(TAG, "View now has valid dimensions, rendering current page: " + currentPage);
            renderPage(currentPage);
        } else {
            updateMatrixScale();
            invalidate();
        }
    }
    
    private void updateMatrixScale() {
        if (currentBitmap == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        
        float viewWidth = getWidth() - spacing * 2;
        float viewHeight = getHeight() - spacing * 2;
        float bitmapWidth = currentBitmap.getWidth();
        float bitmapHeight = currentBitmap.getHeight();
        
        float scaleX = viewWidth / bitmapWidth;
        float scaleY = viewHeight / bitmapHeight;
        
        // Apply fit policy
        float scale;
        switch (fitPolicy) {
            case WIDTH:
                scale = scaleX;
                break;
            case HEIGHT:
                scale = scaleY;
                break;
            case BOTH:
            default:
                scale = Math.min(scaleX, scaleY);
                break;
        }
        
        // Apply current zoom level
        float finalScale = scale * scaleFactor;
        
        // Calculate center position for proper zoom anchoring
        float scaledWidth = bitmapWidth * finalScale;
        float scaledHeight = bitmapHeight * finalScale;
        
        float translateX = (viewWidth - scaledWidth) / 2f + spacing;
        float translateY = (viewHeight - scaledHeight) / 2f + spacing;
        
        // Reset matrix and apply transformations
        matrix.reset();
        matrix.setScale(finalScale, finalScale);
        matrix.postTranslate(translateX, translateY);
        
        Log.d(TAG, "Matrix updated - baseScale: " + scale + ", finalScale: " + finalScale + 
                   ", translate: (" + translateX + ", " + translateY + ")" +
                   ", viewSize: " + viewWidth + "x" + viewHeight + 
                   ", bitmapSize: " + bitmapWidth + "x" + bitmapHeight);
    }
    
    // Configuration methods
    public PDFView enableSwipe(boolean enableSwipe) {
        this.enableSwipe = enableSwipe;
        return this;
    }
    
    public PDFView swipeHorizontal(boolean swipeHorizontal) {
        this.swipeHorizontal = swipeHorizontal;
        return this;
    }
    
    public PDFView enableDoubletap(boolean enableDoubletap) {
        this.enableDoubletap = enableDoubletap;
        return this;
    }
    
    public PDFView enableAntialiasing(boolean enableAntialiasing) {
        this.enableAntialiasing = enableAntialiasing;
        return this;
    }
    
    public PDFView setNightMode(boolean nightMode) {
        this.nightMode = nightMode;
        updateColorFilter();
        invalidate();
        return this;
    }
    
    public PDFView useBestQuality(boolean useBestQuality) {
        this.useBestQuality = useBestQuality;
        return this;
    }
    
    public PDFView spacing(int spacing) {
        this.spacing = spacing;
        return this;
    }
    
    public PDFView setCacheSize(int cacheSize) {
        this.cacheSize = Math.max(1, cacheSize); // Ensure minimum cache size of 1
        // Reinitialize cache with new size
        if (pageCache != null) {
            pageCache.evictAll();
        }
        pageCache = new android.util.LruCache<Integer, Bitmap>(this.cacheSize) {
            @Override
            protected int sizeOf(Integer key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024; // Size in KB
            }

            @Override
            protected void entryRemoved(boolean evicted, Integer key, Bitmap oldValue, Bitmap newValue) {
                // Only recycle if it's not the current bitmap being displayed
                if (evicted && oldValue != null && !oldValue.isRecycled() && oldValue != currentBitmap) {
                    Log.d(TAG, "Recycling cached bitmap for page: " + key);
                    oldValue.recycle();
                }
            }
        };
        return this;
    }
    
    public PDFView defaultPage(int defaultPage) {
        this.defaultPage = defaultPage;
        return this;
    }
    
    public PDFView fitPolicy(FitPolicy fitPolicy) {
        this.fitPolicy = fitPolicy;
        return this;
    }
    
    public PDFView pages(int... pages) {
        this.pages = pages;
        return this;
    }
    
    // Additional configuration methods
    public PDFView enableAnnotationRendering(boolean enableAnnotationRendering) {
        this.enableAnnotationRendering = enableAnnotationRendering;
        return this;
    }
    
    public PDFView scrollHandle(View scrollHandle) {
        // Remove previous scroll handle if exists
        if (this.scrollHandle != null && this.scrollHandle.getParent() == this) {
            removeView(this.scrollHandle);
        }
        
        this.scrollHandle = scrollHandle;
        
        // Add new scroll handle if provided
        if (scrollHandle != null) {
            addView(scrollHandle);
        }
        
        return this;
    }
    
    public PDFView autoSpacing(boolean autoSpacing) {
        this.autoSpacing = autoSpacing;
        return this;
    }
    
    public PDFView pageFitPolicy(FitPolicy pageFitPolicy) {
        this.pageFitPolicy = pageFitPolicy;
        return this;
    }
    
    public PDFView fitEachPage(boolean fitEachPage) {
        this.fitEachPage = fitEachPage;
        return this;
    }
    
    // Listener methods
    public PDFView onLoad(OnLoadCompleteListener onLoadCompleteListener) {
        this.onLoadCompleteListener = onLoadCompleteListener;
        return this;
    }
    
    public PDFView onPageChange(OnPageChangeListener onPageChangeListener) {
        this.onPageChangeListener = onPageChangeListener;
        return this;
    }
    
    public PDFView onError(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
        return this;
    }
    
    public PDFView onDownloadProgress(OnDownloadProgressListener onDownloadProgressListener) {
        this.onDownloadProgressListener = onDownloadProgressListener;
        return this;
    }
    
    // Loading methods
    public PDFView fromAsset(String assetName) {
        try {
            Log.d(TAG, "Loading PDF from asset: " + assetName);
            
            // Read the asset as input stream and create a temporary file
            InputStream inputStream = getContext().getAssets().open(assetName);
            File tempFile = File.createTempFile("pdf_temp", ".pdf", getContext().getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            
            // Copy the input stream to the temporary file
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            
            inputStream.close();
            outputStream.close();
            
            Log.d(TAG, "Temporary file created: " + tempFile.getAbsolutePath());
            
            // Open the temporary file with PdfRenderer
            fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);
            
            totalPages = pdfRenderer.getPageCount();
            currentPage = defaultPage;
            
            // Adjust total pages if custom page order is specified
            if (pages != null) {
                totalPages = pages.length;
            }
            
            Log.d(TAG, "PDF loaded successfully with " + totalPages + " pages");
            
            if (onLoadCompleteListener != null) {
                onLoadCompleteListener.loadComplete(totalPages);
            }
            
            renderPage(currentPage);
            
        } catch (IOException e) {
            Log.e(TAG, "Error loading PDF from asset: " + e.getMessage());
        if (onErrorListener != null) {
                onErrorListener.onError(e);
            }
        }
        return this;
    }
    
    public PDFView fromFile(File file) {
        try {
            Log.d(TAG, "Loading PDF from file: " + file.getAbsolutePath());
            
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);
            
            totalPages = pdfRenderer.getPageCount();
            currentPage = defaultPage;
            
            // Adjust total pages if custom page order is specified
            if (pages != null) {
                totalPages = pages.length;
            }
            
            Log.d(TAG, "PDF loaded successfully with " + totalPages + " pages");
            
            if (onLoadCompleteListener != null) {
                onLoadCompleteListener.loadComplete(totalPages);
            }
            
            renderPage(currentPage);
            
        } catch (IOException e) {
            Log.e(TAG, "Error loading PDF from file: " + e.getMessage());
            if (onErrorListener != null) {
                onErrorListener.onError(e);
            }
        }
        return this;
    }
    
    public PDFView fromBytes(byte[] bytes) {
        try {
            Log.d(TAG, "Loading PDF from bytes: " + bytes.length + " bytes");
            
            // Create a temporary file from bytes
            File tempFile = File.createTempFile("pdf_temp", ".pdf", getContext().getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            outputStream.write(bytes);
            outputStream.close();
            
            return fromFile(tempFile);
            
        } catch (IOException e) {
            Log.e(TAG, "Error loading PDF from bytes: " + e.getMessage());
            if (onErrorListener != null) {
                onErrorListener.onError(e);
            }
        }
        return this;
    }
    
    public PDFView fromUri(android.net.Uri uri) {
        try {
            Log.d(TAG, "Loading PDF from URI: " + uri.toString());
            
            // Open input stream from URI
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                throw new IOException("Cannot open input stream from URI: " + uri);
            }
            
            return fromStream(inputStream);
            
        } catch (IOException e) {
            Log.e(TAG, "Error loading PDF from URI: " + e.getMessage());
            if (onErrorListener != null) {
                onErrorListener.onError(e);
            }
        }
        return this;
    }
    
    public PDFView fromStream(InputStream inputStream) {
        try {
            Log.d(TAG, "Loading PDF from InputStream");
            
            // Create a temporary file from input stream
            File tempFile = File.createTempFile("pdf_temp", ".pdf", getContext().getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            
            // Copy the input stream to the temporary file
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            
            inputStream.close();
            outputStream.close();
            
            return fromFile(tempFile);
            
        } catch (IOException e) {
            Log.e(TAG, "Error loading PDF from InputStream: " + e.getMessage());
            if (onErrorListener != null) {
                onErrorListener.onError(e);
            }
        }
        return this;
    }
    
    public PDFView fromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            if (onErrorListener != null) {
                onErrorListener.onError(new IllegalArgumentException("URL cannot be null or empty"));
            }
            return this;
        }
        
        // Download PDF from URL in background thread
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Downloading PDF from URL: " + url);
                
                URL pdfUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) pdfUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000); // 30 seconds
                connection.setReadTimeout(60000);    // 60 seconds
                
                // Set user agent to avoid blocking
                connection.setRequestProperty("User-Agent", "Alamin5G-PDF-Viewer/1.0.10");
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + responseCode);
                }
                
                // Get file size for progress tracking
                long totalBytes = connection.getContentLengthLong();
                Log.d(TAG, "PDF file size: " + totalBytes + " bytes");
                
                InputStream inputStream = connection.getInputStream();
                
                // Create temporary file
                File tempFile = File.createTempFile("pdf_download", ".pdf", getContext().getCacheDir());
                FileOutputStream outputStream = new FileOutputStream(tempFile);
                
                // Download with progress tracking
                byte[] buffer = new byte[8192];
                long bytesDownloaded = 0;
                int bytesRead;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    bytesDownloaded += bytesRead;
                    
                    // Report progress
                    if (onDownloadProgressListener != null) {
                        final long finalBytesDownloaded = bytesDownloaded;
                        final long finalTotalBytes = totalBytes;
                        final int progress = totalBytes > 0 ? (int) ((bytesDownloaded * 100) / totalBytes) : -1;
                        
                        post(() -> onDownloadProgressListener.onDownloadProgress(
                            finalBytesDownloaded, finalTotalBytes, progress));
                    }
                }
                
                inputStream.close();
                outputStream.close();
                connection.disconnect();
                
                Log.d(TAG, "PDF downloaded successfully: " + tempFile.getAbsolutePath());
                
                // Load the downloaded file on main thread
                post(() -> fromFile(tempFile).load());
                
            } catch (Exception e) {
                Log.e(TAG, "Error downloading PDF from URL: " + e.getMessage(), e);
                if (onErrorListener != null) {
                    post(() -> onErrorListener.onError(e));
                }
            }
        });
        
        return this;
    }
    
    public void load() {
        // Loading is handled in fromAsset(), fromFile(), etc.
    }
    
    // Navigation methods
    public void jumpTo(int page) {
        Log.d(TAG, "jumpTo called with page: " + page + ", totalPages: " + totalPages);
        if (page >= 0 && page < totalPages) {
            currentPage = page;
            Log.d(TAG, "Jumping to page: " + currentPage);
            renderPage(currentPage);
            resetZoom();
            
            if (onPageChangeListener != null) {
                onPageChangeListener.onPageChanged(currentPage, totalPages);
            }
        } else {
            Log.w(TAG, "Invalid page number: " + page + " (total: " + totalPages + ")");
        }
    }
    
    public void jumpTo(int page, boolean withAnimation) {
        if (withAnimation) {
            // Simple fade animation for page changes
            animate().alpha(0.5f).setDuration(150).withEndAction(() -> {
                jumpTo(page);
                animate().alpha(1.0f).setDuration(150).start();
            }).start();
        } else {
            jumpTo(page);
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageCount() {
        return totalPages;
    }
    
    // Zoom methods
    public void setMinZoom(float minZoom) {
        this.minZoom = minZoom;
    }

    public void setMidZoom(float midZoom) {
        this.midZoom = midZoom;
    }

    public void setMaxZoom(float maxZoom) {
        this.maxZoom = maxZoom;
    }

    public void zoomTo(float zoom) {
        scaleFactor = Math.max(minZoom, Math.min(maxZoom, zoom));
        updateMatrixScale();
        invalidate();
        Log.d(TAG, "Zoom set to: " + scaleFactor);
    }
    
    public void zoomWithAnimation(float zoom) {
        // Smooth zoom animation
        animate().scaleX(zoom / scaleFactor).scaleY(zoom / scaleFactor).setDuration(300).withEndAction(() -> {
            zoomTo(zoom);
            setScaleX(1.0f);
            setScaleY(1.0f);
        }).start();
    }
    
    public float getZoom() {
        return scaleFactor;
    }
    
    public void resetZoom() {
        scaleFactor = 1.0f;
        updateMatrixScale();
        invalidate();
        Log.d(TAG, "Zoom reset to: " + scaleFactor);
    }
    
    public void resetZoomWithAnimation() {
        // Smooth reset zoom animation
        animate().scaleX(1.0f / scaleFactor).scaleY(1.0f / scaleFactor).setDuration(300).withEndAction(() -> {
            resetZoom();
            setScaleX(1.0f);
            setScaleY(1.0f);
        }).start();
    }
    
    // Utility methods
    public void recycle() {
        // Safely recycle current bitmap
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
        }
        currentBitmap = null;
        
        // Clear and recycle cached bitmaps
        if (pageCache != null) {
            pageCache.evictAll();
            pageCache = null;
        }
        if (pdfRenderer != null) {
            pdfRenderer.close();
            pdfRenderer = null;
        }
        if (fileDescriptor != null) {
            try {
                fileDescriptor.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing file descriptor: " + e.getMessage());
            }
            fileDescriptor = null;
        }
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }
    
    private void renderPage(int pageIndex) {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= totalPages) {
            Log.e(TAG, "Cannot render page " + pageIndex + ": pdfRenderer=" + (pdfRenderer != null) + ", totalPages=" + totalPages);
            return;
        }
        
        // Check cache first
        Bitmap cachedBitmap = pageCache.get(pageIndex);
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            Log.d(TAG, "Using cached bitmap for page " + pageIndex);
            post(() -> {
                if (currentBitmap != null) {
                    currentBitmap.recycle();
                }
                currentBitmap = cachedBitmap;
                invalidate();
            });
            return;
        }
        
        executorService.execute(() -> {
            try {
                // Use custom page order if specified
                int actualPageIndex = pageIndex;
                if (pages != null && pageIndex < pages.length) {
                    actualPageIndex = pages[pageIndex];
                }
                
                Log.d(TAG, "Rendering page " + pageIndex + " (actual: " + actualPageIndex + ")");
                PdfRenderer.Page page = pdfRenderer.openPage(actualPageIndex);
                
                // Calculate bitmap size based on fit policy
                int viewWidth = getWidth();
                int viewHeight = getHeight();
                
                // Check if view has valid dimensions
                if (viewWidth <= 0 || viewHeight <= 0) {
                    Log.w(TAG, "View dimensions not ready: " + viewWidth + "x" + viewHeight + ", skipping render");
                    page.close(); // Close the page before returning
                    return;
                }
                
                int width, height;
                
                // Use pageFitPolicy if fitEachPage is enabled, otherwise use fitPolicy
                FitPolicy currentFitPolicy = fitEachPage ? pageFitPolicy : fitPolicy;
                
                switch (currentFitPolicy) {
                    case WIDTH:
                        width = viewWidth;
                        height = (int) (width * (float) page.getHeight() / page.getWidth());
                        break;
                    case HEIGHT:
                        height = viewHeight;
                        width = (int) (height * (float) page.getWidth() / page.getHeight());
                        break;
                    case BOTH:
                    default:
                        width = viewWidth;
                        height = viewHeight;
                        break;
                }
                
                // Apply spacing if autoSpacing is enabled
                if (autoSpacing) {
                    // Reduce size to accommodate spacing
                    width -= spacing * 2;
                    height -= spacing * 2;
                }
                
                // Ensure minimum dimensions
                width = Math.max(width, 1);
                height = Math.max(height, 1);
                
                Log.d(TAG, "Creating bitmap with dimensions: " + width + "x" + height + 
                      " (view: " + viewWidth + "x" + viewHeight + ")");
                
                // Create bitmap with appropriate quality
                Bitmap.Config config = useBestQuality ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
                Bitmap bitmap = Bitmap.createBitmap(width, height, config);
                
                // Render the page to the bitmap
                // Use RENDER_MODE_FOR_DISPLAY for annotations, RENDER_MODE_FOR_PRINT to exclude them
                int renderMode = enableAnnotationRendering ? 
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY : 
                    PdfRenderer.Page.RENDER_MODE_FOR_PRINT;
                page.render(bitmap, null, null, renderMode);
                
                // Close the page
                page.close();
                
                // Update UI on main thread
                post(() -> {
                    // Safely replace current bitmap
                    Bitmap oldBitmap = currentBitmap;
                    
                    // Set new bitmap BEFORE caching to prevent immediate recycling
                    currentBitmap = bitmap;
                    
                    // Update matrix scale to fit the view
                    updateMatrixScale();
                    
                    // Cache the bitmap AFTER setting as current
                    pageCache.put(pageIndex, bitmap);
                    
                    // Recycle old bitmap after setting new one (but not if it's the same)
                    if (oldBitmap != null && !oldBitmap.isRecycled() && oldBitmap != bitmap) {
                        // Check if old bitmap is still in cache before recycling
                        boolean inCache = false;
                        for (int i = 0; i < pageCache.size(); i++) {
                            if (pageCache.get(i) == oldBitmap) {
                                inCache = true;
                                break;
                            }
                        }
                        if (!inCache) {
                            Log.d(TAG, "Recycling old bitmap for page: " + pageIndex);
                            oldBitmap.recycle();
                        }
                    }
                    
                    invalidate();
                    Log.d(TAG, "Successfully rendered page: " + pageIndex);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error rendering page " + pageIndex + ": " + e.getMessage());
                if (onErrorListener != null) {
                    post(() -> onErrorListener.onError(e));
                }
            }
        });
    }
    
    private void updateColorFilter() {
        if (nightMode) {
            // Invert colors for night mode
            colorMatrix.set(new float[]{
                -1, 0, 0, 0, 255,
                0, -1, 0, 0, 255,
                0, 0, -1, 0, 255,
                0, 0, 0, 1, 0
            });
        } else {
            // Normal colors
            colorMatrix.reset();
        }
        colorFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorFilter);
    }
    
    // Gesture listener for zoom
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = detector.getScaleFactor();
            float newScaleFactor = scaleFactor * scale;

            // Clamp zoom level between min and max
            newScaleFactor = Math.max(minZoom, Math.min(newScaleFactor, maxZoom));

            Log.d(TAG, "Scale gesture: " + scale + ", newScale: " + newScaleFactor);

            // Only update if the scale actually changed
            if (newScaleFactor != scaleFactor) {
                scaleFactor = newScaleFactor;
                updateMatrixScale(); // Use updateMatrixScale for proper centering
                invalidate();
                Log.d(TAG, "Zoom applied: " + scaleFactor);
            }

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            // Optional: Add any cleanup or final adjustments here
        }
    }
    
    // Gesture listener for swipe navigation
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!enableSwipe) return false;
            
            Log.d(TAG, "Fling detected: velocityX=" + velocityX + ", velocityY=" + velocityY);
            
            if (swipeHorizontal) {
                // Horizontal swipe
                if (Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (velocityX > 0) {
                        // Swipe right - previous page
                        Log.d(TAG, "Swipe right detected, currentPage: " + currentPage);
                        if (currentPage > 0) {
                            jumpTo(currentPage - 1);
                            return true;
                        }
                    } else {
                        // Swipe left - next page
                        Log.d(TAG, "Swipe left detected, currentPage: " + currentPage);
                        if (currentPage < totalPages - 1) {
                            jumpTo(currentPage + 1);
                            return true;
                        }
                    }
                }
            } else {
                // Vertical swipe
                if (Math.abs(velocityY) > Math.abs(velocityX)) {
                    if (velocityY > 0) {
                        // Swipe down - previous page
                        Log.d(TAG, "Swipe down detected, currentPage: " + currentPage);
                        if (currentPage > 0) {
                            jumpTo(currentPage - 1);
                            return true;
                        }
                    } else {
                        // Swipe up - next page
                        Log.d(TAG, "Swipe up detected, currentPage: " + currentPage);
                        if (currentPage < totalPages - 1) {
                            jumpTo(currentPage + 1);
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (!enableDoubletap) return false;
            
            Log.d(TAG, "Double tap detected - toggling zoom");
            
            if (scaleFactor > minZoom) {
                resetZoom();
            } else {
                zoomTo(midZoom);
            }
            return true;
        }
    }
    
    // Getter methods for new configuration options
    public boolean isAnnotationRenderingEnabled() {
        return enableAnnotationRendering;
    }
    
    public View getScrollHandle() {
        return scrollHandle;
    }
    
    public int getSpacing() {
        return spacing;
    }
    
    public int getCacheSize() {
        return cacheSize;
    }
    
    public boolean isAutoSpacing() {
        return autoSpacing;
    }
    
    public FitPolicy getPageFitPolicy() {
        return pageFitPolicy;
    }
    
    public boolean isFitEachPage() {
        return fitEachPage;
    }
    
    // Additional utility methods
    public void setSpacing(int spacing) {
        this.spacing = spacing;
        invalidate(); // Redraw with new spacing
    }
    
    public void setAutoSpacing(boolean autoSpacing) {
        this.autoSpacing = autoSpacing;
        if (currentPage >= 0) {
            renderPage(currentPage); // Re-render current page
        }
    }
    
    public void setPageFitPolicy(FitPolicy pageFitPolicy) {
        this.pageFitPolicy = pageFitPolicy;
        if (fitEachPage && currentPage >= 0) {
            renderPage(currentPage); // Re-render current page
        }
    }
    
    public void setFitEachPage(boolean fitEachPage) {
        this.fitEachPage = fitEachPage;
        if (currentPage >= 0) {
            renderPage(currentPage); // Re-render current page
        }
    }
    
    public void setAnnotationRenderingEnabled(boolean enableAnnotationRendering) {
        this.enableAnnotationRendering = enableAnnotationRendering;
        if (currentPage >= 0) {
            renderPage(currentPage); // Re-render current page
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        recycle();
    }
}
