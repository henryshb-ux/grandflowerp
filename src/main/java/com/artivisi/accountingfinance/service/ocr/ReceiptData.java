package com.artivisi.accountingfinance.service.ocr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Model untuk data terstruktur yang diekstrak dari receipt/invoice
 * Digunakan untuk ekstraksi field spesifik dari dokumen
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptData {

    /**
     * Nomor invoice/receipt
     */
    private String invoiceNumber;

    /**
     * Nama vendor/supplier
     */
    private String vendorName;

    /**
     * Tanggal transaksi
     */
    private LocalDate transactionDate;

    /**
     * Jumlah total
     */
    private BigDecimal totalAmount;

    /**
     * Jumlah pajak
     */
    private BigDecimal taxAmount;

    /**
     * Daftar item/line items
     */
    private List<LineItem> lineItems;

    /**
     * Teks mentah dari OCR (fallback jika parsing gagal)
     */
    private String rawText;

    /**
     * Confidence score untuk hasil parsing (0-100)
     */
    private float confidence;

    /**
     * Pesan error jika ada
     */
    private String errorMessage;

    /**
     * Model untuk item baris dalam receipt
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItem {
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String unit;
    }
}
