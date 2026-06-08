package com.artivisi.accountingfinance.service.ocr;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

/**
 * Service interface untuk Optical Character Recognition (OCR)
 * Mendukung multiple OCR engines (Tesseract 4, Google Cloud Vision)
 */
public interface OcrService {

    /**
     * Melakukan ekstraksi teks dari file gambar
     *
     * @param imageFile file gambar (JPG, PNG, TIFF, BMP)
     * @return teks yang dikenali dari gambar
     * @throws IOException jika gagal membaca file
     */
    String extractText(MultipartFile imageFile) throws IOException;

    /**
     * Melakukan ekstraksi teks dari file gambar dengan confidence score
     *
     * @param imageFile file gambar
     * @return result yang berisi teks dan confidence score
     * @throws IOException jika gagal membaca file
     */
    OcrResult extractTextWithConfidence(MultipartFile imageFile) throws IOException;

    /**
     * Melakukan ekstraksi teks terstruktur (per baris/paragraf) dari gambar
     *
     * @param imageFile file gambar
     * @return list of text lines
     * @throws IOException jika gagal membaca file
     */
    List<String> extractLines(MultipartFile imageFile) throws IOException;

    /**
     * Melakukan ekstraksi teks dengan preprocessing (rotate, denoise, threshold)
     *
     * @param imageFile file gambar
     * @param preprocessingConfig konfigurasi preprocessing
     * @return teks yang dikenali
     * @throws IOException jika gagal membaca file
     */
    String extractTextWithPreprocessing(MultipartFile imageFile, OcrPreprocessingConfig preprocessingConfig) throws IOException;

    /**
     * Mengekstrak nilai spesifik (invoice number, amount, date) dari receipt/invoice
     *
     * @param imageFile file gambar
     * @return data yang terstruktur
     * @throws IOException jika gagal membaca file
     */
    ReceiptData extractReceiptData(MultipartFile imageFile) throws IOException;

    /**
     * Health check untuk OCR service
     *
     * @return true jika service siap, false jika tidak
     */
    boolean isHealthy();

    /**
     * Mendapatkan nama OCR engine yang digunakan
     *
     * @return nama engine
     */
    String getEngineName();
}
