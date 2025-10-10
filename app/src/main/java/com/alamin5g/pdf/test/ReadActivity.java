package com.alamin5g.pdf.test;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alamin5g.pdf.PDFView;
import com.alamin5g.pdf.listener.OnLoadCompleteListener;
import com.alamin5g.pdf.listener.OnPageChangeListener;
import com.alamin5g.pdf.listener.OnErrorListener;
import com.alamin5g.pdf.listener.OnDownloadProgressListener;

public class ReadActivity extends AppCompatActivity {

    private static final String TAG = "ReadActivity";
    PDFView pdfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        pdfView = findViewById(R.id.pdfView);

        String pdfSource = getIntent().getStringExtra("PDF_SOURCE");
        
        if ("ASSET".equals(pdfSource)) {
            loadFromAsset();
        } else if ("URL".equals(pdfSource)) {
            loadFromUrl();
        } else if ("FEATURES_TEST".equals(pdfSource)) {
            testAllFeatures();
        } else {
            loadFromAsset(); // Default
        }
    }

    private void loadFromAsset() {
        String pdfFile = getIntent().getStringExtra("PDF_FILE");
        if (pdfFile == null) pdfFile = "ALAMIN5G_PDF_VIEWER_16KB_GUIDE.pdf";

        Log.d(TAG, "Loading PDF from asset: " + pdfFile);
        
        pdfView.fromAsset(pdfFile)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(0)
                .enableAnnotationRendering(true)
                .scrollHandle(null)
                .enableAntialiasing(true)
                .useBestQuality(true)
                .fitPolicy(PDFView.FitPolicy.WIDTH)  // Main fit policy - fills screen width
                .spacing(0)
                .autoSpacing(false)
                .pageFitPolicy(PDFView.FitPolicy.WIDTH)
                .fitEachPage(false)  // Use main fitPolicy, not individual page fitting
                .setCacheSize(10)
                .onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {
                        Log.d(TAG, "PDF loaded successfully with " + nbPages + " pages");
                        Toast.makeText(ReadActivity.this, "PDF loaded: " + nbPages + " pages", Toast.LENGTH_SHORT).show();
                    }
                })
                .onPageChange(new OnPageChangeListener() {
                    @Override
                    public void onPageChanged(int page, int pageCount) {
                        Log.d(TAG, "Page changed to: " + (page + 1) + " of " + pageCount);
                        setTitle("Page " + (page + 1) + " of " + pageCount);
                    }
                })
                .onError(new OnErrorListener() {
                    @Override
                    public void onError(Throwable t) {
                        Log.e(TAG, "Error loading PDF: " + t.getMessage());
                        Toast.makeText(ReadActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .load();
    }

    private void loadFromUrl() {
        String pdfUrl = getIntent().getStringExtra("PDF_URL");
        if (pdfUrl == null) {
            pdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf";
        }

        Log.d(TAG, "Loading PDF from URL: " + pdfUrl);
        Toast.makeText(this, "Downloading PDF from server...", Toast.LENGTH_SHORT).show();

        pdfView.fromUrl(pdfUrl)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(0)
                .enableAnnotationRendering(true)
                .scrollHandle(null)
                .enableAntialiasing(true)
                .useBestQuality(true)
                .fitPolicy(PDFView.FitPolicy.WIDTH)  // Main fit policy - fills screen width
                .spacing(0)
                .autoSpacing(false)
                .pageFitPolicy(PDFView.FitPolicy.WIDTH)
                .fitEachPage(false)  // Use main fitPolicy, not individual page fitting
                .setCacheSize(8)
                .onDownloadProgress(new OnDownloadProgressListener() {
                    @Override
                    public void onDownloadProgress(long bytesDownloaded, long totalBytes, int progress) {
                        String progressText = progress >= 0 ? 
                            "Downloading: " + progress + "%" : 
                            "Downloaded: " + (bytesDownloaded / 1024) + " KB";
                        
                        runOnUiThread(() -> {
                            Toast.makeText(ReadActivity.this, progressText, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Download progress: " + progressText);
                        });
                    }
                })
                .onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {
                        Log.d(TAG, "PDF downloaded and loaded successfully with " + nbPages + " pages");
                        Toast.makeText(ReadActivity.this, "PDF downloaded: " + nbPages + " pages", Toast.LENGTH_SHORT).show();
                    }
                })
                .onPageChange(new OnPageChangeListener() {
                    @Override
                    public void onPageChanged(int page, int pageCount) {
                        Log.d(TAG, "Page changed to: " + (page + 1) + " of " + pageCount);
                        setTitle("URL PDF - Page " + (page + 1) + " of " + pageCount);
                    }
                })
                .onError(new OnErrorListener() {
                    @Override
                    public void onError(Throwable t) {
                        Log.e(TAG, "Error downloading/loading PDF: " + t.getMessage());
                        Toast.makeText(ReadActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
                // Note: fromUrl() automatically calls load() after download
    }

    private void testAllFeatures() {
        Log.d(TAG, "Testing all features with asset PDF");
        
        pdfView.fromAsset("ALAMIN5G_PDF_VIEWER_16KB_GUIDE.pdf")
                // Navigation
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(0)
                
                // Display options
                .enableAnnotationRendering(true)
                .scrollHandle(null)
                .enableAntialiasing(true)
                .setNightMode(false)
                .useBestQuality(true)
                .fitPolicy(PDFView.FitPolicy.WIDTH)
                
                // NEW methods
                .spacing(10)
                .autoSpacing(false)
                .pageFitPolicy(PDFView.FitPolicy.WIDTH)
                .fitEachPage(false)
                .setCacheSize(15)
                
                // Listeners
                .onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {
                        Log.d(TAG, "Features test - PDF loaded: " + nbPages + " pages");
                        Toast.makeText(ReadActivity.this, "All features loaded successfully!", Toast.LENGTH_SHORT).show();
                    }
                })
                .onPageChange(new OnPageChangeListener() {
                    @Override
                    public void onPageChanged(int page, int pageCount) {
                        Log.d(TAG, "Features test - Page: " + (page + 1) + " of " + pageCount);
                        setTitle("Features Test - Page " + (page + 1) + " of " + pageCount);
                    }
                })
                .onError(new OnErrorListener() {
                    @Override
                    public void onError(Throwable t) {
                        Log.e(TAG, "Features test error: " + t.getMessage());
                        Toast.makeText(ReadActivity.this, "Features test error: " + t.getMessage(), Toast.LENGTH_LONG).show();
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
