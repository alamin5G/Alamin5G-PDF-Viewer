#!/bin/bash

echo "╔═══════════════════════════════════════════════════════════════════════════╗"
echo "║           📦 Publishing Alamin5G PDF Viewer to Local Maven                ║"
echo "╚═══════════════════════════════════════════════════════════════════════════╝"
echo ""

# Clean and build
echo "🧹 Cleaning previous builds..."
./gradlew clean --quiet

echo "🔨 Building library..."
./gradlew :library:assembleRelease --quiet

# Publish to local Maven
echo "📦 Publishing to local Maven repository..."
./gradlew :library:publishToMavenLocal

echo ""
echo "✅ Library published successfully to:"
echo "   ~/.m2/repository/com/github/alamin5g/Alamin5G-PDF-Viewer/"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📖 HOW TO USE IN YOUR APP (e.g., SokalSondhaDoa):"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "1. Add to your project's build.gradle (or settings.gradle):"
echo ""
echo "   repositories {"
echo "       mavenLocal()  // Add this FIRST"
echo "       maven { url 'https://jitpack.io' }"
echo "       google()"
echo "       mavenCentral()"
echo "   }"
echo ""
echo "2. Add dependency:"
echo ""
echo "   dependencies {"
echo "       implementation 'com.github.alamin5g:Alamin5G-PDF-Viewer:1.0.14'"
echo "   }"
echo ""
echo "3. Sync Gradle and rebuild your app"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Now you can test locally WITHOUT waiting for JitPack build!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

