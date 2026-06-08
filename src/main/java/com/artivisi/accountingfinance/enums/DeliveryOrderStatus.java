package com.artivisi.accountingfinance.enums;

public enum DeliveryOrderStatus {
    DRAFT("Draft"),
    SHIPPED("Dikirim"),
    DELIVERED("Diterima"),
    BAST_SIGNED("BAST Ditandatangani"),
    CANCELLED("Dibatalkan");

    private final String label;

    DeliveryOrderStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
