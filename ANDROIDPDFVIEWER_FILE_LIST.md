# AndroidPdfViewer - Complete File Structure

**Total Files**: 39 Java files

## Core Classes (7 files)
1. **PDFView.java** - Main view class
2. **PdfFile.java** - PDF document management
3. **CacheManager.java** - Page caching system
4. **RenderingHandler.java** - Background rendering
5. **PagesLoader.java** - Page loading logic
6. **DecodingAsyncTask.java** - Async PDF decoding
7. **DragPinchManager.java** - Gesture handling
8. **AnimationManager.java** - Smooth animations

## Source/Loading Classes (6 files)
9. **DocumentSource.java** - Base interface
10. **AssetSource.java** - Load from assets
11. **FileSource.java** - Load from file
12. **UriSource.java** - Load from URI
13. **ByteArraySource.java** - Load from byte array
14. **InputStreamSource.java** - Load from InputStream

## Listener Interfaces (8 files)
15. **Callbacks.java** - Callback dispatcher
16. **OnLoadCompleteListener.java** - PDF load complete
17. **OnPageChangeListener.java** - Page changed
18. **OnErrorListener.java** - Error handling
19. **OnDrawListener.java** - Drawing events
20. **OnRenderListener.java** - Rendering events
21. **OnPageScrollListener.java** - Scroll events
22. **OnTapListener.java** - Tap events
23. **OnLongPressListener.java** - Long press events
24. **OnPageErrorListener.java** - Page-specific errors

## Scroll Handle Classes (2 files)
25. **ScrollHandle.java** - Interface
26. **DefaultScrollHandle.java** - Default implementation

## Link Handling (2 files)
27. **LinkHandler.java** - Interface
28. **DefaultLinkHandler.java** - Default implementation

## Model Classes (2 files)
29. **PagePart.java** - Rendered page part
30. **LinkTapEvent.java** - Link tap event data

## Utility Classes (8 files)
31. **Util.java** - General utilities
32. **FileUtils.java** - File operations
33. **MathUtils.java** - Math calculations
34. **ArrayUtils.java** - Array operations
35. **Constants.java** - App constants
36. **FitPolicy.java** - Page fit policies (enum)
37. **SnapEdge.java** - Page snapping edges (enum)
38. **PageSizeCalculator.java** - Page size calculations

## Exception Classes (2 files)
39. **FileNotFoundException.java** - Custom exception
40. **PageRenderingException.java** - Rendering errors

## Architecture Summary

### Key Design Patterns:
- **Separation of Concerns**: Core, rendering, gestures, utilities separated
- **Source Pattern**: Multiple DocumentSource implementations for different inputs
- **Listener Pattern**: Extensive callback system for events
- **Handler Pattern**: Background rendering with RenderingHandler
- **Cache Strategy**: CacheManager for efficient memory use
- **Animation**: Dedicated AnimationManager for smooth UX

### Dependencies (from build.gradle):
- PdfiumAndroid (for PDF parsing) - **NOT 16KB compatible!**
- AndroidX libraries (compatible)

### Key Differences from Alamin5G-PDF-Viewer:
- Uses PdfiumAndroid (native library)
- More modular architecture (separate managers)
- Advanced caching with PagePart system
- Comprehensive listener system
- Animation framework
- Custom scroll handle support

