
# Alamin5G PDF Viewer

A powerful, **16KB-compatible** Android PDF library featuring animations, gestures, zoom, and double-tap support. Built with Android's native `PdfRenderer` API for full 16KB page size compatibility required by Google Play.

## 🚀 Key Features

### ✅ 16KB Page Size Compatibility
- **Full 16KB Alignment Support**: Built with Android's native `PdfRenderer` API
- **Google Play Ready**: Meets all 16KB page size requirements for Android 15+
- **No Native Library Conflicts**: Eliminates problematic `.so` files
- **Future Proof**: Compatible with upcoming Android versions

### ✅ Core PDF Functionality
- **PDF Rendering**: High-quality PDF page rendering
- **Zoom Support**: Pinch-to-zoom and programmatic zoom
- **Page Navigation**: Swipe gestures and page jumping
- **Fit Policies**: Width, height, and both fitting options
- **Night Mode**: Inverted colors for dark theme support

### ✅ Performance Optimizations
- **Memory Efficient**: Optimized bitmap rendering
- **Smooth Scrolling**: Hardware-accelerated rendering
- **Caching**: Intelligent page caching system
- **Background Rendering**: Non-blocking page rendering

## 📦 Installation

### Step 1: Add Repository

Add JitPack repository to your root `build.gradle`:

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2: Add Dependency

Include the library in your app-level `build.gradle`:

```groovy
dependencies {
    implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:1.0.0'
}
```

### Step 3: 16KB Compatibility Configuration

Ensure your app's `build.gradle` has 16KB compatibility:

```groovy
android {
    compileSdk 34
    
    defaultConfig {
        targetSdk 34
        
        // 16KB Page Size Compatibility
        ndk {
            version "28.0.0"
        }
    }
    
    // 16KB Page Size Compatibility Configuration
    packagingOptions {
        jniLibs {
            useLegacyPackaging = false
        }
        // Exclude problematic libraries
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

## 📱 Usage

### Include PDFView in Your Layout

Add the `PDFView` component to your XML layout:

```xml
<com.alamin5g.pdf.PDFView
    android:id="@+id/pdfView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```

### Load a PDF File

You can load a PDF file using various methods:

```java
import com.alamin5g.pdf.PDFView;
import com.alamin5g.pdf.listener.OnLoadCompleteListener;
import com.alamin5g.pdf.listener.OnPageChangeListener;

PDFView pdfView = findViewById(R.id.pdfView);

pdfView.fromAsset("sample.pdf") // Load from assets
    .fromFile(File file) // Load from a file
    .fromUri(Uri uri) // Load from a URI
    .fromBytes(byte[] bytes) // Load from byte array
    .fromStream(InputStream stream) // Load from InputStream
    .pages(0, 2, 1, 3, 3, 3) // Specify which pages to display
    .enableSwipe(true) // Enable swipe for page navigation
    .swipeHorizontal(false) // Set swipe direction
    .enableDoubletap(true) // Enable double-tap to zoom
    .defaultPage(0) // Set the default page to display
    .onLoad(new OnLoadCompleteListener() {
        @Override
        public void loadComplete(int nbPages) {
            // Handle loading completion
            Log.d("PDF", "Loaded " + nbPages + " pages");
        }
    })
    .onPageChange(new OnPageChangeListener() {
        @Override
        public void onPageChanged(int page, int pageCount) {
            // Handle page changes
            Log.d("PDF", "Page changed to: " + page);
        }
    })
    .onError(error -> {
        // Handle errors
        Log.e("PDF", "Error: " + error.getMessage());
    })
    .load(); // Trigger the loading
```

### Complete Example

```java
public class MainActivity extends AppCompatActivity {
    private PDFView pdfView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        pdfView = findViewById(R.id.pdfView);
        
        pdfView.fromAsset("document.pdf")
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .defaultPage(0)
            .onLoad(new OnLoadCompleteListener() {
                @Override
                public void loadComplete(int nbPages) {
                    // PDF loaded successfully
                    Toast.makeText(MainActivity.this, 
                        "PDF loaded with " + nbPages + " pages", 
                        Toast.LENGTH_SHORT).show();
                }
            })
            .onError(error -> {
                // Handle loading errors
                Toast.makeText(MainActivity.this, 
                    "Failed to load PDF: " + error.getMessage(), 
                    Toast.LENGTH_LONG).show();
            })
            .load();
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

## 🔧 Configuration Options

### Zoom Settings
```java
pdfView.setMinZoom(1.0f);    // Minimum zoom level
pdfView.setMidZoom(1.75f);   // Medium zoom level
pdfView.setMaxZoom(3.0f);    // Maximum zoom level
```

### Quality Settings
```java
pdfView.useBestQuality(true);  // Use ARGB_8888 (higher quality, more memory)
pdfView.useBestQuality(false); // Use RGB_565 (lower quality, less memory)
```

### Night Mode
```java
pdfView.setNightMode(true);  // Enable night mode (inverted colors)
pdfView.setNightMode(false); // Disable night mode
```

## 🚀 16KB Compatibility Benefits

### Why Choose Alamin5G PDF Viewer?

1. **Google Play Compliance**: Meets all 16KB page size requirements
2. **Future Proof**: Compatible with Android 15+ and upcoming versions
3. **No Warnings**: Clean APK analysis without 16KB alignment warnings
4. **Native Performance**: Uses Android's optimized PDF rendering engine
5. **Smaller APK**: No heavy third-party native libraries
6. **Better Security**: Leverages Android's secure PDF rendering

### Migration from Other Libraries

**From AndroidPdfViewer:**
```java
// Before
import com.ymg.pdf.viewer.PDFView;

// After
import com.alamin5g.pdf.PDFView;
```

**From PdfiumAndroid:**
```gradle
// Before
implementation 'com.github.barteksc:pdfium-android:1.9.0'

// After
implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:1.0.0'
```

## 📋 Requirements

- **Android API Level**: 24+ (Android 7.0)
- **Target SDK**: 34+ (Android 14)
- **Compile SDK**: 34+ (Android 14)
- **NDK Version**: 28.0.0+ (for 16KB compatibility)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built upon the excellent work of the Android PDF rendering community
- Uses Android's native `PdfRenderer` API for optimal performance
- Inspired by the need for 16KB page size compatibility

---

**Alamin5G PDF Viewer** - Your complete solution for 16KB-compatible PDF rendering on Android! 🚀

© 2024 Alamin5G. All rights reserved.

