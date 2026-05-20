# Alamin5G PDF Viewer 📚

[![JitPack](https://jitpack.io/v/alamin5g/Alamin5G-PDF-Viewer.svg)](https://jitpack.io/#alamin5g/Alamin5G-PDF-Viewer)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![16KB Compatible](https://img.shields.io/badge/16KB-Compatible-success.svg?style=flat)](https://developer.android.com/guide/practices/page-sizes)

**The only 16KB page size compatible PDF viewer for Android 15+.** Built with Android's native `PdfRenderer` API — zero native libraries, guaranteed Google Play compatibility.

## 🚨 What's New in v1.0.17

- ✅ **Zoom Control**: `enableZoom(boolean)` to disable pinch-to-zoom ([#5](https://github.com/alamin5G/Alamin5G-PDF-Viewer/issues/5))
- ✅ **Builder Chain Fix**: `setMinZoom()`, `setMidZoom()`, `setMaxZoom()` now return `PDFView` ([#5](https://github.com/alamin5G/Alamin5G-PDF-Viewer/issues/5))
- ✅ **7 New Listeners**: `onDraw()`, `onDrawAll()`, `onPageScroll()`, `onRender()`, `onTap()`, `onLongPress()`, `onPageError()`
- ✅ **Default Scroll Handle**: Built-in `DefaultScrollHandle` with draggable handle + page indicator ([#6](https://github.com/alamin5G/Alamin5G-PDF-Viewer/issues/6))
- ✅ **Continuous Scroll Fix**: `loadFromFile()` now respects `continuousScrollMode` ([#7](https://github.com/alamin5G/Alamin5G-PDF-Viewer/issues/7))
- ✅ **Zoom Gap Fix**: Per-page `canvas.scale()` eliminates visual gaps during pinch zoom ([#2](https://github.com/alamin5G/Alamin5G-PDF-Viewer/issues/2))

Fully backward compatible — just update the version:

```gradle
    implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:v1.0.17'
```

## 🏆 Why Choose Alamin5G PDF Viewer?

Starting **November 1st, 2025**, Google Play **REQUIRES** all apps targeting Android 15+ to support **16KB page size alignment**. Most PDF libraries (including `barteksc/AndroidPdfViewer`) use native `.so` files that are **NOT 16KB compatible**.

| Feature              | Alamin5G PDF Viewer | barteksc/AndroidPdfViewer | MuPDF       |
| -------------------- | ------------------- | ------------------------- | ----------- |
| **16KB Compatible**  | ✅ YES               | ❌ NO (native libs)        | ❌ NO        |
| **Google Play 2025** | ✅ Passes            | ❌ Rejected                | ❌ Rejected  |
| **Native Libraries** | ✅ Zero              | ❌ Multiple .so files      | ❌ .so files |
| **File Size**        | ✅ ~200 KB           | ⚠️ 20+ MB                  | ⚠️ 30+ MB    |
| **Android 15+**      | ✅ Full support      | ❌ Crashes                 | ❌ Issues    |

## 🚀 Key Features

- **PDF Rendering**: High-quality rendering with customizable quality (ARGB_8888 / RGB_565)
- **Multiple Sources**: Assets, files, URIs, bytes, streams, and remote URLs
- **Zoom Support**: Pinch-to-zoom, double-tap zoom, programmatic zoom, enable/disable toggle
- **Navigation**: Swipe gestures, page jumping with animation, continuous scroll
- **Fit Policies**: Width, height, or both
- **Night Mode**: Inverted colors for dark theme
- **Scroll Handle**: Built-in `DefaultScrollHandle` with page indicator
- **7 Listener Callbacks**: Draw, render, scroll, tap, long press, page error
- **Memory Efficient**: LRU caching, configurable cache size, lazy loading
- **16KB Compatible**: Uses native `PdfRenderer` — zero `.so` files

## 📦 Installation

### Step 1: Add Repository

Add JitPack to your `settings.gradle`:

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

### Step 2: Add Dependency

```gradle
dependencies {
    implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:1.0.17'
}
```

### Step 3: 16KB Compatibility (Optional)

If your app includes native libraries, ensure your `build.gradle` has:

```gradle
android {
    defaultConfig {
        ndk { version "28.0.0" }
    }
    packagingOptions {
        jniLibs { useLegacyPackaging = false }
    }
}
```

> **Note**: Alamin5G PDF Viewer itself has NO native libraries — this config is only needed if your app uses other native libs.

## 📱 Basic Usage

### 1. Add PDFView to Layout

```xml
<com.alamin5g.pdf.PDFView
    android:id="@+id/pdfView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### 2. Load a PDF

```java
PDFView pdfView = findViewById(R.id.pdfView);

pdfView.fromAsset("sample.pdf")
    .enableSwipe(true)
    .enableDoubletap(true)
    .defaultPage(0)
    .onLoad(nbPages -> Log.d("PDF", "Loaded " + nbPages + " pages"))
    .onPageChange((page, pageCount) -> Log.d("PDF", "Page " + (page + 1) + "/" + pageCount))
    .onError(error -> Log.e("PDF", "Error: " + error.getMessage()))
    .load(); // ⬅️ Must be called LAST
```

> **⚠️ Important**: Always call `.load()` **last** in the chain, after setting all callbacks. See [Callback Setup](#-callback-setup) below.

### 3. Complete Activity Example

```java
public class PdfActivity extends AppCompatActivity {
    private PDFView pdfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        pdfView = findViewById(R.id.pdfView);
        pdfView.fromAsset("document.pdf")
            .enableSwipe(true)
            .enableDoubletap(true)
            .fitPolicy(PDFView.FitPolicy.WIDTH)
            .onLoad(nbPages -> Toast.makeText(this, nbPages + " pages", Toast.LENGTH_SHORT).show())
            .onError(error -> Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show())
            .load();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfView != null) pdfView.recycle();
    }
}
```

## ⚠️ Callback Setup

Callbacks must be registered **before** `.load()` is called:

```java
// ✅ Correct — load() called AFTER callbacks
pdfView.fromAsset("file.pdf")
    .onLoad(nbPages -> { /* fires */ })
    .onError(error -> { /* fires */ })
    .load();

// ❌ Wrong — load() called BEFORE callbacks
pdfView.fromAsset("file.pdf").load();
pdfView.onLoad(nbPages -> { /* NEVER fires */ });
```

## 🎯 Loading Methods

```java
// From assets
pdfView.fromAsset("sample.pdf").load();

// From file
pdfView.fromFile(new File("/path/to/document.pdf")).load();

// From URI
pdfView.fromUri(uri).load();

// From byte array
pdfView.fromBytes(pdfBytes).load();

// From input stream
pdfView.fromStream(inputStream).load();

// From URL (auto-calls load after download)
pdfView.fromUrl("https://example.com/document.pdf")
    .onDownloadProgress((bytes, total, progress) -> {
        Log.d("PDF", "Download: " + progress + "%");
    })
    .onLoad(nbPages -> { /* loaded */ })
    .onError(error -> { /* handle error */ });
```

### Remote PDF Permissions

Add to `AndroidManifest.xml` for URL loading:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## ⚙️ Configuration

### All Options

```java
pdfView.fromAsset("sample.pdf")
    // Navigation
    .enableSwipe(true)                          // Enable swipe navigation
    .swipeHorizontal(false)                     // false = vertical, true = horizontal
    .enableDoubletap(true)                      // Enable double-tap zoom
    .defaultPage(0)                             // Starting page (0-based)
    .pages(0, 2, 1, 3)                         // Custom page order

    // Display
    .enableAntialiasing(true)                   // Smooth rendering
    .setNightMode(false)                        // Night mode (inverted colors)
    .useBestQuality(true)                       // ARGB_8888 vs RGB_565
    .fitPolicy(PDFView.FitPolicy.WIDTH)         // WIDTH, HEIGHT, or BOTH
    .spacing(10)                                // Spacing between pages in dp
    .autoSpacing(false)                         // Dynamic spacing to fit pages
    .pageFitPolicy(PDFView.FitPolicy.WIDTH)     // Per-page fit policy
    .fitEachPage(false)                         // Fit each page individually
    .enableAnnotationRendering(true)            // Render annotations

    // Zoom
    .enableZoom(true)                           // Enable pinch-to-zoom
    .setMinZoom(0.5f)                          // Minimum zoom level
    .setMidZoom(1.75f)                         // Middle zoom for double-tap
    .setMaxZoom(5.0f)                          // Maximum zoom level

    // Scroll Handle
    .scrollHandle(new DefaultScrollHandle(this)) // Built-in scroll handle

    // Performance
    .setCacheSize(10)                           // Pages to cache (default: 10)

    // Callbacks
    .onLoad(listener)
    .onPageChange(listener)
    .onError(listener)
    .onDownloadProgress(listener)
    .onDraw(listener)
    .onDrawAll(listener)
    .onPageScroll(listener)
    .onRender(listener)
    .onTap(listener)
    .onLongPress(listener)
    .onPageError(listener)

    .load();
```

### Fit Policies

```java
.fitPolicy(PDFView.FitPolicy.WIDTH)   // Fit to width (recommended)
.fitPolicy(PDFView.FitPolicy.HEIGHT)  // Fit to height
.fitPolicy(PDFView.FitPolicy.BOTH)    // Fit both dimensions
```

## 📖 Method Reference

### Loading

| Method                    | Description                             |
| ------------------------- | --------------------------------------- |
| `fromAsset(String)`       | Load from assets folder                 |
| `fromFile(File)`          | Load from file                          |
| `fromUri(Uri)`            | Load from URI                           |
| `fromBytes(byte[])`       | Load from byte array                    |
| `fromStream(InputStream)` | Load from input stream                  |
| `fromUrl(String)`         | Load from URL with download progress    |
| `load()`                  | **Must be called last!** Starts loading |

### Navigation

| Method                     | Description                          |
| -------------------------- | ------------------------------------ |
| `enableSwipe(boolean)`     | Enable/disable swipe navigation      |
| `swipeHorizontal(boolean)` | Set swipe direction                  |
| `defaultPage(int)`         | Set starting page (0-based)          |
| `pages(int...)`            | Load specific pages in custom order  |
| `jumpTo(int)`              | Jump to page                         |
| `jumpTo(int, boolean)`     | Jump to page with optional animation |

### Display

| Method                               | Description                     |
| ------------------------------------ | ------------------------------- |
| `enableAntialiasing(boolean)`        | Smooth rendering                |
| `setNightMode(boolean)`              | Invert colors for night reading |
| `useBestQuality(boolean)`            | ARGB_8888 vs RGB_565            |
| `fitPolicy(FitPolicy)`               | WIDTH, HEIGHT, or BOTH          |
| `spacing(int)`                       | Spacing between pages in dp     |
| `autoSpacing(boolean)`               | Dynamic spacing                 |
| `pageFitPolicy(FitPolicy)`           | Per-page fit policy             |
| `fitEachPage(boolean)`               | Fit each page individually      |
| `enableAnnotationRendering(boolean)` | Render annotations              |
| `scrollHandle(View)`                 | Custom or default scroll handle |

### Zoom

| Method                     | Description                  |
| -------------------------- | ---------------------------- |
| `enableZoom(boolean)`      | Enable/disable pinch-to-zoom |
| `enableDoubletap(boolean)` | Enable double-tap to zoom    |
| `setMinZoom(float)`        | Minimum zoom level           |
| `setMidZoom(float)`        | Middle zoom for double-tap   |
| `setMaxZoom(float)`        | Maximum zoom level           |
| `zoomTo(float)`            | Set zoom level               |
| `zoomWithAnimation(float)` | Zoom with animation          |
| `resetZoom()`              | Reset to default zoom        |
| `resetZoomWithAnimation()` | Smooth reset animation       |

### Callbacks

| Method                                           | Description                         |
| ------------------------------------------------ | ----------------------------------- |
| `onLoad(OnLoadCompleteListener)`                 | PDF loading complete                |
| `onPageChange(OnPageChangeListener)`             | Page changed                        |
| `onError(OnErrorListener)`                       | Error occurred                      |
| `onDownloadProgress(OnDownloadProgressListener)` | URL download progress               |
| `onDraw(OnDrawListener)`                         | After each page draw                |
| `onDrawAll(OnDrawAllListener)`                   | After all pages drawn               |
| `onPageScroll(OnPageScrollListener)`             | During scroll with offset           |
| `onRender(OnRenderListener)`                     | After page renders                  |
| `onTap(OnTapListener)`                           | Single tap (return bool to consume) |
| `onLongPress(OnLongPressListener)`               | Long press gesture                  |
| `onPageError(OnPageErrorListener)`               | Page-specific render error          |

### Programmatic Control

```java
// Navigation
pdfView.jumpTo(5);                    // Jump to page 6 (0-based)
pdfView.jumpTo(5, true);              // With animation
int page = pdfView.getCurrentPage();
int total = pdfView.getPageCount();

// Position
float offset = pdfView.getPositionOffset();  // 0.0 = top, 1.0 = bottom
pdfView.setPositionOffset(0.5f);             // Scroll to 50%
pdfView.moveTo(100f, 200f);                  // Absolute position
pdfView.moveRelativeTo(50f, -100f);          // Relative movement

// Zoom
pdfView.zoomTo(2.0f);
pdfView.zoomWithAnimation(2.0f);
pdfView.zoomWithAnimation(centerX, centerY, 2.0f);
pdfView.resetZoom();
pdfView.resetZoomWithAnimation();
float zoom = pdfView.getZoom();

// Scroll configuration
pdfView.pageFling(true);              // Jump to page on fling
pdfView.pageSnap(true);               // Snap to page boundaries
pdfView.stopFling();                  // Stop ongoing fling

// State checks
boolean zoomed = pdfView.isZooming();
boolean closed = pdfView.isRecycled();
boolean zoomEnabled = pdfView.isZoomEnabled();

// Page info
android.util.SizeF size = pdfView.getPageSize(pageIndex);
int page = pdfView.getPageAtPositionOffset(0.5f);
pdfView.performPageSnap();

// Cleanup
pdfView.recycle();
```

## 🔧 Default Scroll Handle

The built-in `DefaultScrollHandle` provides a draggable scroll indicator with page number bubble:

```java
import com.alamin5g.pdf.scroll.DefaultScrollHandle;

// Vertical handle (right edge, default)
pdfView.fromAsset("file.pdf")
    .scrollHandle(new DefaultScrollHandle(this))
    .load();

// Horizontal handle (bottom edge)
pdfView.fromAsset("file.pdf")
    .scrollHandle(new DefaultScrollHandle(this, true))
    .load();

// Custom scroll handle (your own View)
pdfView.fromAsset("file.pdf")
    .scrollHandle(myCustomView)
    .load();

// Remove scroll handle
pdfView.scrollHandle(null);
```

**DefaultScrollHandle features:**
- 📍 Draggable handle bar synced with scroll position
- 🔢 Page indicator bubble showing current page number
- ⏱️ Auto-hides indicator after 1.5 seconds
- ↕️ Vertical (right edge) and horizontal (bottom edge) orientations
- 🔄 No feedback loops between handle drag and PDF scroll

## 🎨 Customization

### Night Mode
```java
pdfView.setNightMode(true);
```

### Custom Page Order
```java
pdfView.fromAsset("sample.pdf")
    .pages(0, 2, 1, 3)  // Load pages in custom order
    .load();
```

### Password-Protected PDFs
```java
pdfView.password("your-pdf-password");
```

### Cache Management
```java
pdfView.setCacheSize(15);  // Cache 15 pages (default: 10)
```

## 🌐 Remote PDF Loading

Supports HTTP/HTTPS URLs, Google Drive, Dropbox, AWS S3, and any web server serving PDF files.

```java
// Google Drive — use direct download link
String url = "https://drive.google.com/uc?export=download&id=FILE_ID";
pdfView.fromUrl(url).load();

// Dropbox — replace www.dropbox.com with dl.dropboxusercontent.com
String url = "https://dl.dropboxusercontent.com/s/abc123/document.pdf";
pdfView.fromUrl(url).load();

// AWS S3
String url = "https://bucket.s3.amazonaws.com/path/to/document.pdf";
pdfView.fromUrl(url).load();
```

## 🔧 Troubleshooting

**PDF not loading from assets:**
```java
// Ensure file is in src/main/assets/ folder
pdfView.fromAsset("folder/sample.pdf").load();
```

**Memory issues with large PDFs:**
```java
pdfView.setCacheSize(5).useBestQuality(false).load();
```

**Zoom not working:**
```java
pdfView.enableZoom(true).enableDoubletap(true).load();
```

## 📋 Requirements

- **Android API**: 24+ (Android 7.0)
- **Target SDK**: 34+ (for 16KB compatibility)
- **Java**: 8+ (11+ recommended)
- **Permissions**: None for local files; `INTERNET` for URL loading
- **ProGuard**: No additional rules needed

## 📋 Version History

### v1.0.17 (2025-05-19) - ZOOM CONTROL + LISTENERS + SCROLL HANDLE + BUG FIXES
- `enableZoom(boolean)`, builder chain fix ([#5](https://github.com/alamin5G/Alamin5G-PDF-Viewer/issues/5))
- 7 new listener interfaces: `OnDraw`, `OnDrawAll`, `OnPageScroll`, `OnRender`, `OnTap`, `OnLongPress`, `OnPageError`
- `DefaultScrollHandle` with draggable handle + page indicator ([#6](https://github.com/alamin5G/Alamin5G-PDF-Viewer/issues/6))
- `loadFromFile()` respects `continuousScrollMode` ([#7](https://github.com/alamin5G/Alamin5G-PDF-Viewer/issues/7))
- Per-page `canvas.scale()` eliminates zoom gaps ([#2](https://github.com/alamin5G/Alamin5G-PDF-Viewer/issues/2))
- `canScrollVertically()` zoom fix, `getMinZoom()`, `getMidZoom()`, `isZoomEnabled()`

### v1.0.16 (2025-12-07) - CALLBACK FIX + SMOOTH ZOOM RENDERING
- Callbacks fixed: `onLoad()`, `onError()`, `onDownloadProgress()` ([#4](https://github.com/alamin5G/Alamin5G-PDF-Viewer/issues/4))
- Smart cache replacement, progressive rendering, 3-page buffer

### v1.0.15 (2025-10-16) - PAGE CHANGE CALLBACK FIX + URI PERFORMANCE
- `onPageChanged()` fires during scroll, URI loading 20x faster

### v1.0.14 (2025-10-15) - COMPLETE FEATURE PARITY
- 40+ new methods, 97% feature parity with AndroidPdfViewer
- Swipe gesture fix, jumpTo fix, position/movement methods

### v1.0.13 (2025-10-15) - CRITICAL OOM FIX
- Memory reduced from ~2 GB to ~35 MB for large PDFs
- Lazy loading, LRU eviction, virtual scrolling

### v1.0.12 (2025-10-10) - DYNAMIC HIGH-QUALITY RENDERING
- Auto re-render at zoom resolution, no pixelation

### v1.0.11 (2025-10-10) - ZOOM & DISPLAY FIXES
- Zoom anchoring fix, PDF centering, `resetZoomWithAnimation()`

### v1.0.10 (2025-10-09) - REMOTE PDF LOADING
- `fromUrl()`, download progress, cloud integration

### v1.0.9 (2025-10-09) - CRITICAL BUG FIXES
- Recycled bitmap crash fix, `setCacheSize()` added

### v1.0.8 (2025-10-09) - ADVANCED CONFIGURATION
- Annotation rendering, scroll handle, page spacing, individual page fitting

### v1.0.7 (2025-09-29) - STABLE FOUNDATION
- 16KB compatibility, core PDF features, LRU caching, gestures

## 🏆 16KB Compatibility

**Why it matters**: Google Play requires 16KB page size support for Android 15+ apps starting November 2025. Libraries with native `.so` files (barteksc/AndroidPdfViewer, MuPDF, pdf.js wrappers) will cause app rejection.

**Alamin5G PDF Viewer**:
- ✅ Uses Android's native `PdfRenderer` API — no `.so` files
- ✅ ~200 KB library size (vs 20+ MB for native alternatives)
- ✅ Works on 4KB and 16KB devices
- ✅ Compatible with Android 15, 16, and beyond

**Migration from barteksc/AndroidPdfViewer:**
```gradle
// OLD (not 16KB compatible)
// implementation 'com.github.barteksc:android-pdf-viewer:3.2.0-beta.1'

// NEW (16KB compatible)
implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:1.0.17'
```

```java
// Just change the import:
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
