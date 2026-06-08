package com.artivisi.accountingfinance.entity;

/**
 * Status for production orders.
 */
public enum ProductionOrderStatus {
    DRAFT("Draft"),
    IN_PROGRESS("Sedang Diproses"),
    COMPLETED("Selesai"),
    CANCELLED("Dibatalkan");

    private final String label;

    ProductionOrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getDisplayName() {
        return label;
    }
}
