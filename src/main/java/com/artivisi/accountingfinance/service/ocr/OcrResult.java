package com.artivisi.accountingfinance.service.ocr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model untuk hasil OCR dengan confidence score
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResult {

    /**
     * Teks yang dikenali dari gambar
     */
    private String text;

    /**
     * Confidence score (0-100)
     * Semakin tinggi, semakin akurat hasil OCR
     */
    private float confidence;

    /**
     * Waktu processing (ms)
     */
    private long processingTime;

    /**
     * Jumlah karakter yang dikenali
     */
    private int characterCount;

    /**
     * Jumlah baris yang dikenali
     */
    private int lineCount;

    /**
     * Flag untuk menunjukkan apakah hasil memenuhi confidence threshold
     */
    private boolean reliable;

    /**
     * Pesan error jika ada
     */
    private String errorMessage;

    /**
     * OCR engine yang digunakan
     */
    private String engine;
}
