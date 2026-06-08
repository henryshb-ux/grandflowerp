package com.artivisi.accountingfinance.entity;

import lombok.Getter;

/**
 * Inventory costing method for calculating cost of goods sold (COGS).
 */
@Getter
public enum CostingMethod {
    /**
     * First In, First Out - oldest inventory costs are used first.
     * More accurate cost tracking, better for perishables.
     */
    FIFO("FIFO (First In, First Out)"),

    /**
     * Weighted Average Cost - average of all inventory costs.
     * Simpler calculation, suitable for homogeneous products.
     */
    WEIGHTED_AVERAGE("Rata-rata Tertimbang");

    private final String displayName;

    CostingMethod(String displayName) {
        this.displayName = displayName;
    }
}
