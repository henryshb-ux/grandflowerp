// ── PurchaseRequestStatus.java ───────────────────────────────────────────────
package com.artivisi.accountingfinance.enums;

public enum PurchaseRequestStatus {
    DRAFT("Draft"),
    SUBMITTED("Diajukan"),
    APPROVED("Disetujui"),
    REJECTED("Ditolak"),
    ORDERED("Sudah Dipesan"),
    CLOSED("Selesai"),
    CANCELLED("Dibatalkan");

    private final String label;
    PurchaseRequestStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}


// ── PurchaseRequestPriority.java ─────────────────────────────────────────────
package com.artivisi.accountingfinance.enums;

public enum PurchaseRequestPriority {
    LOW("Rendah"),
    NORMAL("Normal"),
    HIGH("Tinggi"),
    URGENT("Mendesak");

    private final String label;
    PurchaseRequestPriority(String label) { this.label = label; }
    public String getLabel() { return label; }
}


// ── PurchaseOrderStatus.java ─────────────────────────────────────────────────
package com.artivisi.accountingfinance.enums;

public enum PurchaseOrderStatus {
    DRAFT("Draft"),
    SENT("Terkirim ke Supplier"),
    CONFIRMED("Dikonfirmasi Supplier"),
    PARTIALLY_RECEIVED("Sebagian Diterima"),
    RECEIVED("Semua Diterima"),
    BILLED("Sudah Dibill"),
    CANCELLED("Dibatalkan");

    private final String label;
    PurchaseOrderStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}


// ── GoodsReceiptStatus.java ──────────────────────────────────────────────────
package com.artivisi.accountingfinance.enums;

public enum GoodsReceiptStatus {
    DRAFT("Draft"),
    CONFIRMED("Dikonfirmasi"),
    BILLED("Sudah Dibill"),
    CANCELLED("Dibatalkan");

    private final String label;
    GoodsReceiptStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
