# Local Usage Guide: Alamin5G PDF Viewer

## 🎉 GOOD NEWS: Global Release Available!

**The Alamin5G PDF Viewer is now successfully published and available globally via JitPack!**

[![JitPack](https://jitpack.io/v/alamin5g/Alamin5G-PDF-Viewer.svg)](https://jitpack.io/#alamin5g/Alamin5G-PDF-Viewer)

### ✅ Recommended: Use Global Release

```gradle
dependencies {
    implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:1.0.7'
}
```

**Benefits of using the global release:**
- ✅ **Easy Integration**: Just add one line to your dependencies
- ✅ **Automatic Updates**: Get updates without manual copying
- ✅ **Tested & Stable**: Version 1.0.7 is thoroughly tested
- ✅ **16KB Compatible**: Fully compatible with Google Play requirements
- ✅ **No Local Files**: Keeps your project clean

---

## 🛠️ Alternative: Local Integration

**However, if you still prefer local integration**, this guide explains how to integrate the library directly into your Android project as a local module.

### When to Use Local Integration

- **Development & Testing**: When contributing to the library
- **Custom Modifications**: When you need to modify the library source
- **Offline Development**: When working without internet access
- **Corporate Environments**: When external dependencies are restricted
- **Learning Purpose**: When you want to understand the library internals

### Step 1: Download the Library

**Option A: Clone the Repository (Recommended)**
```bash
git clone https://github.com/alamin5G/Alamin5G-PDF-Viewer.git
```

**Option B: Download ZIP**
1. Go to [Alamin5G-PDF-Viewer GitHub page](https://github.com/alamin5G/Alamin5G-PDF-Viewer)
2. Click "Code" → "Download ZIP"
3. Extract the ZIP file

### Step 2: Copy the Library Module

Copy the `library` folder from the downloaded repository into your Android project's root directory.

**Your project structure should look like:**
```
YourAndroidProject/
├── app/
├── library/                    ← Copied from Alamin5G-PDF-Viewer
│   ├── build.gradle
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── alamin5g/
│   │       │           └── pdf/
│   │       │               ├── PDFView.java
│   │       │               └── listener/
│   │       └── res/
├── build.gradle
├── settings.gradle
└── ...
```

### Step 3: Include the Module in settings.gradle

Open your project's `settings.gradle` file and add:

```gradle
include ':library'
project(':library').projectDir = new File(rootProject.projectDir, 'library')
```

**Complete settings.gradle example:**
```gradle
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Your Project Name"
include ':app'
include ':library'  // Add this line
project(':library').projectDir = new File(rootProject.projectDir, 'library')  // Add this line
```

### Step 4: Add Dependency in App Module

Open your app's `build.gradle` file and add the local dependency:

```gradle
dependencies {
    // ... other dependencies ...
    implementation project(':library')
}
```

**Complete app/build.gradle example:**
```gradle
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.yourpackage.yourapp'
    compileSdk 34

    defaultConfig {
        applicationId "com.yourpackage.yourapp"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"

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
        excludes += [
            '**/libc++_shared.so',
            '**/libjniPdfium.so',
            '**/libmodft2.so',
            '**/libmodpdfium.so',
            '**/libmodpng.so'
        ]
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.7.1'
    implementation 'com.google.android.material:material:1.13.0'
    implementation 'androidx.activity:activity:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.2.1'
    
    // Local Alamin5G PDF Viewer library
    implementation project(':library')
    
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.3.0'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.7.0'
}
```

### Step 5: Sync and Build

1. **Sync Project**: Click "Sync Project with Gradle Files" in Android Studio
2. **Clean Build**: Run `./gradlew clean` in terminal
3. **Build Project**: Run `./gradlew assembleDebug` to verify everything compiles

### Step 6: Use the Library

Now you can use the library exactly like the global version:

```java
import com.alamin5g.pdf.PDFView;
import com.alamin5g.pdf.listener.OnLoadCompleteListener;
import com.alamin5g.pdf.listener.OnPageChangeListener;
import com.alamin5g.pdf.listener.OnErrorListener;

public class MainActivity extends AppCompatActivity {
    private PDFView pdfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        pdfView = findViewById(R.id.pdfView);
        
        pdfView.fromAsset("sample.pdf")
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .defaultPage(0)
            .onLoad(new OnLoadCompleteListener() {
                @Override
                public void loadComplete(int nbPages) {
                    Toast.makeText(MainActivity.this, 
                        "PDF loaded: " + nbPages + " pages", 
                        Toast.LENGTH_SHORT).show();
                }
            })
            .onPageChange(new OnPageChangeListener() {
                @Override
                public void onPageChanged(int page, int pageCount) {
                    // Handle page change
                }
            })
            .onError(new OnErrorListener() {
                @Override
                public void onError(Throwable t) {
                    Toast.makeText(MainActivity.this, 
                        "Error: " + t.getMessage(), 
                        Toast.LENGTH_LONG).show();
                }
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

## 🔧 Local Development Benefits

### Advantages of Local Integration

1. **Full Source Access**: Modify the library as needed
2. **Debugging**: Step through library code during debugging
3. **Custom Features**: Add your own features to the library
4. **No Network Dependency**: Works completely offline
5. **Version Control**: Include library changes in your project's version control

### Development Workflow

1. **Make Changes**: Modify the library source code in the `library` folder
2. **Test Immediately**: Changes are reflected immediately in your app
3. **Debug**: Set breakpoints in library code
4. **Contribute**: Submit pull requests with your improvements

### Customization Examples

**Custom Color Scheme:**
```java
// In library/src/main/java/com/alamin5g/pdf/PDFView.java
// Add custom color methods
public PDFView setCustomBackgroundColor(int color) {
    setBackgroundColor(color);
    return this;
}
```

**Custom Loading Animation:**
```java
// Add your own loading animation
public PDFView setLoadingAnimation(View loadingView) {
    // Your custom implementation
    return this;
}
```

## 🚨 Troubleshooting Local Setup

### Common Issues and Solutions

**Issue 1: "Project with path ':library' could not be found"**
```gradle
// Solution: Check settings.gradle
include ':library'
project(':library').projectDir = new File(rootProject.projectDir, 'library')
```

**Issue 2: Compilation Errors**
```gradle
// Solution: Ensure compatible versions
android {
    compileSdk 34
    defaultConfig {
        minSdk 24
        targetSdk 34
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

**Issue 3: 16KB Compatibility Issues**
```gradle
// Solution: Add proper NDK and packaging configuration
defaultConfig {
    ndk {
        version "28.0.0"
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
```

**Issue 4: Build Failures**
```bash
# Solution: Clean and rebuild
./gradlew clean
./gradlew assembleDebug
```

## 📊 Comparison: Global vs Local

| Feature | Global Release | Local Integration |
|---------|----------------|-------------------|
| **Setup Time** | ⚡ 2 minutes | 🕐 10-15 minutes |
| **Maintenance** | ✅ Automatic | 🔧 Manual updates |
| **Customization** | ❌ Limited | ✅ Full control |
| **Debugging** | ⚠️ Limited | ✅ Full access |
| **Project Size** | ✅ Smaller | 📦 Larger |
| **Offline Work** | ❌ Needs internet | ✅ Fully offline |
| **Updates** | ✅ Easy | 🔄 Manual |
| **Stability** | ✅ Tested | ⚠️ Your responsibility |

## 🎯 Recommendation

**For most developers**: Use the **global release** (`implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:1.0.7'`)

**Use local integration only if you:**
- Need to modify the library source code
- Are contributing to the library development
- Have specific corporate requirements
- Want to learn how the library works internally

---

## 📞 Support

If you encounter issues with local integration:

1. **Check this guide** - Most issues are covered here
2. **Compare with global version** - Ensure your setup matches the working global release
3. **GitHub Issues** - [Report problems](https://github.com/alamin5G/Alamin5G-PDF-Viewer/issues)
4. **Sample Project** - Use the included sample app as reference

**Remember**: The global release (v1.0.7) is thoroughly tested and recommended for production use! 🚀