# Alamin5G PDF Viewer 📚

[![JitPack](https://jitpack.io/v/alamin5g/Alamin5G-PDF-Viewer.svg)](https://jitpack.io/#alamin5g/Alamin5G-PDF-Viewer)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A powerful, **16KB-page memory compatible** Android PDF library featuring animations, gestures, zoom, and double-tap support. Built with Android's native `PdfRenderer` API for full 16KB page size compatibility required by Google Play starting November 1st, 2025.

## 🚀 Key Features

### ✅ 16KB Page Size Compatibility
- **Full 16KB Alignment Support**: Built with Android's native `PdfRenderer` API
- **Google Play Ready**: Meets all 16KB page size requirements for Android 15+
- **No Native Library Conflicts**: Eliminates problematic `.so` files
- **Future Proof**: Compatible with upcoming Android versions

### ✅ Core PDF Functionality
- **PDF Rendering**: High-quality PDF page rendering with customizable quality
- **Multiple Load Sources**: Assets, files, URIs, bytes, and streams
- **Zoom Support**: Pinch-to-zoom, double-tap zoom, and programmatic zoom control
- **Page Navigation**: Swipe gestures, page jumping, and smooth transitions
- **Fit Policies**: Width, height, and both fitting options
- **Night Mode**: Inverted colors for dark theme support
- **Custom Page Order**: Load specific pages in custom sequence

### ✅ Performance Optimizations
- **Memory Efficient**: Optimized bitmap rendering with LRU caching
- **Smooth Scrolling**: Hardware-accelerated rendering
- **Intelligent Caching**: Page caching system (configurable cache size)
- **Background Rendering**: Non-blocking page rendering with thread pool
- **Quality Settings**: ARGB_8888 vs RGB_565 for memory optimization

### ✅ Advanced Features
- **Animations**: Smooth page transitions and zoom animations
- **Gesture Support**: Pinch-to-zoom, double-tap, pan, swipe navigation
- **Error Handling**: Comprehensive error callbacks and logging
- **Memory Management**: Automatic bitmap recycling and cleanup
- **Listener Support**: Load complete, page change, and error listeners

## 📦 Installation

### Step 1: Add Repository

Add JitPack repository to your root `build.gradle` or `settings.gradle`:

**For `settings.gradle` (Recommended):**
```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**For root `build.gradle` (Alternative):**
```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2: Add Dependency

Include the library in your app-level `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:1.0.11'
}
```

### Step 3: 16KB Compatibility Configuration

Ensure your app's `build.gradle` has 16KB compatibility:

```gradle
android {
    compileSdk 34
    
    defaultConfig {
        minSdk 24
        targetSdk 34
        
        // 16KB Page Size Compatibility
        ndk {
            version "28.0.0"
        }
        
        externalNativeBuild {
            cmake {
                arguments "-DANDROID_PAGE_SIZE_AGNOSTIC=ON"
            }
        }
    }
    
    // 16KB Page Size Compatibility Configuration
    packagingOptions {
        jniLibs {
            useLegacyPackaging = false
        }
        // Exclude problematic libraries that don't support 16KB alignment
        excludes += [
            '**/libc++_shared.so',
            '**/libjniPdfium.so',
            '**/libmodft2.so',
            '**/libmodpdfium.so',
            '**/libmodpng.so'
        ]
    }
}
```

## 📱 Basic Usage

### 1. Add PDFView to Layout

```xml
<com.alamin5g.pdf.PDFView
    android:id="@+id/pdfView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F5F5F5" />
```

### 2. Load PDF from Assets

```java
PDFView pdfView = findViewById(R.id.pdfView);

pdfView.fromAsset("sample.pdf")
    .enableSwipe(true)
    .swipeHorizontal(false)
    .enableDoubletap(true)
    .defaultPage(0)
    .onLoad(new OnLoadCompleteListener() {
        @Override
        public void loadComplete(int nbPages) {
            Log.d("PDF", "Loaded " + nbPages + " pages");
        }
    })
    .onPageChange(new OnPageChangeListener() {
        @Override
        public void onPageChanged(int page, int pageCount) {
            Log.d("PDF", "Page " + (page + 1) + " of " + pageCount);
        }
    })
    .onError(new OnErrorListener() {
        @Override
        public void onError(Throwable t) {
            Log.e("PDF", "Error: " + t.getMessage());
        }
    })
    .load();
```

## 🔧 Advanced Usage

### Complete Activity Example

```java
public class MainActivity extends AppCompatActivity {
    private PDFView pdfView;
    private Button btnPrevPage, btnNextPage, btnZoomIn, btnZoomOut;
    private TextView tvPageInfo;
    private int currentPage = 0;
    private int totalPages = 0;
    private float currentZoom = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupPDFView();
        setupControls();
    }

    private void initViews() {
        pdfView = findViewById(R.id.pdfView);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        tvPageInfo = findViewById(R.id.tvPageInfo);
    }

    private void setupPDFView() {
        pdfView.fromAsset("sample.pdf")
            .enableSwipe(true)                    // Enable swipe navigation
            .swipeHorizontal(false)               // Vertical scrolling
            .enableDoubletap(true)                // Enable double-tap zoom
            .enableAntialiasing(true)             // Smooth rendering
            .setNightMode(false)                  // Normal colors
            .useBestQuality(true)                 // High quality rendering
            .fitPolicy(PDFView.FitPolicy.WIDTH)   // Fit to width
            .defaultPage(0)                       // Start at first page
            .onLoad(new OnLoadCompleteListener() {
                @Override
                public void loadComplete(int nbPages) {
                    totalPages = nbPages;
                    currentPage = 0;
                    updatePageInfo();
                    updateButtonStates();
                    Toast.makeText(MainActivity.this, 
                        "PDF loaded: " + nbPages + " pages", 
                        Toast.LENGTH_SHORT).show();
                }
            })
            .onPageChange(new OnPageChangeListener() {
                @Override
                public void onPageChanged(int page, int pageCount) {
                    currentPage = page;
                    updatePageInfo();
                    updateButtonStates();
                }
            })
            .onError(new OnErrorListener() {
                @Override
                public void onError(Throwable t) {
                    Log.e("PDF", "Error loading PDF: " + t.getMessage());
                    Toast.makeText(MainActivity.this, 
                        "Error loading PDF: " + t.getMessage(), 
                        Toast.LENGTH_LONG).show();
                }
            })
            .load();
    }

    private void setupControls() {
        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 0) {
                pdfView.jumpTo(currentPage - 1, true); // With animation
            }
        });

        btnNextPage.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                pdfView.jumpTo(currentPage + 1, true); // With animation
            }
        });

        btnZoomIn.setOnClickListener(v -> {
            currentZoom = Math.min(currentZoom * 1.2f, 5.0f);
            pdfView.zoomWithAnimation(currentZoom);
        });

        btnZoomOut.setOnClickListener(v -> {
            currentZoom = Math.max(currentZoom / 1.2f, 0.5f);
            pdfView.zoomWithAnimation(currentZoom);
        });
    }

    private void updatePageInfo() {
        tvPageInfo.setText((currentPage + 1) + " / " + totalPages);
    }

    private void updateButtonStates() {
        btnPrevPage.setEnabled(currentPage > 0);
        btnNextPage.setEnabled(currentPage < totalPages - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfView != null) {
            pdfView.recycle(); // Clean up resources
        }
    }
}
```

### Layout Example (activity_main.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <!-- PDF Viewer -->
    <com.alamin5g.pdf.PDFView
        android:id="@+id/pdfView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:background="#F5F5F5" />

    <!-- Controls -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="16dp"
        android:background="#FFFFFF"
        android:elevation="4dp">

        <Button
            android:id="@+id/btnPrevPage"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginEnd="4dp"
            android:text="◀ Prev"
            android:background="#FF5722"
            android:textColor="#FFFFFF" />

        <TextView
            android:id="@+id/tvPageInfo"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="1 / 1"
            android:textAlignment="center"
            android:textSize="16sp"
            android:textStyle="bold"
            android:gravity="center" />

        <Button
            android:id="@+id/btnNextPage"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="4dp"
            android:text="Next ▶"
            android:background="#FF9800"
            android:textColor="#FFFFFF" />

    </LinearLayout>

    <!-- Zoom Controls -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="8dp"
        android:background="#F0F0F0">

        <Button
            android:id="@+id/btnZoomOut"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginEnd="4dp"
            android:text="🔍- Zoom Out"
            android:background="#607D8B"
            android:textColor="#FFFFFF" />

        <Button
            android:id="@+id/btnZoomIn"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="4dp"
            android:text="🔍+ Zoom In"
            android:background="#4CAF50"
            android:textColor="#FFFFFF" />

    </LinearLayout>

</LinearLayout>
```

## 🎯 All Loading Methods

### Load from Assets
```java
pdfView.fromAsset("sample.pdf").load();
```

### Load from File
```java
File pdfFile = new File("/path/to/document.pdf");
pdfView.fromFile(pdfFile).load();
```

### Load from URI (Local Content)
```java
Uri pdfUri = Uri.parse("content://path/to/document.pdf");
pdfView.fromUri(pdfUri).load();
```

### 🆕 Load from URL (Remote Server) - NEW in v1.0.10!
```java
// Load from HTTP/HTTPS URL with download progress
pdfView.fromUrl("https://example.com/document.pdf")
    .onDownloadProgress(new OnDownloadProgressListener() {
        @Override
        public void onDownloadProgress(long bytesDownloaded, long totalBytes, int progress) {
            // Update progress bar or show download status
            if (progress >= 0) {
                Log.d("PDF", "Download progress: " + progress + "%");
            } else {
                Log.d("PDF", "Downloaded: " + (bytesDownloaded / 1024) + " KB");
            }
        }
    })
    .onLoad(nbPages -> {
        // PDF downloaded and loaded successfully
        Toast.makeText(this, "PDF loaded: " + nbPages + " pages", Toast.LENGTH_SHORT).show();
    })
    .onError(error -> {
        // Handle download or loading errors
        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
    });
    // Note: fromUrl() automatically calls load() after download
```

### Load from Byte Array
```java
byte[] pdfBytes = getPdfBytes(); // Your method to get PDF bytes
pdfView.fromBytes(pdfBytes).load();
```

### Load from InputStream
```java
InputStream pdfStream = getAssets().open("sample.pdf");
pdfView.fromStream(pdfStream).load();
```

## ⚙️ Configuration Options

### All Available Options

```java
pdfView.fromAsset("sample.pdf")
    // Navigation
    .enableSwipe(true)                          // Enable swipe navigation
    .swipeHorizontal(false)                     // false = vertical, true = horizontal
    .enableDoubletap(true)                      // Enable double-tap zoom
    .defaultPage(0)                             // Starting page (0-based)
    .pages(0, 2, 1, 3, 3, 3)                  // Custom page order
    
    // Display
    .enableAntialiasing(true)                   // Smooth rendering
    .setNightMode(false)                        // Night mode (inverted colors)
    .useBestQuality(true)                       // ARGB_8888 vs RGB_565
    .fitPolicy(PDFView.FitPolicy.WIDTH)         // WIDTH, HEIGHT, or BOTH
    
    // NEW: Advanced Display Options
    .enableAnnotationRendering(true)            // Render annotations (comments, forms)
    .scrollHandle(customScrollView)             // Custom scroll handle view
    .spacing(10)                                // Spacing between pages in dp
    .autoSpacing(false)                         // Dynamic spacing to fit pages
    .pageFitPolicy(PDFView.FitPolicy.WIDTH)     // Individual page fit policy
    .fitEachPage(false)                         // Fit each page individually
    
    // Zoom
    .enableZoom(true)                           // Enable zoom functionality
    .setMinZoom(0.5f)                          // Minimum zoom level
    .setMaxZoom(5.0f)                          // Maximum zoom level
    
    // Performance
    .setCacheSize(10)                           // Number of pages to cache (NEW!)
    .enableHardwareAcceleration(true)          // Hardware acceleration
    
    // Listeners
    .onLoad(loadCompleteListener)               // PDF loaded callback
    .onPageChange(pageChangeListener)           // Page changed callback
    .onError(errorListener)                     // Error callback
    
    .load();
```

### Fit Policies

```java
// Fit to width (recommended for most cases)
.fitPolicy(PDFView.FitPolicy.WIDTH)

// Fit to height
.fitPolicy(PDFView.FitPolicy.HEIGHT)

// Fit both dimensions
.fitPolicy(PDFView.FitPolicy.BOTH)
```

### Quality Settings

```java
// Best quality (more memory usage)
.useBestQuality(true)   // Uses ARGB_8888

// Memory optimized (less memory usage)
.useBestQuality(false)  // Uses RGB_565
```

### NEW: Advanced Configuration Options

#### Annotation Rendering
```java
// Enable rendering of PDF annotations (comments, forms, highlights)
.enableAnnotationRendering(true)   // Default: true

// Disable annotations for faster rendering
.enableAnnotationRendering(false)
```

#### Page Spacing
```java
// Add spacing between pages (in dp)
.spacing(10)                       // 10dp spacing

// Dynamic spacing to fit each page on screen
.autoSpacing(true)                 // Automatically adjust spacing
```

#### Individual Page Fitting
```java
// Fit each page individually vs scaling relative to largest page
.fitEachPage(true)                 // Each page fits the view
.pageFitPolicy(PDFView.FitPolicy.WIDTH)  // How to fit individual pages

// Scale all pages relative to the largest page (default)
.fitEachPage(false)
```

#### Custom Scroll Handle
```java
// Add a custom scroll handle view
View customScrollHandle = new MyCustomScrollView(context);
.scrollHandle(customScrollHandle)

// Remove scroll handle
.scrollHandle(null)
```

#### Cache Management (NEW in v1.0.9!)
```java
// Set cache size (number of pages to keep in memory)
.setCacheSize(15)                      // Cache 15 pages (default: 10)

// For memory-constrained devices
.setCacheSize(5)                       // Smaller cache

// For high-end devices
.setCacheSize(20)                      // Larger cache

// Get current cache size
int currentCacheSize = pdfView.getCacheSize();
```

## 🌐 Remote PDF Loading (NEW in v1.0.10!)

### Supported URL Sources
- ✅ **HTTP/HTTPS URLs**: Direct PDF file links
- ✅ **Google Drive**: Public PDF files (use direct download links)
- ✅ **Dropbox**: Public PDF files (use direct download links)
- ✅ **AWS S3**: Public PDF files
- ✅ **Any Web Server**: That serves PDF files

### Google Drive Integration
```java
// For Google Drive, use the direct download link format:
// https://drive.google.com/uc?export=download&id=FILE_ID

String googleDriveFileId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms";
String googleDriveUrl = "https://drive.google.com/uc?export=download&id=" + googleDriveFileId;

pdfView.fromUrl(googleDriveUrl)
    .onDownloadProgress((bytesDownloaded, totalBytes, progress) -> {
        // Show download progress
        updateProgressBar(progress);
    })
    .onLoad(nbPages -> {
        // PDF loaded from Google Drive
        showSuccess("Google Drive PDF loaded: " + nbPages + " pages");
    })
    .onError(error -> {
        // Handle errors (network, file not found, etc.)
        showError("Failed to load from Google Drive: " + error.getMessage());
    });
```

### Dropbox Integration
```java
// For Dropbox, replace 'www.dropbox.com' with 'dl.dropboxusercontent.com'
// and remove '?dl=0' parameter

String dropboxUrl = "https://dl.dropboxusercontent.com/s/abc123/document.pdf";

pdfView.fromUrl(dropboxUrl)
    .onDownloadProgress((bytesDownloaded, totalBytes, progress) -> {
        Log.d("PDF", "Dropbox download: " + progress + "%");
    })
    .load(); // fromUrl() automatically calls load()
```

### AWS S3 Integration
```java
// For AWS S3, use the direct object URL
String s3Url = "https://your-bucket.s3.amazonaws.com/path/to/document.pdf";

pdfView.fromUrl(s3Url)
    .onDownloadProgress((bytesDownloaded, totalBytes, progress) -> {
        // Update UI with download progress
        runOnUiThread(() -> {
            progressBar.setProgress(progress);
            statusText.setText("Downloading: " + progress + "%");
        });
    })
    .onLoad(nbPages -> {
        // Hide progress bar and show PDF
        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, "S3 PDF loaded successfully", Toast.LENGTH_SHORT).show();
    })
    .onError(error -> {
        progressBar.setVisibility(View.GONE);
        showErrorDialog("S3 Download Failed", error.getMessage());
    });
```

### Network Requirements
Add these permissions to your app's `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

For HTTP URLs (not HTTPS), also add:
```xml
<application
    android:usesCleartextTraffic="true">
    <!-- Your app content -->
</application>
```

### Error Handling for Remote Loading
```java
pdfView.fromUrl("https://example.com/document.pdf")
    .onError(error -> {
        if (error instanceof java.net.UnknownHostException) {
            showError("No internet connection");
        } else if (error instanceof java.net.SocketTimeoutException) {
            showError("Download timeout - file too large or slow connection");
        } else if (error instanceof java.io.FileNotFoundException) {
            showError("PDF file not found at URL");
        } else if (error.getMessage().contains("HTTP error code: 403")) {
            showError("Access denied - check file permissions");
        } else if (error.getMessage().contains("HTTP error code: 404")) {
            showError("PDF file not found");
        } else {
            showError("Download failed: " + error.getMessage());
        }
    });
```

## 🎮 Programmatic Control

### Navigation Methods

```java
// Jump to specific page
pdfView.jumpTo(pageIndex);                      // Without animation
pdfView.jumpTo(pageIndex, true);                // With animation

// Get current page
int currentPage = pdfView.getCurrentPage();

// Get total pages
int totalPages = pdfView.getPageCount();
```

### Zoom Methods (NEW in v1.0.11!)

```java
// Set zoom level programmatically
pdfView.zoomTo(2.0f);                          // Without animation
pdfView.zoomWithAnimation(2.0f);               // With smooth animation

// Get current zoom level
float currentZoom = pdfView.getZoom();         // Returns current scale factor

// Reset zoom to default (1.0x)
pdfView.resetZoom();                           // Instant reset
pdfView.resetZoomWithAnimation();              // Smooth reset animation

// Set zoom limits
pdfView.setMinZoom(0.5f);                      // Minimum zoom level
pdfView.setMaxZoom(5.0f);                      // Maximum zoom level
```

### 🔍 Understanding Zoom vs Scale

**Zoom Methods** (`zoomTo`, `getZoom`, `resetZoom`):
- Control the **user zoom level** (`scaleFactor`) applied on top of the base fit policy
- Range from `minZoom` (default 0.5x) to `maxZoom` (default 5.0x)
- Used for user interactions like pinch-to-zoom, double-tap zoom
- `getZoom()` returns the current zoom multiplier (1.0 = no zoom)

**Scale/Matrix Methods** (internal):
- Handle the **base scaling** to fit PDF pages to the view size
- Controlled by `fitPolicy` (WIDTH, HEIGHT, BOTH)
- Combined with zoom factor for final display scaling
- `updateMatrixScale()` calculates final transformation matrix

**Example**:
```java
// If fit policy scales PDF to 0.8x to fit screen width
// And user zooms to 2.0x
// Final display scale = 0.8 × 2.0 = 1.6x

pdfView.fitPolicy(PDFView.FitPolicy.WIDTH);  // Base scaling: ~0.8x
pdfView.zoomTo(2.0f);                        // User zoom: 2.0x
// Result: PDF displays at 1.6x total scale
```

### Utility Methods

```java
// Check if PDF is loaded
boolean isLoaded = pdfView.isLoaded();

// Get document info
int pageCount = pdfView.getPageCount();
float pageWidth = pdfView.getPageWidth(pageIndex);
float pageHeight = pdfView.getPageHeight(pageIndex);

// Memory management
pdfView.recycle();                             // Clean up resources
```

## 🎨 Customization

### Night Mode

```java
// Enable night mode (inverted colors)
pdfView.setNightMode(true);

// Toggle night mode
boolean isNightMode = pdfView.isNightMode();
pdfView.setNightMode(!isNightMode);
```

### Custom Page Order

```java
// Load specific pages in custom order
pdfView.fromAsset("sample.pdf")
    .pages(0, 2, 1, 3, 3, 3)  // Pages: 1st, 3rd, 2nd, 4th, 4th, 4th
    .load();
```

### Performance Tuning

```java
// Adjust cache size based on available memory
int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024 / 1024 / 8); // MB/8
pdfView.setCacheSize(Math.max(5, Math.min(cacheSize, 20)));

// Enable hardware acceleration for better performance
pdfView.enableHardwareAcceleration(true);

// Use memory-optimized quality for large documents
pdfView.useBestQuality(false);
```

## 📋 Requirements

### Minimum Requirements
- **Android API Level**: 24+ (Android 7.0)
- **Target SDK**: 34+ (for 16KB compatibility)
- **NDK Version**: 28.0.0+ (for 16KB alignment)
- **Java Version**: 8+ (Java 11+ recommended)
- **Gradle**: 8.0+
- **Android Gradle Plugin**: 8.0.2+

### Permissions
No special permissions required! The library uses Android's native `PdfRenderer` which doesn't require external storage permissions for assets or app-internal files.

### ProGuard/R8
No additional ProGuard rules needed. The library is fully compatible with code obfuscation.

## 📋 Version History & Features

### 🎯 v1.0.11 - Latest (2025-10-10) - ZOOM & DISPLAY FIXES
- **🔍 Enhanced Zoom Experience**: Fixed zoom anchoring from center instead of top-left
- **📱 PDF Centering**: Fixed PDF display centering and proper screen fitting
- **⚡ New Zoom Method**: Added `resetZoomWithAnimation()` for smooth zoom reset
- **📐 Matrix Improvements**: Better scaling and translation calculations

### 🌐 v1.0.10 (2025-10-09) - REMOTE PDF LOADING
- **🌐 Remote PDF Support**: Load PDFs from HTTP/HTTPS URLs
- **📥 Download Progress**: Track download progress with `OnDownloadProgressListener`
- **🔗 Cloud Integration**: Support for Google Drive, Dropbox, AWS S3
- **🌐 Network Permissions**: Added internet and network state permissions

### 🚨 v1.0.9 (2025-10-09) - CRITICAL BUG FIXES
- **🚨 CRITICAL FIX**: Resolved recycled bitmap crash that caused app crashes
- **📦 Missing Method**: Added `setCacheSize()` method that was missing
- **🛡️ Safe Drawing**: Enhanced error handling and bitmap lifecycle management
- **🔧 Compilation**: Fixed duplicate variable declarations

### ⚙️ v1.0.8 (2025-10-09) - ADVANCED CONFIGURATION
- **🎨 Annotation Control**: `enableAnnotationRendering()` method
- **📜 Custom Scroll Handle**: `scrollHandle()` for custom scroll indicators
- **📏 Page Spacing**: `spacing()` and `autoSpacing()` methods
- **📄 Individual Page Fitting**: `pageFitPolicy()` and `fitEachPage()` methods

### ✅ v1.0.7 (2025-09-29) - STABLE FOUNDATION
- **16KB Compatibility**: Full Android 15+ compatibility
- **Core PDF Features**: Loading, rendering, navigation, zoom
- **Performance**: LRU caching, hardware acceleration
- **Gestures**: Pinch-to-zoom, double-tap, swipe navigation

## 🚨 Critical Bug Fixes in v1.0.9

### ⚠️ IMPORTANT: Update from v1.0.7/v1.0.8 Immediately!

**Version 1.0.9 fixes critical crashes:**

#### Fixed: Recycled Bitmap Crash
```
FATAL EXCEPTION: Canvas: trying to use a recycled bitmap
at com.alamin5g.pdf.PDFView.onDraw(PDFView.java:185)
```

**Root Cause**: Bitmap was being recycled while still in use by the drawing thread.

**Fix Applied**:
- Added `!bitmap.isRecycled()` checks in `onDraw()`
- Improved bitmap lifecycle management
- Safe bitmap replacement in rendering thread
- Enhanced error handling with try-catch blocks

#### Added: Missing setCacheSize() Method
```java
// This method was missing in v1.0.7, causing crashes for users
pdfView.setCacheSize(10);  // Now available!
```

**Migration from v1.0.7/v1.0.8**:
```gradle
// OLD (has critical bugs)
implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:1.0.7'

// NEW (stable and safe)
implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:1.0.11'
```

## 🔧 Troubleshooting

### Common Issues

**1. PDF not loading from assets:**
```java
// Make sure the PDF file is in src/main/assets/ folder
// Check the file name and path
pdfView.fromAsset("folder/sample.pdf").load(); // If in subfolder
```

**2. Memory issues with large PDFs:**
```java
// Reduce cache size and use memory-optimized quality
pdfView.setCacheSize(5)
    .useBestQuality(false)
    .load();
```

**3. 16KB compatibility issues:**
```gradle
// Ensure NDK version 28.0.0+ in build.gradle
ndk {
    version "28.0.0"
}

// Add 16KB alignment arguments
externalNativeBuild {
    cmake {
        arguments "-DANDROID_PAGE_SIZE_AGNOSTIC=ON"
    }
}
```

**4. Zoom not working:**
```java
// Make sure zoom is enabled
pdfView.enableZoom(true)
    .enableDoubletap(true)
    .load();
```

## 📊 Performance Tips

### Memory Optimization
```java
// For large documents
pdfView.fromAsset("large_document.pdf")
    .setCacheSize(5)                    // Reduce cache size
    .useBestQuality(false)              // Use RGB_565 instead of ARGB_8888
    .enableHardwareAcceleration(true)   // Use GPU acceleration
    .load();
```

### Smooth Scrolling
```java
// For smooth scrolling experience
pdfView.fromAsset("document.pdf")
    .enableAntialiasing(true)           // Smooth edges
    .enableHardwareAcceleration(true)   // GPU acceleration
    .setCacheSize(10)                   // Adequate cache
    .load();
```

### Battery Optimization
```java
// For battery-conscious apps
pdfView.fromAsset("document.pdf")
    .useBestQuality(false)              // Lower quality = less processing
    .setCacheSize(3)                    // Smaller cache = less memory
    .enableHardwareAcceleration(false)  // Disable GPU if not needed
    .load();
```

## 🏆 16KB Compatibility Benefits

### Why 16KB Matters
- **Google Play Requirement**: Mandatory for Android 15+ devices starting November 1st, 2025
- **Performance**: Better memory alignment improves performance on modern devices
- **Future-Proof**: Ensures compatibility with upcoming Android versions
- **No Crashes**: Eliminates native library alignment crashes

### Migration from Other Libraries
```java
// From barteksc/android-pdf-viewer
// OLD (Not 16KB compatible):
// implementation 'com.github.barteksc:android-pdf-viewer:3.2.0-beta.1'

// NEW (16KB compatible):
implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:1.0.7'

// API is similar, just change import:
// OLD: import com.github.barteksc.pdfviewer.PDFView;
// NEW: import com.alamin5g.pdf.PDFView;
```

## 📄 License

```
MIT License

Copyright (c) 2025 Alamin5G

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📞 Support

- **GitHub Issues**: [Report bugs or request features](https://github.com/alamin5G/Alamin5G-PDF-Viewer/issues)
- **JitPack**: [Library status and builds](https://jitpack.io/#alamin5g/Alamin5G-PDF-Viewer)

---

**Made with ❤️ by [Alamin5G](https://github.com/alamin5G)**

**⭐ If this library helped you, please give it a star on GitHub!**