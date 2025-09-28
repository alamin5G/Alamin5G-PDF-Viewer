# Publishing Guide: Alamin5G PDF Viewer

## 🚀 Publishing to JitPack for Global Distribution

### Step 1: Create GitHub Repository

1. **Create a new repository** on GitHub:
   - Repository name: `Alamin5G-PDF-Viewer`
   - Description: `16KB-compatible Android PDF library with native PdfRenderer support`
   - Make it **Public**
   - Add README: ✅
   - Add .gitignore: ✅ (Android)

2. **Clone and setup**:
   ```bash
   git clone https://github.com/alamin5g/Alamin5G-PDF-Viewer.git
   cd Alamin5G-PDF-Viewer
   ```

### Step 2: Prepare Project for Publishing

1. **Clean up the project**:
   ```bash
   # Remove unnecessary files
   rm -rf app/src/main/assets/sample.pdf
   rm -rf app/src/test
   rm -rf app/src/androidTest
   ```

2. **Update project structure**:
   - Keep only the `alamin5g-pdf-viewer` module
   - Remove the demo `app` module or make it optional
   - Update `settings.gradle` to include only the library

3. **Create proper library structure**:
   ```
   Alamin5G-PDF-Viewer/
   ├── alamin5g-pdf-viewer/          # Main library module
   │   ├── build.gradle
   │   ├── src/main/
   │   │   ├── java/com/alamin5g/pdf/
   │   │   └── AndroidManifest.xml
   │   └── proguard-rules.pro
   ├── README.md
   ├── LICENSE
   ├── settings.gradle
   └── build.gradle
   ```

### Step 3: Update settings.gradle

```gradle
rootProject.name = "Alamin5G PDF Viewer"

include ':alamin5g-pdf-viewer'
```

### Step 4: Update Root build.gradle

```gradle
// Top-level build file
plugins {
    id 'com.android.library' version '8.13.0' apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 5: Create LICENSE File

Create `LICENSE` file with Apache 2.0 license:

```
Copyright 2024 Alamin5G

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

### Step 6: Create JitPack Configuration

Create `jitpack.yml` in the root directory:

```yaml
jdk:
  - openjdk17

before_install:
  - ./gradlew clean

build:
  - ./gradlew :alamin5g-pdf-viewer:assembleRelease
  - ./gradlew :alamin5g-pdf-viewer:publishToMavenLocal

install:
  - ./gradlew :alamin5g-pdf-viewer:publishToMavenLocal
```

### Step 7: Update Library build.gradle for Publishing

Update `alamin5g-pdf-viewer/build.gradle`:

```gradle
plugins {
    id 'com.android.library'
    id 'maven-publish'
}

android {
    namespace 'com.alamin5g.pdf'
    compileSdk 34

    defaultConfig {
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        
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

    buildTypes {
        release {
            shrinkResources false
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
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
    
    publishing {
        singleVariant('release') {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.10.0'
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'

    // Use Android's native PdfRenderer for 16KB compatibility
    implementation 'androidx.core:core:1.12.0'
}

publishing {
    publications {
        release(MavenPublication) {
            from components.release
            
            groupId = 'com.github.alamin5g'
            artifactId = 'Alamin5G-PDF-Viewer'
            version = '1.0.0'
            
            pom {
                name = 'Alamin5G PDF Viewer'
                description = '16KB-compatible Android PDF library with native PdfRenderer support'
                url = 'https://github.com/alamin5g/Alamin5G-PDF-Viewer'
                
                licenses {
                    license {
                        name = 'The Apache License, Version 2.0'
                        url = 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                    }
                }
                
                developers {
                    developer {
                        id = 'alamin5g'
                        name = 'Alamin5G'
                        email = 'your-email@example.com'
                    }
                }
                
                scm {
                    connection = 'scm:git:git://github.com/alamin5g/Alamin5G-PDF-Viewer.git'
                    developerConnection = 'scm:git:ssh://github.com:alamin5g/Alamin5G-PDF-Viewer.git'
                    url = 'https://github.com/alamin5g/Alamin5G-PDF-Viewer/tree/main'
                }
            }
        }
    }
}
```

### Step 8: Commit and Push to GitHub

```bash
# Add all files
git add .

# Commit with message
git commit -m "Initial release: 16KB-compatible PDF library v1.0.0"

# Push to GitHub
git push origin main
```

### Step 9: Create Release Tag

1. **Go to GitHub repository**
2. **Click "Releases"**
3. **Click "Create a new release"**
4. **Tag version**: `v1.0.0`
5. **Release title**: `Alamin5G PDF Viewer v1.0.0 - 16KB Compatible`
6. **Description**:
   ```
   ## 🚀 Alamin5G PDF Viewer v1.0.0
   
   ### Features
   - ✅ 16KB Page Size Compatibility
   - ✅ Google Play Ready
   - ✅ Native Android PdfRenderer
   - ✅ High Performance
   - ✅ Memory Efficient
   
   ### Installation
   ```gradle
   implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:1.0.0'
   ```
   ```
7. **Publish release**

### Step 10: JitPack Auto-Build

1. **Go to [JitPack.io](https://jitpack.io)**
2. **Enter your repository**: `alamin5g/Alamin5G-PDF-Viewer`
3. **Click "Look up"**
4. **Click "Get it"** on the v1.0.0 tag
5. **Wait for build to complete** (usually 2-5 minutes)

### Step 11: Test the Published Library

Create a test project to verify the library works:

```gradle
// In test project's build.gradle
dependencies {
    implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:1.0.0'
}
```

## 📦 Usage After Publishing

### For Users Worldwide

```gradle
// Add to root build.gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

// Add to app build.gradle
dependencies {
    implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:1.0.0'
}
```

### In Code

```java
import com.alamin5g.pdf.PDFView;

PDFView pdfView = findViewById(R.id.pdfView);
pdfView.fromAsset("document.pdf")
    .enableSwipe(true)
    .enableDoubletap(true)
    .load();
```

## 🔄 Version Management

### For Future Updates

1. **Update version** in `build.gradle`:
   ```gradle
   version = '1.1.0'
   ```

2. **Commit and push**:
   ```bash
   git add .
   git commit -m "Update to v1.1.0"
   git push origin main
   ```

3. **Create new release tag**:
   - Tag: `v1.1.0`
   - JitPack will automatically build the new version

## 📊 Benefits of Publishing

### ✅ Global Distribution
- **Available worldwide** via JitPack
- **Easy installation** with single line dependency
- **Version management** with semantic versioning
- **Automatic builds** on GitHub releases

### ✅ Professional Credibility
- **Open source** and publicly available
- **Proper licensing** with Apache 2.0
- **Documentation** and examples
- **Community contributions** welcome

### ✅ Easy Maintenance
- **Single source** for all users
- **Automatic updates** via JitPack
- **Version control** with Git tags
- **Issue tracking** on GitHub

## 🎯 Next Steps

1. **Publish the library** following this guide
2. **Share with community** on Reddit, Stack Overflow
3. **Create demo app** showcasing features
4. **Write blog posts** about 16KB compatibility
5. **Monitor usage** and gather feedback
6. **Iterate and improve** based on user needs

---

**Your Alamin5G PDF Viewer will be available worldwide! 🌍**
