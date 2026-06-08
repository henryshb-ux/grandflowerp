package com.artivisi.accountingfinance.enums;

public enum FiscalPeriodStatus {
    OPEN("Open", "Terbuka"),
    MONTH_CLOSED("Month Closed", "Tutup Bulan"),
    TAX_FILED("Tax Filed", "SPT Dilaporkan");

    private final String englishName;
    private final String indonesianName;

    FiscalPeriodStatus(String englishName, String indonesianName) {
        this.englishName = englishName;
        this.indonesianName = indonesianName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getIndonesianName() {
        return indonesianName;
    }
}
