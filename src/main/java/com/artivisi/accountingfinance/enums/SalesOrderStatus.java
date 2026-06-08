package com.artivisi.accountingfinance.enums;

public enum SalesOrderStatus {
    CONFIRMED("Terkonfirmasi"),
    IN_PROGRESS("Dalam Proses"),
    PARTIALLY_DELIVERED("Sebagian Terkirim"),
    DELIVERED("Terkirim"),
    INVOICED("Sudah Diinvoice"),
    CANCELLED("Dibatalkan");

    private final String label;

    SalesOrderStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
