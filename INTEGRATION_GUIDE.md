# Integration Guide: Alamin5G PDF Viewer

Complete step-by-step guide to integrate the Alamin5G PDF Viewer library into your existing Android applications.

## 🚀 Quick Start

### Step 1: Add Repository

Add JitPack repository to your project. Choose one of the following methods:

**Method A: settings.gradle (Recommended for newer projects)**
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

**Method B: Root build.gradle (For older projects)**
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

In your app's `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:1.0.7'
}
```

### Step 3: Update Build Configuration

Ensure 16KB compatibility in your app's `build.gradle`:

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

### Step 4: Update Imports

Replace your existing PDF viewer imports:

```java
// Remove old imports
// import com.github.barteksc.pdfviewer.PDFView;
// import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
// import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;

// Add new imports
import com.alamin5g.pdf.PDFView;
import com.alamin5g.pdf.listener.OnLoadCompleteListener;
import com.alamin5g.pdf.listener.OnPageChangeListener;
import com.alamin5g.pdf.listener.OnErrorListener;
```

## 🔄 Migration from Other Libraries

### From barteksc/android-pdf-viewer

**Old Dependency:**
```gradle
implementation 'com.github.barteksc:android-pdf-viewer:3.2.0-beta.1'
```

**New Dependency:**
```gradle
implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:1.0.7'
```

**Code Changes:**
```java
// OLD CODE
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;

PDFView pdfView = findViewById(R.id.pdfView);
pdfView.fromAsset("sample.pdf")
    .enableSwipe(true)
    .swipeHorizontal(false)
    .onLoad(new OnLoadCompleteListener() {
        @Override
        public void loadComplete(int nbPages) {
            // Handle load complete
        }
    })
    .load();

// NEW CODE (16KB Compatible)
import com.alamin5g.pdf.PDFView;
import com.alamin5g.pdf.listener.OnLoadCompleteListener;
import com.alamin5g.pdf.listener.OnErrorListener;

PDFView pdfView = findViewById(R.id.pdfView);
pdfView.fromAsset("sample.pdf")
    .enableSwipe(true)
    .swipeHorizontal(false)
    .onLoad(new OnLoadCompleteListener() {
        @Override
        public void loadComplete(int nbPages) {
            // Handle load complete
        }
    })
    .onError(new OnErrorListener() {
        @Override
        public void onError(Throwable t) {
            // Handle errors
        }
    })
    .load();
```

### From MuPDF or Other Libraries

**Replace your existing PDF viewer implementation:**

```java
// Remove old PDF viewer dependencies and code
// Add Alamin5G PDF Viewer as shown above

// The API is designed to be familiar and easy to migrate to
```

## 📱 Integration Examples

### Basic Integration

**1. Add to Layout:**
```xml
<com.alamin5g.pdf.PDFView
    android:id="@+id/pdfView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

**2. Initialize in Activity:**
```java
public class PDFActivity extends AppCompatActivity {
    private PDFView pdfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        
        pdfView = findViewById(R.id.pdfView);
        loadPDF();
    }

    private void loadPDF() {
        pdfView.fromAsset("sample.pdf")
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .defaultPage(0)
            .onLoad(nbPages -> {
                Toast.makeText(this, "PDF loaded: " + nbPages + " pages", 
                    Toast.LENGTH_SHORT).show();
            })
            .onPageChange((page, pageCount) -> {
                // Update page indicator
                setTitle("Page " + (page + 1) + " of " + pageCount);
            })
            .onError(t -> {
                Toast.makeText(this, "Error: " + t.getMessage(), 
                    Toast.LENGTH_LONG).show();
            })
            .load();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfView != null) {
            pdfView.recycle();
        }
    }
}
```

### Advanced Integration with Controls

**Layout with Controls:**
```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <!-- Toolbar -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="8dp"
        android:background="#2196F3">

        <Button
            android:id="@+id/btnPrev"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="◀ Prev"
            android:textColor="#FFFFFF"
            android:background="?android:attr/selectableItemBackground" />

        <TextView
            android:id="@+id/tvPageInfo"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Page 1 of 1"
            android:textColor="#FFFFFF"
            android:textAlignment="center"
            android:gravity="center" />

        <Button
            android:id="@+id/btnNext"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Next ▶"
            android:textColor="#FFFFFF"
            android:background="?android:attr/selectableItemBackground" />

    </LinearLayout>

    <!-- PDF Viewer -->
    <com.alamin5g.pdf.PDFView
        android:id="@+id/pdfView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <!-- Bottom Controls -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="8dp"
        android:background="#F5F5F5">

        <Button
            android:id="@+id/btnZoomOut"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginEnd="4dp"
            android:text="Zoom Out" />

        <Button
            android:id="@+id/btnFitWidth"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="2dp"
            android:layout_marginEnd="2dp"
            android:text="Fit Width" />

        <Button
            android:id="@+id/btnZoomIn"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="4dp"
            android:text="Zoom In" />

    </LinearLayout>

</LinearLayout>
```

