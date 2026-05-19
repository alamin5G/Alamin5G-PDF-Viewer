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
import android.widget.OverScroller;

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
import com.alamin5g.pdf.listener.OnDrawListener;
import com.alamin5g.pdf.listener.OnDrawAllListener;
import com.alamin5g.pdf.listener.OnPageScrollListener;
import com.alamin5g.pdf.listener.OnRenderListener;
import com.alamin5g.pdf.listener.OnTapListener;
import com.alamin5g.pdf.listener.OnLongPressListener;
import com.alamin5g.pdf.listener.OnPageErrorListener;

import com.alamin5g.pdf.scroll.DefaultScrollHandle;

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
    private int previousPage = -1; // v1.0.15: Track for page change detection

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
    private boolean continuousScrollMode = true; // Enable continuous scrolling by default
    private boolean pageFling = false; // v1.0.14: Page fling on swipe (false = smooth scroll)
    private boolean pageSnap = false; // v1.0.14: Snap to page boundary after scroll
    private boolean renderDuringScale = false; // v1.0.14: Render during pinch zoom
    private String pdfPassword = null; // v1.0.14: Password for encrypted PDFs

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
    private float panX = 0f;
    private float panY = 0f;
    private boolean zoomEnabled = true; // Issue #5: Enable/disable zoom gesture
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private float lastTouchX, lastTouchY;
    private boolean isDragging = false;

    // Smooth scrolling (v1.0.14)
    private OverScroller scroller;
    private boolean flinging = false;

    // Fit policies
    public enum FitPolicy {
        WIDTH, HEIGHT, BOTH
    }

    // Rendering
    private ExecutorService executorService;
    private Bitmap currentBitmap; // For single page mode
    private java.util.List<Bitmap> pageBitmaps = new java.util.ArrayList<>(); // For continuous mode
    private java.util.List<Float> pageOffsets = new java.util.ArrayList<>(); // Y positions of each page
    private Paint paint;
    private ColorMatrix colorMatrix;
    private ColorMatrixColorFilter colorFilter;
    private float totalContentHeight = 0f;

    // Caching
    private android.util.LruCache<Integer, Bitmap> pageCache;
    private static final int DEFAULT_CACHE_SIZE = 10; // Default cache size
    private int cacheSize = DEFAULT_CACHE_SIZE; // Configurable cache size

    // Lazy loading for continuous mode (v1.0.13 - Memory optimization)
    private static final int MAX_CACHED_PAGES = 12; // v1.0.15: Increased from 7 to 12 (~84 MB for smoother scrolling)
    private final java.util.Map<Integer, Bitmap> continuousPageCache = new java.util.LinkedHashMap<Integer, Bitmap>(
            MAX_CACHED_PAGES + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Integer, Bitmap> eldest) {
            if (size() > MAX_CACHED_PAGES) {
                if (eldest.getValue() != null && !eldest.getValue().isRecycled()) {
                    eldest.getValue().recycle();
                    Log.d(TAG, "Recycled page " + eldest.getKey() + " from cache");
                }
                return true;
            }
            return false;
        }
    };
    private int visibleStartPage = 0;
    private int visibleEndPage = 0;
    private float lastRenderedZoom = 1.0f; // Track zoom for quality re-rendering

    // Listeners
    private OnLoadCompleteListener onLoadCompleteListener;
    private OnPageChangeListener onPageChangeListener;
    private OnErrorListener onErrorListener;
    private OnDownloadProgressListener onDownloadProgressListener;
    // P1 Listeners (v1.0.17)
    private OnDrawListener onDrawListener;
    private OnDrawAllListener onDrawAllListener;
    private OnPageScrollListener onPageScrollListener;
    private OnRenderListener onRenderListener;
    private OnTapListener onTapListener;
    private OnLongPressListener onLongPressListener;
    private OnPageErrorListener onPageErrorListener;

    // v1.0.16: Source tracking for deferred loading (fix for issue #4)
    private enum SourceType {
        NONE, ASSET, FILE, URI, BYTES, STREAM, URL
    }

    private SourceType sourceType = SourceType.NONE;
    private String assetName;
    private File sourceFile;
    private android.net.Uri sourceUri;
    private byte[] sourceBytes;
    private InputStream sourceStream;
    private String sourceUrl;

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

        // Initialize smooth scrolling (v1.0.14)
        scroller = new OverScroller(getContext());

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
                // Don't intercept touch events when scroll handle is being dragged
                if (scrollHandle instanceof DefaultScrollHandle
                        && ((DefaultScrollHandle) scrollHandle).isDragging()) {
                    return false;
                }

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

        if (continuousScrollMode && !pageOffsets.isEmpty()) {
            // v1.0.13: Continuous scroll mode - draw only cached/visible pages (lazy
            // loading)
            Log.d(TAG, "onDraw - continuous mode, cache size: " + continuousPageCache.size() + ", panY: " + panY
                    + ", zoom: " + scaleFactor);

            canvas.save();

            // Apply pan offsets
            canvas.translate(panX, panY);

            // Draw only cached pages (lazy loaded)
            // v1.0.17: Per-page canvas.scale() to eliminate visual SIZE gaps during pinch zoom
            // Each bitmap is scaled from its rendered size to the expected visual size at current zoom
            float viewWidth = getWidth();
            for (int i = 0; i < pageOffsets.size(); i++) {
                Bitmap bitmap = continuousPageCache.get(i);

                if (bitmap != null && !bitmap.isRecycled()) {
                    // CRITICAL: Multiply BASE offset by scaleFactor (like AndroidPdfViewer)
                    // pageOffsets are stored at zoom=1.0, so we scale them dynamically
                    float baseOffset = pageOffsets.get(i);
                    float scaledOffset = baseOffset * scaleFactor;

                    // Approach B: Scale bitmap from rendered size to current zoom size
                    // This eliminates gaps regardless of what zoom the bitmap was rendered at
                    if (viewWidth > 0 && bitmap.getWidth() > 0) {
                        float expectedWidth = viewWidth * scaleFactor;
                        float bitmapScale = expectedWidth / bitmap.getWidth();

                        canvas.save();
                        canvas.translate(0, scaledOffset);
                        canvas.scale(bitmapScale, bitmapScale);
                        canvas.drawBitmap(bitmap, 0, 0, paint);
                        canvas.restore();
                    } else {
                        canvas.drawBitmap(bitmap, 0, scaledOffset, paint);
                    }

                    // P1: OnDrawListener - called after each page is drawn
                    if (onDrawListener != null) {
                        onDrawListener.onLayerDrawn(canvas, i, bitmap.getWidth(), bitmap.getHeight());
                    }
                }
            }

            canvas.restore();
        } else if (currentBitmap != null && !currentBitmap.isRecycled()) {
            // Single page mode
            Log.d(TAG,
                    "onDraw - single page mode, bitmap: " + currentBitmap.getWidth() + "x" + currentBitmap.getHeight());

            try {
                canvas.save();
                canvas.concat(matrix);

                // Draw bitmap at origin (0,0) - matrix already includes translation and spacing
                canvas.drawBitmap(currentBitmap, 0, 0, paint);

                // P1: OnDrawListener - called after page is drawn (single page mode)
                if (onDrawListener != null) {
                    onDrawListener.onLayerDrawn(canvas, currentPage, currentBitmap.getWidth(), currentBitmap.getHeight());
                }

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
            Log.w(TAG, "Cannot draw - no bitmap available");
        }

        // P1: OnDrawAllListener - called after all pages are drawn
        if (onDrawAllListener != null) {
            onDrawAllListener.onLayerDrawn(canvas, getWidth(), getHeight());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "View size changed: " + w + "x" + h);

        // v1.0.13: If we have a PDF loaded but no bitmap (due to previous zero
        // dimensions), render now
        if (pdfRenderer != null && w > 0 && h > 0) {
            if (continuousScrollMode && pageOffsets.isEmpty()) {
                Log.d(TAG, "View now has valid dimensions, initializing page offsets and rendering visible pages");
                initializePageOffsets();
                renderVisiblePages();
            } else if (!continuousScrollMode && currentBitmap == null) {
                Log.d(TAG, "View now has valid dimensions, rendering current page: " + currentPage);
                renderPage(currentPage);
            } else {
                updateMatrixScale();
                invalidate();
            }
        }
    }

    private void updateMatrixScale() {
        if (currentBitmap == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float bitmapWidth = currentBitmap.getWidth();
        float bitmapHeight = currentBitmap.getHeight();

        float scaleX = viewWidth / bitmapWidth;
        float scaleY = viewHeight / bitmapHeight;

        // Apply fit policy
        float baseScale;
        switch (fitPolicy) {
            case WIDTH:
                baseScale = scaleX;
                break;
            case HEIGHT:
                baseScale = scaleY;
                break;
            case BOTH:
            default:
                baseScale = Math.min(scaleX, scaleY);
                break;
        }

        // Apply current zoom level
        float finalScale = baseScale * scaleFactor;

        // Calculate scaled dimensions
        float scaledWidth = bitmapWidth * finalScale;
        float scaledHeight = bitmapHeight * finalScale;

        // Calculate center position including current pan offsets
        float translateX = (viewWidth - scaledWidth) / 2f + panX;
        float translateY = (viewHeight - scaledHeight) / 2f + panY;

        // Reset matrix and apply transformations
        matrix.reset();
        matrix.setScale(finalScale, finalScale);
        matrix.postTranslate(translateX, translateY);

        Log.d(TAG, "Matrix updated - baseScale: " + baseScale + ", finalScale: " + finalScale +
                ", translate: (" + translateX + ", " + translateY + ")" +
                ", pan: (" + panX + ", " + panY + ")" +
                ", viewSize: " + viewWidth + "x" + viewHeight +
                ", bitmapSize: " + bitmapWidth + "x" + bitmapHeight +
                ", scaledSize: " + scaledWidth + "x" + scaledHeight);
    }

    // Configuration methods
    public PDFView enableSwipe(boolean enableSwipe) {
        this.enableSwipe = enableSwipe;
        return this;
    }

    public PDFView continuousScroll(boolean continuousScroll) {
        this.continuousScrollMode = continuousScroll;
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

            // Issue #6: Wire DefaultScrollHandle into PDFView
            if (scrollHandle instanceof DefaultScrollHandle) {
                DefaultScrollHandle handle = (DefaultScrollHandle) scrollHandle;
                handle.setPDFView(this);
                handle.show();
            }
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

    // v1.0.14: Page fling and snap configuration
    public PDFView pageFling(boolean pageFling) {
        this.pageFling = pageFling;
        return this;
    }

    public PDFView pageSnap(boolean pageSnap) {
        this.pageSnap = pageSnap;
        return this;
    }

    // Issue #5: Enable/disable zoom gesture
    public PDFView enableZoom(boolean zoomEnabled) {
        this.zoomEnabled = zoomEnabled;
        return this;
    }

    public boolean isPageFlingEnabled() {
        return pageFling;
    }

    public boolean isPageSnapEnabled() {
        return pageSnap;
    }

    // v1.0.14: Additional getter methods for complete API coverage
    public boolean isBestQuality() {
        return useBestQuality;
    }

    public boolean isSwipeVertical() {
        return !swipeHorizontal;
    }

    public boolean isSwipeEnabled() {
        return enableSwipe;
    }

    public boolean isAnnotationRendering() {
        return enableAnnotationRendering;
    }

    public boolean isAntialiasing() {
        return enableAntialiasing;
    }

    public int getSpacingPx() {
        return spacing;
    }

    public boolean isAutoSpacingEnabled() {
        return autoSpacing;
    }

    public FitPolicy getPageFitPolicy() {
        return pageFitPolicy;
    }

    public boolean isFitEachPage() {
        return fitEachPage;
    }

    public void enableRenderDuringScale(boolean enable) {
        this.renderDuringScale = enable;
    }

    public boolean doRenderDuringScale() {
        return renderDuringScale;
    }

    // v1.0.14: Password support for encrypted PDFs
    public PDFView password(String password) {
        this.pdfPassword = password;
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

    // P1 Listener setters (v1.0.17)
    public PDFView onDraw(OnDrawListener onDrawListener) {
        this.onDrawListener = onDrawListener;
        return this;
    }

    public PDFView onDrawAll(OnDrawAllListener onDrawAllListener) {
        this.onDrawAllListener = onDrawAllListener;
        return this;
    }

    public PDFView onPageScroll(OnPageScrollListener onPageScrollListener) {
        this.onPageScrollListener = onPageScrollListener;
        return this;
    }

    public PDFView onRender(OnRenderListener onRenderListener) {
        this.onRenderListener = onRenderListener;
        return this;
    }

    public PDFView onTap(OnTapListener onTapListener) {
        this.onTapListener = onTapListener;
        return this;
    }

    public PDFView onLongPress(OnLongPressListener onLongPressListener) {
        this.onLongPressListener = onLongPressListener;
        return this;
    }

    public PDFView onPageError(OnPageErrorListener onPageErrorListener) {
        this.onPageErrorListener = onPageErrorListener;
        return this;
    }

    // Loading methods
    public PDFView fromAsset(String assetName) {
        Log.d(TAG, "fromAsset() called - Asset: " + assetName);
        Log.e(TAG, "==========================================");
        Log.e(TAG, "fromAsset() - Storing asset name for deferred loading");
        Log.e(TAG, "==========================================");

        this.sourceType = SourceType.ASSET;
        this.assetName = assetName;

        return this;
    }

    public PDFView fromFile(File file) {
        Log.d(TAG, "fromFile() called - File: " + file.getAbsolutePath());
        Log.e(TAG, "fromFile() - Storing file reference for deferred loading");

        this.sourceType = SourceType.FILE;
        this.sourceFile = file;

        return this;
    }

    public PDFView fromBytes(byte[] bytes) {
        Log.d(TAG, "fromBytes() called - size: " + bytes.length + " bytes");
        Log.e(TAG, "fromBytes() - Storing bytes for deferred loading");

        this.sourceType = SourceType.BYTES;
        this.sourceBytes = bytes;

        return this;
    }

    public PDFView fromUri(android.net.Uri uri) {
        Log.d(TAG, "fromUri() called - URI: " + uri);
        Log.e(TAG, "fromUri() - Storing URI for deferred loading");

        this.sourceType = SourceType.URI;
        this.sourceUri = uri;

        return this;
    }

    public PDFView fromStream(InputStream inputStream) {
        Log.d(TAG, "fromStream() called");
        Log.e(TAG, "fromStream() - Storing stream for deferred loading");

        this.sourceType = SourceType.STREAM;
        this.sourceStream = inputStream;

        return this;
    }

    public PDFView fromUrl(String url) {
        Log.d(TAG, "fromUrl() called - URL: " + url);
        Log.e(TAG, "fromUrl() - Storing URL for deferred loading");

        this.sourceType = SourceType.URL;
        this.sourceUrl = url;

        return this;
    }

    public void load() {
        Log.d(TAG, "load() called - sourceType: " + sourceType);
        Log.e(TAG, "==========================================");
        Log.e(TAG, "load() - Starting PDF loading process");
        Log.e(TAG, "==========================================");

        if (sourceType == SourceType.NONE) {
            Log.e(TAG, "ERROR: No PDF source specified! Call fromXXX() before load()");
            if (onErrorListener != null) {
                onErrorListener.onError(new IllegalStateException(
                        "No PDF source specified. Call fromAsset(), fromFile(), fromUri(), fromBytes(), fromStream(), or fromUrl() before calling load()"));
            }
            return;
        }

        switch (sourceType) {
            case ASSET:
                loadFromAsset();
                break;
            case FILE:
                loadFromFile();
                break;
            case URI:
                loadFromUri();
                break;
            case BYTES:
                loadFromBytes();
                break;
            case STREAM:
                loadFromStream();
                break;
            case URL:
                loadFromUrl();
                break;
        }
    }

    // v1.0.16: Private loading methods (called by load())

    private void loadFromAsset() {
        try {
            Log.d(TAG, "Loading PDF from asset: " + assetName);
            Log.e(TAG, "==========================================");
            Log.e(TAG, "loadFromAsset() - Asset: " + assetName);
            Log.e(TAG, "==========================================");

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
            previousPage = -1;

            // Adjust total pages if custom page order is specified
            if (pages != null) {
                totalPages = pages.length;
            }

            Log.d(TAG, "PDF loaded successfully with " + totalPages + " pages");
            Log.e(TAG, "==========================================");
            Log.e(TAG, "PDF LOADED FROM ASSET: " + totalPages + " pages");
            Log.e(TAG, "==========================================");

            if (onLoadCompleteListener != null) {
                onLoadCompleteListener.loadComplete(totalPages);
            }

            // Use lazy loading for continuous mode
            if (continuousScrollMode) {
                initializePageOffsets();
                renderVisiblePages();

                // Trigger initial page change event
                if (onPageChangeListener != null) {
                    onPageChangeListener.onPageChanged(currentPage, totalPages);
                    Log.d(TAG, "Initial page change event: page " + (currentPage + 1) + " of " + totalPages);
                    Log.e(TAG, "INITIAL PAGE CHANGE EVENT: page " + (currentPage + 1) + " of " + totalPages);
                }
            } else {
                renderPage(currentPage);

                // Trigger initial page change event
                if (onPageChangeListener != null) {
                    onPageChangeListener.onPageChanged(currentPage, totalPages);
                    Log.d(TAG, "Initial page change event: page " + (currentPage + 1) + " of " + totalPages);
                    Log.e(TAG, "INITIAL PAGE CHANGE EVENT: page " + (currentPage + 1) + " of " + totalPages);
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Error loading PDF from asset: " + e.getMessage());
            if (onErrorListener != null) {
                onErrorListener.onError(e);
            }
        }
    }

    private void loadFromFile() {
        try {
            Log.d(TAG, "Loading PDF from file: " + sourceFile.getAbsolutePath());

            fileDescriptor = ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);

            totalPages = pdfRenderer.getPageCount();
            currentPage = defaultPage;
            previousPage = -1;

            // Adjust total pages if custom page order is specified
            if (pages != null) {
                totalPages = pages.length;
            }

            Log.d(TAG, "PDF loaded successfully with " + totalPages + " pages");

            if (onLoadCompleteListener != null) {
                onLoadCompleteListener.loadComplete(totalPages);
            }

            // Use lazy loading for continuous mode
            if (continuousScrollMode) {
                initializePageOffsets();
                renderVisiblePages();

                // Trigger initial page change event
                if (onPageChangeListener != null) {
                    onPageChangeListener.onPageChanged(currentPage, totalPages);
                    Log.d(TAG, "Initial page change event: page " + (currentPage + 1) + " of " + totalPages);
                }
            } else {
                renderPage(currentPage);

                // Trigger initial page change event
                if (onPageChangeListener != null) {
                    onPageChangeListener.onPageChanged(currentPage, totalPages);
                    Log.d(TAG, "Initial page change event: page " + (currentPage + 1) + " of " + totalPages);
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Error loading PDF from file: " + e.getMessage());
            if (onErrorListener != null) {
                onErrorListener.onError(e);
            }
        }
    }

    private void loadFromUri() {
        try {
            Log.d(TAG, "Loading PDF from URI: " + sourceUri.toString());
            Log.e(TAG, "loadFromUri() - URI: " + sourceUri);

            // Use ParcelFileDescriptor directly (matches AndroidPdfViewer)
            // This is MUCH faster than copying to temp file!
            ParcelFileDescriptor pfd = getContext().getContentResolver().openFileDescriptor(sourceUri, "r");
            if (pfd == null) {
                throw new IOException("Cannot open file descriptor from URI: " + sourceUri);
            }

            // Store file descriptor for later cleanup
            fileDescriptor = pfd;
            pdfRenderer = new PdfRenderer(fileDescriptor);

            totalPages = pdfRenderer.getPageCount();
            currentPage = defaultPage;
            previousPage = -1;

            // Adjust total pages if custom page order is specified
            if (pages != null) {
                totalPages = pages.length;
            }

            Log.d(TAG, "PDF loaded successfully from URI with " + totalPages + " pages");
            Log.e(TAG, "PDF loaded successfully from URI with " + totalPages + " pages");

            if (onLoadCompleteListener != null) {
                onLoadCompleteListener.loadComplete(totalPages);
            }

            // Use lazy loading for continuous mode
            if (continuousScrollMode) {
                initializePageOffsets();
                renderVisiblePages();

                // Trigger initial page change event
                if (onPageChangeListener != null) {
                    onPageChangeListener.onPageChanged(currentPage, totalPages);
                    Log.d(TAG, "Initial page change event: page " + (currentPage + 1) + " of " + totalPages);
                    Log.e(TAG, "Initial page change event: page " + (currentPage + 1) + " of " + totalPages);
                }
            } else {
                renderPage(currentPage);

                // Trigger initial page change event
                if (onPageChangeListener != null) {
                    onPageChangeListener.onPageChanged(currentPage, totalPages);
                    Log.d(TAG, "Initial page change event: page " + (currentPage + 1) + " of " + totalPages);
                    Log.e(TAG, "Initial page change event: page " + (currentPage + 1) + " of " + totalPages);
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Error loading PDF from URI: " + e.getMessage());
            Log.e(TAG, "Full error: " + android.util.Log.getStackTraceString(e));
            if (onErrorListener != null) {
                onErrorListener.onError(e);
            }
        }
    }

    private void loadFromBytes() {
        try {
            Log.d(TAG, "Loading PDF from bytes: " + sourceBytes.length + " bytes");
            Log.e(TAG, "loadFromBytes() - size: " + sourceBytes.length + " bytes");

            // Create a temporary file from bytes
            File tempFile = File.createTempFile("pdf_temp", ".pdf", getContext().getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            outputStream.write(sourceBytes);
            outputStream.close();

            Log.e(TAG, "loadFromBytes() - temp file created, loading from file");

            // Now load from the temp file
            sourceFile = tempFile;
            loadFromFile();

        } catch (IOException e) {
            Log.e(TAG, "Error loading PDF from bytes: " + e.getMessage());
            Log.e(TAG, "Full error: " + android.util.Log.getStackTraceString(e));
            if (onErrorListener != null) {
                onErrorListener.onError(e);
            }
        }
    }

    private void loadFromStream() {
        try {
            Log.d(TAG, "Loading PDF from InputStream");
            Log.e(TAG, "loadFromStream() - WARNING: This copies to temp file (slower than fromUri)");

            // Create a temporary file from input stream
            File tempFile = File.createTempFile("pdf_temp", ".pdf", getContext().getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            // Copy the input stream to the temporary file
            byte[] buffer = new byte[1024];
            int length;
            while ((length = sourceStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            sourceStream.close();
            outputStream.close();

            Log.e(TAG, "loadFromStream() - temp file created, loading from file");

            // Now load from the temp file
            sourceFile = tempFile;
            loadFromFile();

        } catch (IOException e) {
            Log.e(TAG, "Error loading PDF from InputStream: " + e.getMessage());
            Log.e(TAG, "Full error: " + android.util.Log.getStackTraceString(e));
            if (onErrorListener != null) {
                onErrorListener.onError(e);
            }
        }
    }

    private void loadFromUrl() {
        if (sourceUrl == null || sourceUrl.trim().isEmpty()) {
            if (onErrorListener != null) {
                onErrorListener.onError(new IllegalArgumentException("URL cannot be null or empty"));
            }
            return;
        }

        // Download PDF from URL in background thread
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Downloading PDF from URL: " + sourceUrl);

                URL pdfUrl = new URL(sourceUrl);
                HttpURLConnection connection = (HttpURLConnection) pdfUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000); // 30 seconds
                connection.setReadTimeout(60000); // 60 seconds

                // Set user agent to avoid blocking
                connection.setRequestProperty("User-Agent", "Alamin5G-PDF-Viewer/1.0.16");

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
                post(() -> {
                    sourceFile = tempFile;
                    loadFromFile();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error downloading PDF from URL: " + e.getMessage(), e);
                if (onErrorListener != null) {
                    post(() -> onErrorListener.onError(e));
                }
            }
        });
    }

    // Navigation methods
    public void jumpTo(int page) {
        Log.d(TAG, "jumpTo called with page: " + page + ", totalPages: " + totalPages);

        if (page < 0 || page >= totalPages) {
            Log.w(TAG, "Invalid page number: " + page + " (total: " + totalPages + ")");
            return;
        }

        currentPage = page;

        // v1.0.14: Different behavior for continuous vs single page mode
        if (continuousScrollMode) {
            // In continuous mode, scroll to the page position (don't render single page)
            if (page < pageOffsets.size()) {
                // CRITICAL: Scale BASE offset by scaleFactor (like AndroidPdfViewer)
                float baseOffset = pageOffsets.get(page);
                float scaledOffset = baseOffset * scaleFactor;
                panY = -scaledOffset;
                panX = 0;

                // Ensure visible pages are rendered
                renderVisiblePages();

                Log.d(TAG, "Jumped to page " + page + " in continuous mode, panY: " + panY);
            }
        } else {
            // Single page mode: render the specific page
            Log.d(TAG, "Jumping to page: " + currentPage);
            renderPage(currentPage);
            resetZoom();
        }

        if (onPageChangeListener != null) {
            onPageChangeListener.onPageChanged(currentPage, totalPages);
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

    // v1.0.14: Position and offset methods for AndroidPdfViewer compatibility
    public float getPositionOffset() {
        if (totalContentHeight == 0)
            return 0;
        // CRITICAL: Use scaled content height
        float scaledContentHeight = totalContentHeight * scaleFactor;
        return Math.abs(panY) / scaledContentHeight;
    }

    public void setPositionOffset(float progress) {
        setPositionOffset(progress, true);
    }

    public void setPositionOffset(float progress, boolean moveHandle) {
        if (totalContentHeight == 0)
            return;

        // CRITICAL: Scale BASE totalContentHeight by scaleFactor
        float scaledContentHeight = totalContentHeight * scaleFactor;
        float targetPanY = -progress * scaledContentHeight;
        panY = Math.max(-scaledContentHeight + getHeight(), Math.min(0, targetPanY));

        if (continuousScrollMode) {
            renderVisiblePages();
        }

        // v1.0.15: Check for page change (matches AndroidPdfViewer)
        loadPageByOffset();

        // Issue #6: Sync scroll handle position when not triggered by handle drag
        if (moveHandle && scrollHandle instanceof DefaultScrollHandle) {
            ((DefaultScrollHandle) scrollHandle).setScroll(progress);
        }

        invalidate();
        Log.d(TAG, "Position offset set to: " + progress + ", panY: " + panY);
    }

    /**
     * Issue #6: Sync scroll handle with current scroll position.
     * Called from computeScroll() and onScroll() to keep handle in sync.
     */
    private void updateScrollHandle() {
        if (scrollHandle instanceof DefaultScrollHandle) {
            ((DefaultScrollHandle) scrollHandle).setScroll(getPositionOffset());
        }
    }

    /**
     * Move to absolute position with bounds checking (v1.0.14)
     * Similar to AndroidPdfViewer's moveTo() method
     */
    public void moveTo(float x, float y) {
        if (!continuousScrollMode) {
            return;
        }

        // Apply bounds (like AndroidPdfViewer)
        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float contentWidth = viewWidth * scaleFactor;
        // CRITICAL: Scale BASE totalContentHeight by scaleFactor (like
        // AndroidPdfViewer)
        float contentHeight = totalContentHeight * scaleFactor;

        // Horizontal pan limits (allow FULL left-right panning)
        if (contentWidth < viewWidth) {
            // Content fits - center it
            panX = (viewWidth - contentWidth) / 2f;
        } else {
            // Content larger - allow panning from 0 to -(contentWidth - viewWidth)
            panX = Math.max(-(contentWidth - viewWidth), Math.min(0, x));
        }

        // Vertical pan limits
        if (contentHeight < viewHeight) {
            // Content fits - center it
            panY = (viewHeight - contentHeight) / 2f;
        } else {
            // Content larger - allow panning from 0 to -(contentHeight - viewHeight)
            panY = Math.max(-(contentHeight - viewHeight), Math.min(0, y));
        }

        // v1.0.15: Check for page change (matches AndroidPdfViewer)
        loadPageByOffset();

        invalidate();
    }

    /**
     * Move relative to current position (v1.0.14)
     * Similar to AndroidPdfViewer's moveRelativeTo() method
     */
    public void moveRelativeTo(float dx, float dy) {
        moveTo(panX + dx, panY + dy);
    }

    public float getCurrentXOffset() {
        return panX;
    }

    public float getCurrentYOffset() {
        return panY;
    }

    public boolean isZooming() {
        return scaleFactor > minZoom;
    }

    public boolean isRecycled() {
        return pdfRenderer == null;
    }

    public void stopFling() {
        // Stop any ongoing scroll animation
        // In our implementation, we don't have explicit fling animation to stop
        // But we can clear velocity if needed
        Log.d(TAG, "stopFling called");
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        if (!continuousScrollMode)
            return false;

        float contentWidth = getWidth() * scaleFactor;
        if (direction > 0) {
            // Check if can scroll right
            return panX < (contentWidth - getWidth()) / 2f;
        } else {
            // Check if can scroll left
            return panX > -(contentWidth - getWidth()) / 2f;
        }
    }

    @Override
    public boolean canScrollVertically(int direction) {
        if (!continuousScrollMode)
            return false;

        if (direction > 0) {
            // Check if can scroll down
            return panY < 0;
        } else {
            // Check if can scroll up
            // v1.0.17 fix: Must multiply by scaleFactor for correct scroll limits at zoom > 1.0
            return panY > -(totalContentHeight * scaleFactor - getHeight());
        }
    }

    // v1.0.14: Additional utility methods for complete AndroidPdfViewer
    // compatibility
    @Override
    public void computeScroll() {
        super.computeScroll();

        // v1.0.14: Handle smooth fling animation with OverScroller
        if (scroller.computeScrollOffset()) {
            // Update position from scroller
            moveTo(-scroller.getCurrX(), -scroller.getCurrY());

            // Issue #6: Sync scroll handle during fling
            updateScrollHandle();

            // Request next frame
            postInvalidateOnAnimation();
        } else if (flinging) {
            // Fling animation finished
            flinging = false;
            // Final render after fling stops
            renderVisiblePages();

            // Issue #6: Final handle sync after fling ends
            updateScrollHandle();

            Log.d(TAG, "Fling ended, final render complete");
        }
    }

    public void performPageSnap() {
        if (!pageSnap || !continuousScrollMode || totalContentHeight == 0) {
            return;
        }

        // Calculate which page is closest to current position
        int nearestPage = 0;
        float minDistance = Float.MAX_VALUE;

        for (int i = 0; i < pageOffsets.size(); i++) {
            float pageTop = -pageOffsets.get(i);
            float distance = Math.abs(panY - pageTop);
            if (distance < minDistance) {
                minDistance = distance;
                nearestPage = i;
            }
        }

        // Snap to the nearest page
        float targetPanY = -pageOffsets.get(nearestPage);

        // Animate to snap position
        animate().translationY(targetPanY - panY)
                .setDuration(200)
                .withEndAction(() -> {
                    panY = targetPanY;
                    setTranslationY(0);
                    renderVisiblePages();
                    invalidate();
                }).start();

        Log.d(TAG, "Snapping to page: " + nearestPage);
    }

    public android.util.SizeF getPageSize(int pageIndex) {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= totalPages) {
            return new android.util.SizeF(0, 0);
        }

        try {
            PdfRenderer.Page page = pdfRenderer.openPage(pageIndex);
            android.util.SizeF size = new android.util.SizeF(page.getWidth(), page.getHeight());
            page.close();
            return size;
        } catch (Exception e) {
            Log.e(TAG, "Error getting page size: " + e.getMessage());
            return new android.util.SizeF(0, 0);
        }
    }

    public int getPageAtPositionOffset(float positionOffset) {
        if (totalContentHeight == 0 || pageOffsets.isEmpty()) {
            return 0;
        }

        float targetY = positionOffset * totalContentHeight;

        for (int i = 0; i < pageOffsets.size(); i++) {
            if (pageOffsets.get(i) >= targetY) {
                return Math.max(0, i - 1);
            }
        }

        return totalPages - 1;
    }

    // Zoom methods
    // Issue #5: Return PDFView for builder chaining
    public PDFView setMinZoom(float minZoom) {
        this.minZoom = minZoom;
        return this;
    }

    public PDFView setMidZoom(float midZoom) {
        this.midZoom = midZoom;
        return this;
    }

    public PDFView setMaxZoom(float maxZoom) {
        this.maxZoom = maxZoom;
        return this;
    }

    public float getMaxZoom() {
        return maxZoom;
    }

    public float getMinZoom() {
        return minZoom;
    }

    public float getMidZoom() {
        return midZoom;
    }

    public boolean isZoomEnabled() {
        return zoomEnabled;
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

    // v1.0.14: Zoom with animation centered at specific point
    public void zoomWithAnimation(float centerX, float centerY, float scale) {
        animate().scaleX(scale / scaleFactor).scaleY(scale / scaleFactor).setDuration(300).withEndAction(() -> {
            zoomCenteredTo(scale, centerX, centerY);
            setScaleX(1.0f);
            setScaleY(1.0f);
        }).start();
    }

    // v1.0.14: Relative zoom method
    public void zoomCenteredRelativeTo(float dzoom, android.graphics.PointF pivot) {
        float newZoom = scaleFactor + dzoom;
        newZoom = Math.max(minZoom, Math.min(maxZoom, newZoom));
        zoomCenteredTo(newZoom, pivot.x, pivot.y);
    }

    public float getZoom() {
        return scaleFactor;
    }

    public void resetZoom() {
        scaleFactor = 1.0f;
        panX = 0f;
        panY = 0f;
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

    // v1.0.14: Layout check methods
    public boolean pageFillsScreen() {
        if (continuousScrollMode) {
            return totalContentHeight * scaleFactor >= getHeight();
        }
        return true; // Single page always fills
    }

    public boolean documentFitsView() {
        return totalContentHeight * scaleFactor <= getHeight();
    }

    public void fitToWidth(int page) {
        if (page < 0 || page >= totalPages)
            return;

        jumpTo(page);

        // Calculate zoom needed to fit width
        if (getWidth() > 0) {
            android.util.SizeF pageSize = getPageSize(page);
            if (pageSize.getWidth() > 0) {
                float targetZoom = (float) getWidth() / pageSize.getWidth();
                zoomTo(targetZoom);
            }
        }
    }

    // v1.0.14: Scale conversion utilities
    public float toRealScale(float size) {
        return size / scaleFactor;
    }

    public float toCurrentScale(float size) {
        return size * scaleFactor;
    }

    /**
     * Zoom centered to a pivot point (like Adobe Reader)
     * This makes the zoom feel natural - zooming around the touch point
     */
    private void zoomCenteredTo(float zoom, float pivotX, float pivotY) {
        float dzoom = zoom / this.scaleFactor;
        float oldZoom = this.scaleFactor;
        float oldPanX = panX;
        float oldPanY = panY;

        this.scaleFactor = zoom;

        // Adjust pan offsets to keep the pivot point in place
        // Based on AndroidPdfViewer implementation
        float newPanX = panX * dzoom + (pivotX - pivotX * dzoom);
        float newPanY = panY * dzoom + (pivotY - pivotY * dzoom);

        Log.d(TAG, "ZOOM CENTERED: oldZoom=" + oldZoom + " → newZoom=" + zoom + ", dzoom=" + dzoom);
        Log.d(TAG, "ZOOM PAN: oldPan=(" + oldPanX + "," + oldPanY + ") → newPan=(" + newPanX + "," + newPanY + ")");

        // Apply the new pan with proper limits
        panX = newPanX;
        panY = newPanY;

        // Apply pan limits (like AndroidPdfViewer)
        float viewWidth = getWidth();
        float viewHeight = getHeight();
        // CRITICAL: Scale BASE totalContentHeight by scaleFactor (like
        // AndroidPdfViewer)
        float contentHeight = totalContentHeight * scaleFactor;
        float contentWidth = viewWidth * scaleFactor;

        // Horizontal pan limits (allow FULL left-right panning)
        if (contentWidth < viewWidth) {
            // Content fits - center it
            panX = (viewWidth - contentWidth) / 2f;
        } else {
            // Content larger - allow panning from 0 to -(contentWidth - viewWidth)
            panX = Math.max(-(contentWidth - viewWidth), Math.min(0, panX));
        }

        // Vertical pan limits
        if (contentHeight < viewHeight) {
            // Content fits - center it
            panY = (viewHeight - contentHeight) / 2f;
        } else {
            // Content larger - allow panning from 0 to -(contentHeight - viewHeight)
            panY = Math.max(-(contentHeight - viewHeight), Math.min(0, panY));
        }

        // v1.0.14: Don't clear cache or re-render during zoom gesture
        // This prevents page jumps and flicker. Re-rendering happens in onScaleEnd()
        invalidate();

        Log.d(TAG, "Zoom centered to " + zoom + " at pivot (" + pivotX + ", " + pivotY + "), pan: (" + panX + ", "
                + panY + ")");
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

        // Issue #6: Cleanup scroll handle
        if (scrollHandle instanceof DefaultScrollHandle) {
            ((DefaultScrollHandle) scrollHandle).destroy();
        }
    }

    /**
     * v1.0.13: Initialize page offsets without rendering bitmaps (lightweight
     * operation)
     * This calculates positions for ALL pages but doesn't create any bitmaps yet
     */
    private void initializePageOffsets() {
        if (pdfRenderer == null || getWidth() == 0 || getHeight() == 0) {
            Log.w(TAG, "Cannot initialize page offsets yet - waiting for layout");
            return;
        }

        pageOffsets.clear();
        float currentY = 0f;
        float viewWidth = getWidth();

        Log.d(TAG, "Initializing page offsets for " + totalPages + " pages at BASE zoom (1.0)");

        for (int i = 0; i < totalPages; i++) {
            pageOffsets.add(currentY);

            // CRITICAL: Calculate height at BASE zoom (1.0), NOT current scaleFactor!
            // AndroidPdfViewer stores offsets at zoom=1.0 and multiplies by zoom when
            // drawing
            // This way offsets don't need recalculation when zoom changes!
            try {
                PdfRenderer.Page page = pdfRenderer.openPage(i);
                int width = (int) viewWidth; // Base width (zoom = 1.0)
                int height = (int) (width * (float) page.getHeight() / page.getWidth());
                page.close();

                currentY += height + spacing;

                Log.d(TAG, "Page " + i + " BASE offset: " + pageOffsets.get(i) + ", BASE height: " + height);
            } catch (Exception e) {
                Log.e(TAG, "Error calculating page offset: " + e.getMessage());
                currentY += 1681 + spacing; // Default height fallback
            }
        }

        totalContentHeight = currentY;
        Log.d(TAG, "Page offsets initialized at BASE zoom, total BASE height: " + totalContentHeight);
    }

    /**
     * v1.0.13: Calculate which pages are currently visible on screen
     */
    private void calculateVisiblePages() {
        float viewHeight = getHeight();
        float scrollY = Math.abs(panY);

        visibleStartPage = 0;
        visibleEndPage = 0;

        boolean foundStart = false;

        for (int i = 0; i < pageOffsets.size(); i++) {
            // CRITICAL: Scale BASE offset by scaleFactor (like AndroidPdfViewer)
            float baseOffset = pageOffsets.get(i);
            float pageTop = baseOffset * scaleFactor;

            // Estimate page height (check cache or use default)
            float pageHeight;
            Bitmap cachedBitmap = continuousPageCache.get(i);
            if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
                pageHeight = cachedBitmap.getHeight();
            } else if (i < pageOffsets.size() - 1) {
                float baseHeight = pageOffsets.get(i + 1) - baseOffset - spacing;
                pageHeight = baseHeight * scaleFactor;
            } else {
                pageHeight = 1681 * scaleFactor; // Default height scaled
            }

            float pageBottom = pageTop + pageHeight;

            // Check if page is visible
            if (pageBottom >= scrollY && pageTop <= scrollY + viewHeight) {
                if (!foundStart) {
                    visibleStartPage = i;
                    foundStart = true;
                }
                visibleEndPage = i;
            }
        }

        Log.d(TAG, "Visible pages: " + visibleStartPage + " to " + visibleEndPage + " (scroll: " + scrollY + ")");
    }

    /**
     * v1.0.15: Detect page change during scroll (matches AndroidPdfViewer)
     * Called after moveTo() to check if current page changed
     */
    private void loadPageByOffset() {
        if (!continuousScrollMode || totalPages == 0) {
            return;
        }

        // Calculate screen center position
        float offset = Math.abs(panY);
        float screenCenter = ((float) getHeight()) / 2;

        // Find which page is at screen center
        int newPage = getPageAtPosition(offset + screenCenter);

        // If page changed, trigger callback
        if (newPage >= 0 && newPage < totalPages && newPage != currentPage) {
            previousPage = currentPage;
            currentPage = newPage;

            // Trigger listener (matches AndroidPdfViewer's showPage())
            if (onPageChangeListener != null) {
                onPageChangeListener.onPageChanged(currentPage, totalPages);
            }

            // P1: OnPageScrollListener - notify scroll position change
            if (onPageScrollListener != null) {
                float scrollOffset = getPositionOffset();
                onPageScrollListener.onPageScrolled(currentPage, scrollOffset);
            }

            Log.d(TAG, "Page changed: " + (previousPage + 1) + " → " + (currentPage + 1) + " of " + totalPages);
            Log.e(TAG, "★ PAGE CHANGED: " + (previousPage + 1) + " → " + (currentPage + 1) + " of " + totalPages); // v1.0.15:
                                                                                                                   // ERROR
                                                                                                                   // for
                                                                                                                   // visibility
        }
    }

    /**
     * v1.0.15: Helper to find page at Y position
     */
    private int getPageAtPosition(float yPosition) {
        for (int i = 0; i < pageOffsets.size(); i++) {
            float baseOffset = pageOffsets.get(i);
            float pageTop = baseOffset * scaleFactor;

            // Estimate page height using same logic as calculateVisiblePages()
            float pageHeight;
            Bitmap cachedBitmap = continuousPageCache.get(i);
            if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
                pageHeight = cachedBitmap.getHeight();
            } else if (i < pageOffsets.size() - 1) {
                float baseHeight = pageOffsets.get(i + 1) - baseOffset - spacing;
                pageHeight = baseHeight * scaleFactor;
            } else {
                pageHeight = 1681 * scaleFactor; // Default height scaled
            }

            float pageBottom = pageTop + pageHeight;

            // Check if position is within this page
            if (yPosition >= pageTop && yPosition <= pageBottom) {
                return i;
            }
        }

        // Default to first visible page
        return visibleStartPage;
    }

    /**
     * v1.0.13: Render a single page and add to cache
     */
    private void renderSinglePage(int pageIndex) {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= totalPages) {
            return;
        }

        try {
            PdfRenderer.Page page = pdfRenderer.openPage(pageIndex);

            float viewWidth = getWidth();
            // CRITICAL: Render at CURRENT zoom level (scaleFactor) for high quality
            // Bitmaps are rendered at zoom resolution, then drawn with scaled offsets
            int width = (int) (viewWidth * scaleFactor);
            int height = (int) (width * (float) page.getHeight() / page.getWidth());

            Bitmap.Config config = useBestQuality ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
            Bitmap bitmap = Bitmap.createBitmap(width, height, config);

            int renderMode = enableAnnotationRendering ? PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    : PdfRenderer.Page.RENDER_MODE_FOR_PRINT;
            page.render(bitmap, null, null, renderMode);
            page.close();

            // Add to cache (automatically removes oldest if cache is full)
            continuousPageCache.put(pageIndex, bitmap);

            Log.d(TAG, "Rendered page " + pageIndex + " at zoom " + scaleFactor + " (" + width + "x" + height
                    + ") - Cache size: " + continuousPageCache.size());
        } catch (Exception e) {
            Log.e(TAG, "Error rendering page " + pageIndex + ": " + e.getMessage());
        }
    }

    /**
     * v1.0.13: Render only visible pages (lazy loading - FIXES OOM CRASH!)
     * This replaces renderAllPages() to prevent memory overflow with large PDFs
     */
    private void renderVisiblePages() {
        if (pdfRenderer == null || getWidth() == 0 || getHeight() == 0) {
            Log.w(TAG, "Cannot render visible pages yet - waiting for layout");
            return;
        }

        // Calculate which pages are currently visible
        calculateVisiblePages();

        // v1.0.16: Render visible pages + buffer (3 pages before/after for smoother
        // scrolling)
        // Increased from 2 to 3 to reduce gaps during fast scrolling
        int startPage = Math.max(0, visibleStartPage - 3);
        int endPage = Math.min(totalPages - 1, visibleEndPage + 3);

        Log.d(TAG, "Rendering pages " + startPage + " to " + endPage + " (visible: " + visibleStartPage + "-"
                + visibleEndPage + ") at zoom: " + scaleFactor);
        Log.e(TAG, "Rendering pages " + startPage + " to " + endPage + " (visible: " + visibleStartPage + "-"
                + visibleEndPage + ")");

        // Only render pages that aren't already cached
        for (int i = startPage; i <= endPage; i++) {
            if (!continuousPageCache.containsKey(i)) {
                renderSinglePage(i);
            }
        }

        invalidate();
    }

    /**
     * v1.0.16: Re-render visible pages at new zoom level WITHOUT clearing cache
     * This prevents gaps during zoom by keeping old bitmaps visible while new ones
     * render
     * Fixes issue #2: https://github.com/alamin5G/Alamin5G-PDF-Viewer/issues/2
     */
    private void renderVisiblePagesAtNewZoom() {
        if (pdfRenderer == null || getWidth() == 0 || getHeight() == 0) {
            Log.w(TAG, "Cannot render visible pages yet - waiting for layout");
            return;
        }

        calculateVisiblePages();

        // v1.0.16: Increased buffer from 2 to 3 for smoother scrolling
        int startPage = Math.max(0, visibleStartPage - 3);
        int endPage = Math.min(totalPages - 1, visibleEndPage + 3);

        Log.d(TAG, "Re-rendering pages " + startPage + " to " + endPage + " at new zoom: " + scaleFactor);
        Log.e(TAG, "Re-rendering pages " + startPage + " to " + endPage + " (visible: " + visibleStartPage + "-"
                + visibleEndPage + ") at zoom: " + scaleFactor);

        // Force re-render even if pages are in cache (they're at wrong zoom level)
        // Old bitmaps stay visible until new ones are ready
        for (int i = startPage; i <= endPage; i++) {
            renderSinglePageForceUpdate(i);
        }

        invalidate();
    }

    /**
     * v1.0.16: Render a single page and REPLACE existing cached version
     * Used when zoom level changes to update bitmaps at new resolution
     * Old bitmap stays visible until new one is ready (no gaps!)
     */
    private void renderSinglePageForceUpdate(int pageIndex) {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= totalPages) {
            return;
        }

        executorService.execute(() -> {
            try {
                PdfRenderer.Page page = pdfRenderer.openPage(pageIndex);

                float viewWidth = getWidth();
                // Render at CURRENT zoom level for high quality
                int width = (int) (viewWidth * scaleFactor);
                int height = (int) (width * (float) page.getHeight() / page.getWidth());

                Bitmap bitmap = Bitmap.createBitmap(width, height,
                        useBestQuality ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);

                int renderMode = enableAnnotationRendering
                        ? PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        : PdfRenderer.Page.RENDER_MODE_FOR_PRINT;
                page.render(bitmap, null, null, renderMode);
                page.close();

                // Post to main thread to update cache
                post(() -> {
                    // Replace old bitmap with new one
                    Bitmap oldBitmap = continuousPageCache.put(pageIndex, bitmap);

                    // Recycle old bitmap if it exists and is different
                    if (oldBitmap != null && !oldBitmap.isRecycled() && oldBitmap != bitmap) {
                        oldBitmap.recycle();
                        Log.d(TAG, "Replaced page " + pageIndex + " bitmap at new zoom " + scaleFactor);
                    }

                    invalidate();
                });

                Log.d(TAG, "Force-rendered page " + pageIndex + " at zoom " + scaleFactor + " (" + width + "x" + height
                        + ")");
            } catch (Exception e) {
                Log.e(TAG, "Error force-rendering page " + pageIndex + ": " + e.getMessage());
            }
        });
    }

    private void renderPage(int pageIndex) {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= totalPages) {
            Log.e(TAG, "Cannot render page " + pageIndex + ": pdfRenderer=" + (pdfRenderer != null) + ", totalPages="
                    + totalPages);
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
                // Use RENDER_MODE_FOR_DISPLAY for annotations, RENDER_MODE_FOR_PRINT to exclude
                // them
                int renderMode = enableAnnotationRendering ? PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        : PdfRenderer.Page.RENDER_MODE_FOR_PRINT;
                page.render(bitmap, null, null, renderMode);

                // Close the page
                page.close();

                // Capture final values for lambda
                final int finalWidth = width;
                final int finalHeight = height;

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

                    // P1: OnRenderListener - notify page rendered
                    if (onRenderListener != null) {
                        onRenderListener.onPageRendered(pageIndex, finalWidth, finalHeight);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error rendering page " + pageIndex + ": " + e.getMessage());
                // P1: OnPageErrorListener - notify page-specific error
                if (onPageErrorListener != null) {
                    final int page = pageIndex;
                    post(() -> onPageErrorListener.onPageError(page, e));
                } else if (onErrorListener != null) {
                    post(() -> onErrorListener.onError(e));
                }
            }
        });
    }

    private void updateColorFilter() {
        if (nightMode) {
            // Invert colors for night mode
            colorMatrix.set(new float[] {
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
                if (continuousScrollMode) {
                    // Zoom centered to the focus point (like Adobe Reader)
                    float focusX = detector.getFocusX();
                    float focusY = detector.getFocusY();
                    zoomCenteredTo(newScaleFactor, focusX, focusY);
                } else {
                    // In single page mode, update matrix for proper centering
                    scaleFactor = newScaleFactor;
                    updateMatrixScale();
                    invalidate();
                }

                Log.d(TAG, "Zoom applied: " + scaleFactor + ", continuous: " + continuousScrollMode);
            }

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            // Issue #5: Block zoom gesture when disabled
            if (!zoomEnabled) {
                Log.d(TAG, "Zoom gesture BLOCKED - zoom disabled");
                return false;
            }
            Log.d(TAG, "Zoom gesture STARTED at zoom: " + scaleFactor);
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            // v1.0.16: Re-render pages at new zoom level WITHOUT clearing cache
            // This prevents gaps by keeping old bitmaps visible while new ones render
            if (continuousScrollMode && Math.abs(scaleFactor - lastRenderedZoom) > 0.3f) {
                Log.d(TAG, "Zoom gesture ended, re-rendering at zoom: " + scaleFactor);
                Log.e(TAG, "==========================================");
                Log.e(TAG, "ZOOM ENDED - Smart re-render (no cache clear)");
                Log.e(TAG, "==========================================");
                lastRenderedZoom = scaleFactor;

                // ✅ FIX for issue #2: Don't clear cache!
                // Old bitmaps stay visible while new ones render in background
                // They'll be replaced automatically when new bitmaps are ready
                renderVisiblePagesAtNewZoom();
            }
        }
    }

    // Gesture listener for swipe navigation
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            // v1.0.14: Stop any active fling when user touches the screen
            if (flinging) {
                scroller.forceFinished(true);
                flinging = false;
                Log.d(TAG, "Fling stopped by user touch");
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (continuousScrollMode) {
                // v1.0.14: Use moveRelativeTo() for proper bounds checking
                moveRelativeTo(-distanceX, -distanceY);

                // Issue #6: Sync scroll handle during gesture scroll
                updateScrollHandle();

                // v1.0.13: Trigger lazy loading when scrolling (only if not flinging)
                if (!flinging) {
                    renderVisiblePages();
                }

                Log.d(TAG, "Continuous scrolling - pan: (" + panX + ", " + panY + "), zoom: " + scaleFactor);
                return true;
            } else if (scaleFactor > 1.0f) {
                // Single page mode - only pan when zoomed in
                panX -= distanceX;
                panY -= distanceY;

                // Apply pan limits to keep content visible
                float viewWidth = getWidth();
                float viewHeight = getHeight();

                if (currentBitmap != null) {
                    float bitmapWidth = currentBitmap.getWidth();
                    float bitmapHeight = currentBitmap.getHeight();
                    float scaleX = viewWidth / bitmapWidth;
                    float scaleY = viewHeight / bitmapHeight;
                    float baseScale = Math.min(scaleX, scaleY);
                    float finalScale = baseScale * scaleFactor;

                    float scaledWidth = bitmapWidth * finalScale;
                    float scaledHeight = bitmapHeight * finalScale;

                    // Calculate max pan limits
                    float maxPanX = Math.max(0, (scaledWidth - viewWidth) / 2f);
                    float maxPanY = Math.max(0, (scaledHeight - viewHeight) / 2f);

                    // Clamp pan values
                    panX = Math.max(-maxPanX, Math.min(panX, maxPanX));
                    panY = Math.max(-maxPanY, Math.min(panY, maxPanY));
                }

                updateMatrixScale();
                invalidate();
                Log.d(TAG, "Single page scrolling - pan: (" + panX + ", " + panY + ")");
                return true;
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!enableSwipe)
                return false;

            Log.d(TAG, "Fling detected: velocityX=" + velocityX + ", velocityY=" + velocityY);

            // v1.0.14: In continuous scroll mode with smooth scrolling enabled
            if (continuousScrollMode && !pageFling) {
                // Stop any existing fling
                if (flinging) {
                    scroller.forceFinished(true);
                }

                // Calculate bounds for fling animation (like AndroidPdfViewer)
                int startX = (int) -panX;
                int startY = (int) -panY;
                int viewWidth = getWidth();
                int viewHeight = getHeight();
                int minX = 0;
                // CRITICAL: Allow FULL width panning, not divided by 2!
                int maxX = (int) Math.max(0, viewWidth * scaleFactor - viewWidth);
                int minY = 0;
                int maxY = (int) Math.max(0, totalContentHeight * scaleFactor - viewHeight);

                // Start physics-based fling with OverScroller
                // Note: Negate velocities to match Android's scroll direction convention
                // (swipe up = negative velocity, but should scroll down = positive offset
                // change)
                scroller.fling(startX, startY,
                        (int) -velocityX, (int) -velocityY,
                        minX, maxX, minY, maxY);
                flinging = true;
                postInvalidateOnAnimation(); // Start animation loop

                Log.d(TAG, "OverScroller fling started: bounds=[" + minX + "," + maxX + "," + minY + "," + maxY + "]");
                return true;
            }

            // Single page mode: swipe gesture changes pages
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
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // P1: OnTapListener - allow user to handle tap
            if (onTapListener != null) {
                return onTapListener.onTap(e);
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // P1: OnLongPressListener - notify long press
            if (onLongPressListener != null) {
                onLongPressListener.onLongPress(e);
            }
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (!enableDoubletap)
                return false;

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
