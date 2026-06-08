package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.InventoryFifoLayer;
import com.artivisi.accountingfinance.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InventoryFifoLayerRepository extends JpaRepository<InventoryFifoLayer, UUID> {

    /**
     * Find layers with remaining quantity, ordered by date (oldest first) for FIFO.
     */
    @Query("SELECT l FROM InventoryFifoLayer l " +
           "WHERE l.product.id = :productId " +
           "AND l.fullyConsumed = false " +
           "AND l.remainingQuantity > 0 " +
           "ORDER BY l.layerDate ASC, l.createdAt ASC")
    List<InventoryFifoLayer> findAvailableLayers(@Param("productId") UUID productId);

    /**
     * Find all layers for a product (for valuation report).
     */
    @Query("SELECT l FROM InventoryFifoLayer l " +
           "LEFT JOIN FETCH l.product " +
           "WHERE l.product.id = :productId " +
           "ORDER BY l.layerDate ASC, l.createdAt ASC")
    List<InventoryFifoLayer> findByProductId(@Param("productId") UUID productId);

    /**
     * Get total remaining quantity across all layers for a product.
     */
    @Query("SELECT COALESCE(SUM(l.remainingQuantity), 0) FROM InventoryFifoLayer l " +
           "WHERE l.product.id = :productId AND l.fullyConsumed = false")
    BigDecimal getTotalRemainingQuantity(@Param("productId") UUID productId);

    /**
     * Get total value of remaining inventory (sum of remaining qty * unit cost per layer).
     */
    @Query("SELECT COALESCE(SUM(l.remainingQuantity * l.unitCost), 0) FROM InventoryFifoLayer l " +
           "WHERE l.product.id = :productId AND l.fullyConsumed = false")
    BigDecimal getTotalRemainingValue(@Param("productId") UUID productId);

    /**
     * Find layers that are not fully consumed for a product.
     */
    @Query("SELECT l FROM InventoryFifoLayer l " +
           "WHERE l.product = :product " +
           "AND l.fullyConsumed = false " +
           "ORDER BY l.layerDate ASC")
    List<InventoryFifoLayer> findActiveLayersByProduct(@Param("product") Product product);
}
