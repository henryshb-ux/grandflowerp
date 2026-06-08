# Tesseract 4 OCR Integration Guide

Panduan lengkap untuk mengintegrasikan Tesseract 4 OCR ke aplikasi GrandFlowerP.

## 📋 Daftar Isi

1. [Instalasi Tesseract](#instalasi-tesseract)
2. [Konfigurasi Spring Boot](#konfigurasi-spring-boot)
3. [API Endpoints](#api-endpoints)
4. [Fitur-fitur](#fitur-fitur)
5. [Contoh Penggunaan](#contoh-penggunaan)
6. [Troubleshooting](#troubleshooting)
7. [Performance Optimization](#performance-optimization)

## Instalasi Tesseract

### Linux (Ubuntu/Debian)

```bash
# Install Tesseract 4
sudo apt-get update
sudo apt-get install -y tesseract-ocr

# Install language packs (Bahasa Indonesia, Inggris)
sudo apt-get install -y tesseract-ocr-ind tesseract-ocr-eng

# Verify installation
tesseract --version
```

**Tesseract Data Path:** `/usr/share/tesseract-ocr/4.00/tessdata`

### macOS

```bash
# Install via Homebrew
brew install tesseract

# Install language packs
brew install tesseract-lang

# Verify installation
tesseract --version
```

**Tesseract Data Path:** `/usr/local/share/tessdata`

### Windows

1. Download installer dari: https://github.com/UB-Mannheim/tesseract/wiki
2. Run installer (recommended: install ke `C:\Program Files\Tesseract-OCR`)
3. Select language packs saat install
4. Set environment variable:
   - `TESSDATA_PREFIX=C:\Program Files\Tesseract-OCR\tessdata`

**Tesseract Data Path:** `C:\Program Files\Tesseract-OCR\tessdata`

### Docker

```dockerfile
FROM openjdk:25-slim

# Install Tesseract
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-ind \
    tesseract-ocr-eng && \
    rm -rf /var/lib/apt/lists/*

# Copy application
COPY target/accounting-finance-*.jar app.jar

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Konfigurasi Spring Boot

### 1. Maven Dependency (sudah ditambahkan di pom.xml)

```xml
<!-- Tesseract 4 OCR -->
<dependency>
    <groupId>net.sourceforge.tess4j</groupId>
    <artifactId>tess4j</artifactId>
    <version>5.12.0</version>
</dependency>

<!-- Image processing -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-imaging</artifactId>
    <version>1.0.0-alpha3</version>
</dependency>

<!-- Image manipulation -->
<dependency>
    <groupId>net.coobird</groupId>
    <artifactId>thumbnailator</artifactId>
    <version>0.4.20</version>
</dependency>
```

### 2. Application Properties (application.yml)

```yaml
ocr:
  tesseract:
    enabled: true
    language: eng           # Default language
  confidence:
    threshold: 70          # Minimum confidence score
  temp-dir: /tmp/ocr       # Temporary directory for processing
```

### 3. Spring Beans Configuration

OCR service sudah dikonfigurasi sebagai Spring Bean:
- `TesseractOcrService` - Main OCR service
- `OcrImagePreprocessor` - Image preprocessing utility
- `OcrController` - REST API endpoints

## API Endpoints

### 1. Extract Text

**Endpoint:** `POST /api/v1/ocr/extract-text`

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/ocr/extract-text \
  -F "file=@receipt.jpg" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "text": "INVOICE 123456\nDate: 2026-06-08\n...",
  "processingTime": 2500,
  "characterCount": 1245
}
```

### 2. Extract with Confidence Score

**Endpoint:** `POST /api/v1/ocr/extract-with-confidence`

**Response:**
```json
{
  "success": true,
  "data": {
    "text": "INVOICE 123456\n...",
    "confidence": 87.5,
    "processingTime": 2500,
    "characterCount": 1245,
    "lineCount": 12,
    "reliable": true,
    "engine": "Tesseract 4"
  }
}
```

### 3. Extract with Preprocessing

**Endpoint:** `POST /api/v1/ocr/extract-with-preprocessing`

**Request Body:**
```json
{
  "autoRotate": true,
  "denoise": true,
  "binaryThreshold": 150,
  "scale": 1.5,
  "brightness": 10,
  "contrast": 20,
  "dilation": false,
  "erosion": false,
  "grayscale": true,
  "language": "eng+ind",
  "pageSegmentationMode": 3
}
```

### 4. Extract Receipt Data

**Endpoint:** `POST /api/v1/ocr/extract-receipt-data`

**Response:**
```json
{
  "success": true,
  "data": {
    "invoiceNumber": "INV-2026-001",
    "vendorName": "PT Example Supplier",
    "transactionDate": "2026-06-08",
    "totalAmount": 500000.00,
    "taxAmount": 50000.00,
    "lineItems": [
      {
        "description": "Product A",
        "quantity": 2,
        "unitPrice": 100000.00,
        "totalPrice": 200000.00,
        "unit": "pcs"
      }
    ],
    "confidence": 78.5
  }
}
```

### 5. Health Check

**Endpoint:** `GET /api/v1/ocr/health`

**Response:**
```json
{
  "healthy": true,
  "engine": "Tesseract 4",
  "status": "RUNNING"
}
```

## Fitur-fitur

### 1. Text Extraction
- Ekstraksi teks dari berbagai format image (JPG, PNG, TIFF, BMP)
- Support multi-language recognition
- Confidence scoring

### 2. Image Preprocessing
- Auto-rotate correction
- Brightness/contrast adjustment
- Noise reduction (denoise)
- Binary thresholding
- Dilation/Erosion (morphological operations)
- Image scaling/zoom
- Grayscale conversion

### 3. Receipt/Invoice Data Extraction
- Ekstraksi nomor invoice
- Ekstraksi vendor name
- Ekstraksi tanggal transaksi
- Ekstraksi total amount
- Ekstraksi line items
- Structured data output

### 4. Quality Metrics
- Confidence scoring (0-100)
- Character count
- Line count
- Processing time tracking
- Reliability flagging

## Contoh Penggunaan

### Java Integration

```java
@Service
public class GoodsReceiptService {

    private final OcrService ocrService;

    public void importReceiptFromImage(MultipartFile imageFile) throws IOException {
        // Extract text
        String text = ocrService.extractText(imageFile);
        
        // Extract structured data
        ReceiptData receiptData = ocrService.extractReceiptData(imageFile);
        
        // Create GoodsReceipt dari extracted data
        GoodsReceipt gr = new GoodsReceipt();
        gr.setGrNumber(receiptData.getInvoiceNumber());
        gr.setReceiptDate(receiptData.getTransactionDate());
        gr.setDeliveryNote(receiptData.getVendorName());
        gr.setNotes(text);
        
        // Save ke database
        goodsReceiptRepository.save(gr);
    }
}
```

### Thymeleaf Frontend Integration

```html
<!-- Image upload form -->
<form id="ocrForm" enctype="multipart/form-data">
    <input type="file" id="imageFile" name="file" accept="image/*">
    <button type="button" onclick="extractText()">Scan Document</button>
</form>

<!-- Results display -->
<div id="ocrResults" style="display:none;">
    <h3>Extracted Text:</h3>
    <pre id="extractedText"></pre>
    <p>Confidence: <span id="confidence"></span>%</p>
</div>

<script>
function extractText() {
    const fileInput = document.getElementById('imageFile');
    const formData = new FormData();
    formData.append('file', fileInput.files[0]);
    
    fetch('/api/v1/ocr/extract-with-confidence', {
        method: 'POST',
        body: formData,
        headers: {
            'Authorization': 'Bearer ' + getToken()
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            document.getElementById('extractedText').textContent = data.data.text;
            document.getElementById('confidence').textContent = 
                data.data.confidence.toFixed(1);
            document.getElementById('ocrResults').style.display = 'block';
        } else {
            alert('Error: ' + data.error);
        }
    })
    .catch(error => console.error('Error:', error));
}
</script>
```

## Troubleshooting

### Issue: "Tesseract data path not found"

**Solution:**
```properties
# Set explicit path di application.yml atau environment variable
export TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00/tessdata
```

### Issue: Poor OCR accuracy

**Solution:**
```java
// Use preprocessing dengan konfigurasi optimal
OcrPreprocessingConfig config = OcrPreprocessingConfig.builder()
    .autoRotate(true)
    .denoise(true)
    .binaryThreshold(150)
    .scale(2.0f)  // Zoom 2x
    .grayscale(true)
    .language("eng+ind")  // Multi-language
    .build();

String text = ocrService.extractTextWithPreprocessing(imageFile, config);
```

### Issue: Slow processing

**Solution:**
- Optimalkan image size (resize sebelum OCR)
- Gunakan binary threshold untuk documents dengan background clear
- Reduce language count (jika possible)
- Implement caching untuk repeated OCR tasks

### Issue: Memory out of bounds

**Solution:**
```properties
# Increase JVM heap size
-Xmx2048m

# Implement image size limits di controller
if (imageFile.getSize() > 50 * 1024 * 1024) {
    throw new IOException("File too large");
}
```

## Performance Optimization

### 1. Caching

```java
@Service
@CacheConfig(cacheNames = "ocrResults")
public class CachedOcrService {

    @Cacheable(key = "#imageHash")
    public OcrResult extractTextWithCache(String imageHash, MultipartFile file) {
        // OCR processing - hasil akan di-cache
    }
}
```

### 2. Async Processing

```java
@Service
public class AsyncOcrService {

    @Async
    public CompletableFuture<OcrResult> extractTextAsync(MultipartFile imageFile) {
        OcrResult result = ocrService.extractTextWithConfidence(imageFile);
        return CompletableFuture.completedFuture(result);
    }
}
```

### 3. Batch Processing

```java
@PostMapping("/batch-ocr")
public ResponseEntity<List<OcrResult>> batchOcr(@RequestParam("files") MultipartFile[] files) {
    return ResponseEntity.ok(
        Arrays.stream(files)
            .parallel()  // Parallel processing
            .map(file -> {
                try {
                    return ocrService.extractTextWithConfidence(file);
                } catch (IOException e) {
                    return OcrResult.builder().errorMessage(e.getMessage()).build();
                }
            })
            .collect(Collectors.toList())
    );
}
```

### 4. Image Optimization

```java
// Reduce image size sebelum OCR
BufferedImage optimized = Thumbnails.of(image)
    .size(1000, 1000)  // Max 1000x1000
    .outputQuality(0.85)
    .asBufferedImage();

String text = tesseract.doOCR(optimized);
```

## Monitoring & Logging

```java
// Health check endpoint
@GetMapping("/api/v1/ocr/health")
public ResponseEntity<OcrHealthStatus> health() {
    return ResponseEntity.ok(OcrHealthStatus.builder()
        .healthy(ocrService.isHealthy())
        .engine(ocrService.getEngineName())
        .avgProcessingTime(getAverageProcessingTime())
        .successRate(getSuccessRate())
        .build());
}

// Metrics
@Timed(value = "ocr.extraction.time")
public String extractText(MultipartFile imageFile) {
    // Automatic timing metrics collection
}
```

## Integrasi dengan GoodsReceipt

Tambahkan fitur OCR ke GoodsReceiptController:

```java
@PostMapping("/import-from-receipt-image")
public ResponseEntity<GoodsReceipt> importFromReceiptImage(
    @RequestParam("file") MultipartFile receiptImage,
    @RequestParam UUID poId) throws IOException {
    
    // Extract receipt data
    ReceiptData data = ocrService.extractReceiptData(receiptImage);
    
    // Create GoodsReceipt
    GoodsReceipt gr = new GoodsReceipt();
    gr.setGrNumber(generateGrNumber());
    gr.setPurchaseOrder(purchaseOrderRepository.findById(poId).get());
    gr.setReceiptDate(data.getTransactionDate());
    gr.setNotes(data.getRawText());
    
    // Save
    return ResponseEntity.ok(goodsReceiptRepository.save(gr));
}
```

## References

- [Tesseract Official](https://github.com/UB-Mannheim/tesseract/wiki)
- [Tess4J Documentation](https://github.com/nguyenq/tess4j)
- [OCR Best Practices](https://tesseract-ocr.github.io/tessdoc/)
- [Image Preprocessing for OCR](https://github.com/tesseract-ocr/tesseract/wiki/Improve-quality)

---

**Last Updated:** June 2026
**Maintained by:** GrandFlowerP Development Team
