package com.alamin5g.pdf.test;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btnReadAsset, btnReadUrl, btnTestFeatures;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnReadAsset = findViewById(R.id.btnReadAsset);
        btnReadUrl = findViewById(R.id.btnReadUrl);
        btnTestFeatures = findViewById(R.id.btnTestFeatures);

        // Read PDF from Assets
        btnReadAsset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ReadActivity.class);
                intent.putExtra("PDF_SOURCE", "ASSET");
                intent.putExtra("PDF_FILE", "ALAMIN5G_PDF_VIEWER_16KB_GUIDE.pdf");
                startActivity(intent);
            }
        });

        // Read PDF from URL
        btnReadUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ReadActivity.class);
                intent.putExtra("PDF_SOURCE", "URL");
                intent.putExtra("PDF_URL", "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf");
                startActivity(intent);
            }
        });

        // Test All Features
        btnTestFeatures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ReadActivity.class);
                intent.putExtra("PDF_SOURCE", "FEATURES_TEST");
                startActivity(intent);
            }
        });
    }
}
