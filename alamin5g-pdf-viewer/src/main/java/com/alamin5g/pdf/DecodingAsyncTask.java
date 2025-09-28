/**
 * Copyright 2024 Alamin5G
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alamin5g.pdf;

import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;

import com.alamin5g.pdf.source.DocumentSource;
import com.alamin5g.pdf.util.FitPolicy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 16KB Compatible PDF Decoding Task using Android's native PdfRenderer
 */
class DecodingAsyncTask extends AsyncTask<Void, Void, Throwable> {

    private DocumentSource documentSource;
    private String password;
    private int[] userPages;
    private PDFView pdfView;
    private PdfFile pdfFile;
    private ParcelFileDescriptor fileDescriptor;

    DecodingAsyncTask(DocumentSource documentSource, String password, int[] userPages, PDFView pdfView, Object pdfiumCore) {
        this.documentSource = documentSource;
        this.password = password;
        this.userPages = userPages;
        this.pdfView = pdfView;
    }

    @Override
    protected Throwable doInBackground(Void... params) {
        try {
            // Create a temporary file for the PDF
            File tempFile = File.createTempFile("pdf_temp", ".pdf");
            tempFile.deleteOnExit();
            
            // Copy PDF data to temporary file
            try (InputStream inputStream = documentSource.createInputStream();
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            // Open the PDF file with Android's native PdfRenderer
            fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);
            
            // Create PdfFile with 16KB compatible renderer
            pdfFile = new PdfFile(
                pdfRenderer,
                fileDescriptor,
                FitPolicy.WIDTH,
                new android.util.Size(pdfView.getWidth(), pdfView.getHeight()),
                userPages,
                true, // isVertical
                0, // spacing
                false, // autoSpacing
                false // fitEachPage
            );
            
            return null;
        } catch (Exception e) {
            return e;
        }
    }

    @Override
    protected void onPostExecute(Throwable result) {
        if (result != null) {
            pdfView.loadError(result);
        } else {
            pdfView.loadComplete(pdfFile);
        }
    }
}