**Activity Implementation:**
```java
public class AdvancedPDFActivity extends AppCompatActivity {
    private PDFView pdfView;
    private Button btnPrev, btnNext, btnZoomIn, btnZoomOut, btnFitWidth;
    private TextView tvPageInfo;
    
    private int currentPage = 0;
    private int totalPages = 0;
    private float currentZoom = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_pdf);
        
        initViews();
        setupPDFView();
        setupControls();
    }

    private void initViews() {
        pdfView = findViewById(R.id.pdfView);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnFitWidth = findViewById(R.id.btnFitWidth);
        tvPageInfo = findViewById(R.id.tvPageInfo);
    }

    private void setupPDFView() {
        String pdfFileName = getIntent().getStringExtra("PDF_FILE");
        if (pdfFileName == null) pdfFileName = "sample.pdf";

        pdfView.fromAsset(pdfFileName)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .enableAntialiasing(true)
            .setNightMode(false)
            .useBestQuality(true)
            .fitPolicy(PDFView.FitPolicy.WIDTH)
            .defaultPage(0)
            .onLoad(new OnLoadCompleteListener() {
                @Override
                public void loadComplete(int nbPages) {
                    totalPages = nbPages;
                    currentPage = 0;
                    updateUI();
                }
            })
            .onPageChange(new OnPageChangeListener() {
                @Override
                public void onPageChanged(int page, int pageCount) {
                    currentPage = page;
                    updateUI();
                }
            })
            .onError(new OnErrorListener() {
                @Override
                public void onError(Throwable t) {
                    Toast.makeText(AdvancedPDFActivity.this, 
                        "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            })
            .load();
    }

    private void setupControls() {
        btnPrev.setOnClickListener(v -> {
            if (currentPage > 0) {
                pdfView.jumpTo(currentPage - 1, true);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                pdfView.jumpTo(currentPage + 1, true);
            }
        });

        btnZoomIn.setOnClickListener(v -> {
            currentZoom = Math.min(currentZoom * 1.25f, 5.0f);
            pdfView.zoomWithAnimation(currentZoom);
        });

        btnZoomOut.setOnClickListener(v -> {
            currentZoom = Math.max(currentZoom / 1.25f, 0.5f);
            pdfView.zoomWithAnimation(currentZoom);
        });

        btnFitWidth.setOnClickListener(v -> {
            pdfView.resetZoomWithAnimation();
            currentZoom = 1.0f;
        });
    }

    private void updateUI() {
        tvPageInfo.setText("Page " + (currentPage + 1) + " of " + totalPages);
        btnPrev.setEnabled(currentPage > 0);
        btnNext.setEnabled(currentPage < totalPages - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfView != null) {
            pdfView.recycle();
        }
    }
}
```

## 🔧 Configuration for Different Use Cases

### Document Viewer App
```java
// Optimized for document reading
pdfView.fromAsset("document.pdf")
    .enableSwipe(true)
    .swipeHorizontal(false)           // Vertical scrolling
    .enableDoubletap(true)            // Double-tap to zoom
    .enableAntialiasing(true)         // Smooth text
    .useBestQuality(true)             // High quality
    .fitPolicy(PDFView.FitPolicy.WIDTH)
    .setNightMode(false)              // Normal colors
    .setCacheSize(10)                 // Cache 10 pages
    .load();
```

### Magazine/Comic Reader
```java
// Optimized for visual content
pdfView.fromAsset("magazine.pdf")
    .enableSwipe(true)
    .swipeHorizontal(true)            // Horizontal page flipping
    .enableDoubletap(true)
    .enableAntialiasing(true)
    .useBestQuality(true)             // Important for images
    .fitPolicy(PDFView.FitPolicy.BOTH)
    .setCacheSize(5)                  // Fewer pages for large images
    .load();
```

### Memory-Constrained Devices
```java
// Optimized for low memory
pdfView.fromAsset("document.pdf")
    .enableSwipe(true)
    .swipeHorizontal(false)
    .enableDoubletap(false)           // Disable to save memory
    .enableAntialiasing(false)        // Disable for performance
    .useBestQuality(false)            // Use RGB_565
    .fitPolicy(PDFView.FitPolicy.WIDTH)
    .setCacheSize(3)                  // Minimal cache
    .load();
```

