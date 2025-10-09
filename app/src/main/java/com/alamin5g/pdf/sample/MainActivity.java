package com.alamin5g.pdf.sample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.alamin5g.pdf.PDFView;
import com.alamin5g.pdf.listener.OnLoadCompleteListener;
import com.alamin5g.pdf.listener.OnPageChangeListener;
import com.alamin5g.pdf.listener.OnDownloadProgressListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Alamin5G-PDF-Viewer-Sample";
    private PDFView pdfView;
    private Button btnLoadPdf;
    private Button btnNextPage;
    private Button btnPrevPage;
    private int currentPage = 0;
    private int totalPages = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        pdfView = findViewById(R.id.pdfView);
        btnLoadPdf = findViewById(R.id.btnLoadPdf);
        btnNextPage = findViewById(R.id.btnNextPage);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        
        // Initially disable navigation buttons
        btnNextPage.setEnabled(false);
        btnPrevPage.setEnabled(false);
    }

    private void setupClickListeners() {
        btnLoadPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Load PDF button clicked");
                // Demonstrate both asset loading and URL loading
                if (Math.random() > 0.5) {
                    loadPdf(); // Load from assets
                } else {
                    loadPdfFromUrl(); // Load from URL
                }
            }
        });

        btnNextPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Next button clicked. currentPage: " + currentPage + ", totalPages: " + totalPages);
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    Log.d(TAG, "Attempting to jump to page: " + currentPage);
                    pdfView.jumpTo(currentPage);
                    updateButtonStates();
                    Log.d(TAG, "Jumped to page: " + currentPage);
                } else {
                    Log.w(TAG, "Cannot go to next page - already at last page");
                }
            }
        });

        btnPrevPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Prev button clicked. currentPage: " + currentPage + ", totalPages: " + totalPages);
                if (currentPage > 0) {
                    currentPage--;
                    Log.d(TAG, "Attempting to jump to page: " + currentPage);
                    pdfView.jumpTo(currentPage);
                    updateButtonStates();
                    Log.d(TAG, "Jumped to page: " + currentPage);
                } else {
                    Log.w(TAG, "Cannot go to prev page - already at first page");
                }
            }
        });
    }

    private void loadPdf() {
        String pdfFile = "ALAMIN5G_PDF_VIEWER_16KB_GUIDE.pdf";
        Log.d(TAG, "Starting to load PDF: " + pdfFile);
        
        try {
            pdfView.fromAsset(pdfFile)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .enableAntialiasing(true)
                .defaultPage(0)
                // NEW METHODS DEMONSTRATION
                .enableAnnotationRendering(true)  // Render annotations (comments, forms)
                .scrollHandle(null)               // No custom scroll handle
                .spacing(10)                      // 10dp spacing between pages
                .autoSpacing(false)               // Don't use dynamic spacing
                .pageFitPolicy(PDFView.FitPolicy.WIDTH)  // Fit pages to width
                .fitEachPage(false)               // Don't fit each page individually
                .setCacheSize(8)                  // Cache 8 pages (NEW in v1.0.9!)
                .onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {
                        totalPages = nbPages;
                        currentPage = 0;
                        updateButtonStates();
                        Log.d(TAG, "PDF loaded successfully with " + nbPages + " pages.");
                        Toast.makeText(MainActivity.this, "PDF loaded: " + nbPages + " pages", Toast.LENGTH_SHORT).show();
                    }
                })
                .onPageChange(new OnPageChangeListener() {
                    @Override
                    public void onPageChanged(int page, int pageCount) {
                        currentPage = page;
                        updateButtonStates();
                        Log.d(TAG, "Page changed to: " + page + " of " + pageCount);
                    }
                })
                .onError(error -> {
                    Log.e(TAG, "PDF loading error: " + error.getMessage());
                    Toast.makeText(MainActivity.this, "Error loading PDF: " + error.getMessage(), Toast.LENGTH_LONG).show();
                })
                .load();
        } catch (Exception e) {
            Log.e(TAG, "Exception while loading PDF: " + e.getMessage());
            Toast.makeText(this, "Failed to load PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void updateButtonStates() {
        btnNextPage.setEnabled(currentPage < totalPages - 1);
        btnPrevPage.setEnabled(currentPage > 0);
        btnNextPage.setText("Next (" + (currentPage + 1) + "/" + totalPages + ")");
        btnPrevPage.setText("Prev (" + (currentPage + 1) + "/" + totalPages + ")");
    }

    private void loadPdfFromUrl() {
        // Example URLs for testing (you can replace with your own)
        String[] testUrls = {
            "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
            "https://www.adobe.com/support/products/enterprise/knowledgecenter/media/c4611_sample_explain.pdf",
            "https://file-examples.com/storage/fe68c8c7c66b2b9c7e0b6e4/2017/10/file-sample_150kB.pdf"
        };
        
        String url = testUrls[(int) (Math.random() * testUrls.length)];
        Log.d(TAG, "Loading PDF from URL: " + url);
        
        Toast.makeText(this, "Downloading PDF from server...", Toast.LENGTH_SHORT).show();
        
        pdfView.fromUrl(url)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .enableAntialiasing(true)
            .defaultPage(0)
            // NEW METHODS DEMONSTRATION
            .enableAnnotationRendering(true)
            .scrollHandle(null)
            .spacing(10)
            .autoSpacing(false)
            .pageFitPolicy(PDFView.FitPolicy.WIDTH)
            .fitEachPage(false)
            .setCacheSize(8)
            .onDownloadProgress(new OnDownloadProgressListener() {
                @Override
                public void onDownloadProgress(long bytesDownloaded, long totalBytes, int progress) {
                    String progressText = progress >= 0 ? 
                        "Downloading: " + progress + "%" : 
                        "Downloading: " + (bytesDownloaded / 1024) + " KB";
                    
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, progressText, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Download progress: " + progressText);
                    });
                }
            })
            .onLoad(new OnLoadCompleteListener() {
                @Override
                public void loadComplete(int nbPages) {
                    totalPages = nbPages;
                    currentPage = 0;
                    updateButtonStates();
                    Log.d(TAG, "PDF downloaded and loaded successfully with " + nbPages + " pages.");
                    Toast.makeText(MainActivity.this, "PDF downloaded: " + nbPages + " pages", Toast.LENGTH_SHORT).show();
                }
            })
            .onPageChange(new OnPageChangeListener() {
                @Override
                public void onPageChanged(int page, int pageCount) {
                    currentPage = page;
                    updateButtonStates();
                    Log.d(TAG, "Page changed to: " + page + " of " + pageCount);
                }
            })
            .onError(error -> {
                Log.e(TAG, "PDF download/loading error: " + error.getMessage());
                Toast.makeText(MainActivity.this, "Error downloading PDF: " + error.getMessage(), Toast.LENGTH_LONG).show();
            });
            // Note: fromUrl() automatically calls load() after download
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfView != null) {
            pdfView.recycle();
        }
    }

}