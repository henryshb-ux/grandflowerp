package com.artivisi.accountingfinance.enums;

public enum QuotationStatus {
    DRAFT("Draft"),
    SENT("Terkirim"),
    WON("Menang"),
    LOST("Kalah"),
    CANCELLED("Dibatalkan");

    private final String label;

    QuotationStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
