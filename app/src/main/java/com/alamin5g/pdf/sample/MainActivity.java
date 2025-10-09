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
                loadPdf();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfView != null) {
            pdfView.recycle();
        }
    }

}