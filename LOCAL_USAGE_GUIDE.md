# Local Usage Guide: Alamin5G PDF Viewer

## How to Use Alamin5G PDF Viewer Locally

Instead of using JitPack URL, you can include the library directly in your project as a local module.

### Step 1: Download the Library

```bash
# Clone the repository
git clone https://github.com/alamin5G/Alamin5G-PDF-Viewer.git

# Or download ZIP from GitHub and extract
```

### Step 2: Copy the Library Module

Copy the `alamin5g-pdf-viewer` folder from the downloaded repository into your Android project's root directory.

```
YourProject/
├── app/
├── alamin5g-pdf-viewer/    ← Copy this folder here
├── build.gradle
└── settings.gradle
```

### Step 3: Include Module in settings.gradle

Add these lines to your project's `settings.gradle`:

```gradle
include ':alamin5g-pdf-viewer'
project(':alamin5g-pdf-viewer').projectDir = new File(rootProject.projectDir, 'alamin5g-pdf-viewer')
```

### Step 4: Add Dependency in app/build.gradle

In your app's `build.gradle`, add the dependency:

```gradle
dependencies {
    implementation project(':alamin5g-pdf-viewer')
    
    // Your other dependencies...
}
```

### Step 5: Configure 16KB Compatibility

Ensure your app's `build.gradle` has 16KB compatibility:

```gradle
android {
    compileSdk 36
    
    defaultConfig {
        targetSdk 36
        
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

### Step 6: Update Imports

Use the correct imports in your Java/Kotlin files:

```java
import com.alamin5g.pdf.PDFView;
import com.alamin5g.pdf.listener.OnLoadCompleteListener;
import com.alamin5g.pdf.listener.OnPageChangeListener;
import com.alamin5g.pdf.listener.OnErrorListener;
```

### Step 7: Sync and Build

1. Click "Sync Now" in Android Studio
2. Build your project: `./gradlew assembleDebug`

## Usage Example

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
            Log.d("PDF", "Page: " + page + "/" + pageCount);
        }
    })
    .onError(error -> {
        Log.e("PDF", "Error: " + error.getMessage());
    })
    .load();
```

## Benefits of Local Usage

✅ **No Internet Required**: Works offline  
✅ **Full Control**: Modify library code if needed  
✅ **No JitPack Issues**: Avoid build server problems  
✅ **Faster Builds**: No remote dependency fetching  
✅ **16KB Compatible**: Guaranteed compatibility  

## Troubleshooting

### Module Not Found
```
Error: Project with path ':alamin5g-pdf-viewer' could not be found
```

**Solution**: Ensure the folder path is correct in `settings.gradle`

### Import Errors
```
Error: cannot find symbol class PDFView
```

**Solution**: Check imports and sync project

### Build Errors
```
Error: Duplicate class found
```

**Solution**: Clean and rebuild project:
```bash
./gradlew clean
./gradlew assembleDebug
```

---

**Alamin5G PDF Viewer** - Local usage made simple! 🚀
