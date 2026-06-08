package com.artivisi.accountingfinance.enums;

public enum RfqStatus {
    OPEN("Terbuka"),
    QUOTED("Sudah Dikutip"),
    CLOSED("Ditutup"),
    CANCELLED("Dibatalkan");

    private final String label;

    RfqStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
