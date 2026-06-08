package com.artivisi.accountingfinance.service;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.protobuf.ByteString;
import com.artivisi.accountingfinance.config.GoogleCloudVisionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@ConditionalOnProperty(name = "google.cloud.vision.enabled", havingValue = "true")
public class VisionOcrService {

    private static final Logger log = LoggerFactory.getLogger(VisionOcrService.class);

    private final ImageAnnotatorClient imageAnnotatorClient;
    private final GoogleCloudVisionConfig config;

    public VisionOcrService(ImageAnnotatorClient imageAnnotatorClient, GoogleCloudVisionConfig config) {
        this.imageAnnotatorClient = imageAnnotatorClient;
        this.config = config;
    }

    public record OcrResult(String text, boolean success, String errorMessage) {
        public static OcrResult success(String text) {
            return new OcrResult(text, true, null);
        }

        public static OcrResult error(String message) {
            return new OcrResult(null, false, message);
        }
    }

    public OcrResult extractText(byte[] imageBytes) {
        if (!config.isEnabled() || imageAnnotatorClient == null) {
            return OcrResult.error("Google Cloud Vision is not enabled");
        }

        try {
            ByteString imgBytes = ByteString.copyFrom(imageBytes);
            Image image = Image.newBuilder().setContent(imgBytes).build();

            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                    .build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(image)
                    .build();

            BatchAnnotateImagesResponse response = imageAnnotatorClient.batchAnnotateImages(List.of(request));
            List<AnnotateImageResponse> responses = response.getResponsesList();

            if (responses.isEmpty()) {
                return OcrResult.error("No response from Vision API");
            }

            AnnotateImageResponse imageResponse = responses.getFirst();

            if (imageResponse.hasError()) {
                log.error("Vision API error: {}", imageResponse.getError().getMessage());
                return OcrResult.error(imageResponse.getError().getMessage());
            }

            TextAnnotation fullTextAnnotation = imageResponse.getFullTextAnnotation();
            String extractedText = fullTextAnnotation.getText();

            log.debug("Extracted {} characters from image", extractedText.length());
            return OcrResult.success(extractedText);

        } catch (Exception e) {
            log.error("Error extracting text from image", e);
            return OcrResult.error("OCR processing failed: " + e.getMessage());
        }
    }

    public OcrResult extractText(Path imagePath) throws IOException {
        byte[] imageBytes = Files.readAllBytes(imagePath);
        return extractText(imageBytes);
    }

    public boolean isEnabled() {
        return config.isEnabled() && imageAnnotatorClient != null;
    }
}
