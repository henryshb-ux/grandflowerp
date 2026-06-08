package com.artivisi.accountingfinance.entity;

import lombok.Getter;

/**
 * Types of inventory transactions.
 */
@Getter
public enum InventoryTransactionType {
    /**
     * Purchase of inventory (increases stock and cost)
     */
    PURCHASE("Pembelian", true),

    /**
     * Sale of inventory (decreases stock, records COGS)
     */
    SALE("Penjualan", false),

    /**
     * Adjustment - increase (e.g., stock opname correction)
     */
    ADJUSTMENT_IN("Penyesuaian Masuk", true),

    /**
     * Adjustment - decrease (e.g., damage, loss, expired)
     */
    ADJUSTMENT_OUT("Penyesuaian Keluar", false),

    /**
     * Production input - raw materials consumed
     */
    PRODUCTION_OUT("Produksi Keluar", false),

    /**
     * Production output - finished goods produced
     */
    PRODUCTION_IN("Produksi Masuk", true),

    /**
     * Transfer between warehouses/locations (source)
     */
    TRANSFER_OUT("Transfer Keluar", false),

    /**
     * Transfer between warehouses/locations (destination)
     */
    TRANSFER_IN("Transfer Masuk", true);

    private final String displayName;
    private final boolean inbound;

    InventoryTransactionType(String displayName, boolean inbound) {
        this.displayName = displayName;
        this.inbound = inbound;
    }
}
