# Integration Guide: Alamin5G PDF Viewer

## Quick Start for Existing Apps

### Step 1: Add Repository

Add JitPack repository to your root `build.gradle`:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
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

### Step 4: Update Imports

Replace existing PDF library imports:

**Before:**
```java
import com.ymg.pdf.viewer.PDFView;
import com.github.iamyashchouhan.AndroidPdfViewer.PDFView;
```

**After:**
```java
import com.alamin5g.pdf.PDFView;
```

### Step 5: Update Package References

**Before:**
```java
com.ymg.pdf.viewer.PDFView
```

**After:**
```java
com.alamin5g.pdf.PDFView
```

## Migration Examples

### General Migration Pattern

**Before (Any PDF Library):**
```java
// Old imports
import com.ymg.pdf.viewer.PDFView;
import com.ymg.pdf.viewer.listener.OnLoadCompleteListener;
import com.ymg.pdf.viewer.listener.OnPageChangeListener;

private PDFView pdfView;

private void loadPdf() {
    String pdfFile = "document.pdf";
    pdfView.fromAsset(pdfFile)
        .enableSwipe(true)
        .swipeHorizontal(false)
        .enableDoubletap(true)
        .defaultPage(0)
        .onLoad(new OnLoadCompleteListener() {
            @Override
            public void loadComplete(int nbPages) {
                totalPages = nbPages;
                updateUiElements();
            }
        })
        .onPageChange(new OnPageChangeListener() {
            @Override
            public void onPageChanged(int page, int pageCount) {
                currentPage = page;
                updateUiElements();
            }
        })
        .load();
}
```

**After (Alamin5G PDF Viewer):**
```java
// New imports
import com.alamin5g.pdf.PDFView;
import com.alamin5g.pdf.listener.OnLoadCompleteListener;
import com.alamin5g.pdf.listener.OnPageChangeListener;

private PDFView pdfView;

private void loadPdf() {
    String pdfFile = "document.pdf";
    pdfView.fromAsset(pdfFile)
        .enableSwipe(true)
        .swipeHorizontal(false)
        .enableDoubletap(true)
        .defaultPage(0)
        .onLoad(new OnLoadCompleteListener() {
            @Override
            public void loadComplete(int nbPages) {
                totalPages = nbPages;
                updateUiElements();
            }
        })
        .onPageChange(new OnPageChangeListener() {
            @Override
            public void onPageChanged(int page, int pageCount) {
                currentPage = page;
                updateUiElements();
            }
        })
        .load();
}
```

## Build Configuration Updates

### Root build.gradle

**Before:**
```gradle
dependencies {
    classpath 'com.android.tools.build:gradle:7.3.1'
}
```

**After:**
```gradle
dependencies {
    classpath 'com.android.tools.build:gradle:8.13.0'
}
```

### App build.gradle

**Before:**
```gradle
android {
    compileSdk 33
    
    defaultConfig {
        targetSdk 33
    }
    
    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
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

**After:**
```gradle
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

## Testing the Integration

### 1. Build Test

```bash
./gradlew clean
./gradlew assembleDebug
```

### 2. 16KB Compatibility Test

```bash
# Check APK for 16KB alignment
./gradlew assembleDebug
# APK should not show 16KB alignment warnings
```

### 3. Functionality Test

Test these features:
- ✅ PDF loading from assets
- ✅ Page navigation (swipe, buttons)
- ✅ Zoom functionality
- ✅ Page change callbacks
- ✅ Error handling

### 4. Performance Test

Monitor:
- Memory usage during PDF rendering
- Smooth scrolling performance
- Zoom responsiveness
- Page loading speed

## Troubleshooting

### Common Issues

#### 1. Import Errors
```
Error: cannot find symbol class PDFView
```

**Solution:**
```java
// Ensure correct import
import com.alamin5g.pdf.PDFView;
```

#### 2. Build Errors
```
Error: Could not find method packagingOptions()
```

**Solution:**
```gradle
// Ensure proper Android Gradle Plugin version
dependencies {
    classpath 'com.android.tools.build:gradle:8.13.0'
}
```

#### 3. 16KB Alignment Warnings
```
APK is not compatible with 16 KB devices
```

**Solution:**
```gradle
// Ensure proper packaging configuration
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
```

#### 4. PDF Loading Issues
```
Error loading PDF: No implementation found
```

**Solution:**
```java
// Ensure proper error handling
pdfView.fromAsset("sample.pdf")
    .onError(error -> {
        Log.e("PDF", "Loading error: " + error.getMessage());
        // Handle error gracefully
    })
    .load();
```

## Benefits After Migration

### ✅ 16KB Compatibility
- **Google Play Ready**: Meets Android 15+ requirements
- **Future Proof**: Compatible with upcoming Android versions
- **No Warnings**: Clean APK analysis results

### ✅ Performance Improvements
- **Native Rendering**: Uses Android's optimized PDF renderer
- **Memory Efficient**: Better memory management
- **Smooth Performance**: Hardware-accelerated rendering

### ✅ Simplified Dependencies
- **No Third-Party Libraries**: Eliminates external dependencies
- **Smaller APK**: Reduced APK size
- **Better Security**: Uses Android's secure PDF rendering

### ✅ Maintainability
- **Single Source**: One library for all PDF needs
- **Easy Updates**: Simple library updates
- **Consistent API**: Familiar API across all apps

## Next Steps

1. **Test Integration**: Verify all functionality works
2. **Update Documentation**: Update app-specific documentation
3. **Deploy**: Release updated apps to Google Play
4. **Monitor**: Monitor performance and user feedback
5. **Optimize**: Fine-tune based on usage patterns

---

**Alamin5G PDF Viewer** - Your complete solution for 16KB-compatible PDF rendering! 🚀
