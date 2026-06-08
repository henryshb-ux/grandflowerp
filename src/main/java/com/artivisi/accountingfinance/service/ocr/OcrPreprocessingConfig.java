package com.artivisi.accountingfinance.service.ocr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Konfigurasi untuk image preprocessing sebelum OCR
 * Preprocessing dapat meningkatkan akurasi OCR secara signifikan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrPreprocessingConfig {

    /**
     * Auto-rotate image untuk mendeteksi dan memperbaiki rotasi
     */
    @Builder.Default
    private boolean autoRotate = true;

    /**
     * Denoise image untuk mengurangi noise
     */
    @Builder.Default
    private boolean denoise = true;

    /**
     * Threshold biner (0-255) untuk mengkonversi grayscale ke binary
     * -1 = auto threshold
     */
    @Builder.Default
    private int binaryThreshold = -1;

    /**
     * Zoom/scale factor untuk meningkatkan resolusi
     * 1.0 = no scaling, > 1.0 = zoom in
     */
    @Builder.Default
    private float scale = 1.0f;

    /**
     * Brightness adjustment (-100 hingga 100)
     * 0 = no adjustment
     */
    @Builder.Default
    private int brightness = 0;

    /**
     * Contrast adjustment (-100 hingga 100)
     * 0 = no adjustment
     */
    @Builder.Default
    private int contrast = 0;

    /**
     * Dilation untuk memperkuat karakter yang tipis
     */
    @Builder.Default
    private boolean dilation = false;

    /**
     * Erosion untuk menghilangkan noise kecil
     */
    @Builder.Default
    private boolean erosion = false;

    /**
     * Convert to grayscale sebelum processing
     */
    @Builder.Default
    private boolean grayscale = true;

    /**
     * Language hint untuk Tesseract (e.g., "eng", "ind", "eng+ind")
     * Default = "eng"
     */
    @Builder.Default
    private String language = "eng";

    /**
     * OCR page segmentation mode (Tesseract)
     * 0 = auto, 1 = single uniform block, etc.
     */
    @Builder.Default
    private int pageSegmentationMode = 3;
}