### Presentation Mode
```java
// Optimized for presentations
pdfView.fromAsset("presentation.pdf")
    .enableSwipe(false)               // Disable swipe for controlled navigation
    .swipeHorizontal(true)
    .enableDoubletap(false)           // Disable zoom
    .enableAntialiasing(true)
    .useBestQuality(true)
    .fitPolicy(PDFView.FitPolicy.BOTH)
    .defaultPage(0)
    .load();
```

## 🛠️ Advanced Features

### Custom Page Loading
```java
// Load specific pages only
int[] customPages = {0, 2, 4, 6}; // Pages 1, 3, 5, 7
pdfView.fromAsset("document.pdf")
    .pages(customPages)
    .load();
```

### Dynamic Configuration
```java
// Configure based on device capabilities
boolean isHighEndDevice = isHighEndDevice();
boolean isTablet = isTablet();

pdfView.fromAsset("document.pdf")
    .useBestQuality(isHighEndDevice)
    .setCacheSize(isHighEndDevice ? 15 : 5)
    .enableAntialiasing(isHighEndDevice)
    .fitPolicy(isTablet ? PDFView.FitPolicy.HEIGHT : PDFView.FitPolicy.WIDTH)
    .load();
```

### Error Handling
```java
pdfView.fromAsset("document.pdf")
    .onError(new OnErrorListener() {
        @Override
        public void onError(Throwable t) {
            Log.e("PDF", "Error loading PDF", t);
            
            // Show user-friendly error message
            if (t instanceof FileNotFoundException) {
                showError("PDF file not found");
            } else if (t instanceof SecurityException) {
                showError("Cannot access PDF file");
            } else if (t instanceof OutOfMemoryError) {
                showError("PDF too large for device memory");
            } else {
                showError("Error loading PDF: " + t.getMessage());
            }
        }
    })
    .load();
```

## 📋 Checklist for Integration

### Pre-Integration
- [ ] Check minimum SDK version (24+)
- [ ] Verify target SDK version (34+ for 16KB)
- [ ] Update NDK version to 28.0.0+
- [ ] Add JitPack repository
- [ ] Add library dependency

### During Integration
- [ ] Update imports from old PDF library
- [ ] Add PDFView to layout
- [ ] Implement basic PDF loading
- [ ] Add error handling
- [ ] Test with sample PDF

### Post-Integration
- [ ] Test on different devices
- [ ] Test with various PDF sizes
- [ ] Verify 16KB compatibility
- [ ] Test memory usage
- [ ] Add ProGuard rules if needed

### Testing Checklist
- [ ] PDF loads correctly
- [ ] Page navigation works
- [ ] Zoom functionality works
- [ ] Memory usage is acceptable
- [ ] No crashes on rotation
- [ ] Error handling works
- [ ] 16KB alignment verified

## 🚨 Common Issues and Solutions

### Issue: PDF not loading
**Solution:**
```java
// Check file path and add error handling
pdfView.fromAsset("sample.pdf")
    .onError(t -> {
        Log.e("PDF", "Error: " + t.getMessage(), t);
        // Check if file exists in assets folder
    })
    .load();
```

### Issue: Out of memory errors
**Solution:**
```java
// Reduce memory usage
pdfView.fromAsset("large_document.pdf")
    .useBestQuality(false)      // Use RGB_565
    .setCacheSize(3)            // Reduce cache
    .enableAntialiasing(false)  // Disable for performance
    .load();
```

### Issue: Slow performance
**Solution:**
```java
// Optimize for performance
pdfView.fromAsset("document.pdf")
    .enableHardwareAcceleration(true)
    .setCacheSize(8)
    .useBestQuality(false)
    .load();
```

### Issue: 16KB compatibility errors
**Solution:**
```gradle
// Ensure proper configuration in build.gradle
android {
    defaultConfig {
        ndk {
            version "28.0.0"  // Must be 28.0.0 or higher
        }
    }
    
    packagingOptions {
        jniLibs {
            useLegacyPackaging = false
        }
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

## 📞 Support

If you encounter any issues during integration:

1. **Check the documentation** - Most common issues are covered here
2. **Review the sample code** - Complete working examples are provided
3. **Check GitHub Issues** - [Report bugs or ask questions](https://github.com/alamin5G/Alamin5G-PDF-Viewer/issues)
4. **Verify 16KB configuration** - Ensure all build.gradle settings are correct

## 🎯 Next Steps

After successful integration:

1. **Customize the UI** to match your app's design
2. **Add additional features** like bookmarks, search, annotations
3. **Optimize performance** based on your specific use case
4. **Test thoroughly** on various devices and PDF types
5. **Consider contributing** improvements back to the library

---

**Happy coding! 🚀**