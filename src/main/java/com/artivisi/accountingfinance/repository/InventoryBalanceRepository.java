package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.InventoryBalance;
import com.artivisi.accountingfinance.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, UUID> {

    @Query("SELECT b FROM InventoryBalance b " +
           "LEFT JOIN FETCH b.product p " +
           "WHERE p.id = :productId")
    Optional<InventoryBalance> findByProductId(@Param("productId") UUID productId);

    @Query("SELECT b FROM InventoryBalance b " +
           "LEFT JOIN FETCH b.product p " +
           "WHERE p = :product")
    Optional<InventoryBalance> findByProduct(@Param("product") Product product);

    @Query(value = "SELECT ib.* FROM inventory_balances ib " +
           "LEFT JOIN products p ON ib.id_product = p.id " +
           "LEFT JOIN product_categories pc ON p.id_category = pc.id " +
           "WHERE (COALESCE(CAST(:search AS varchar), '') = '' OR " +
           "       LOWER(p.code) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%')) OR " +
           "       LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))) " +
           "AND (CAST(:categoryId AS uuid) IS NULL OR p.id_category = CAST(:categoryId AS uuid)) " +
           "AND p.active = true " +
           "ORDER BY p.code",
           countQuery = "SELECT COUNT(*) FROM inventory_balances ib " +
           "LEFT JOIN products p ON ib.id_product = p.id " +
           "WHERE (COALESCE(CAST(:search AS varchar), '') = '' OR " +
           "       LOWER(p.code) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%')) OR " +
           "       LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))) " +
           "AND (CAST(:categoryId AS uuid) IS NULL OR p.id_category = CAST(:categoryId AS uuid)) " +
           "AND p.active = true",
           nativeQuery = true)
    Page<InventoryBalance> findByFilters(
            @Param("search") String search,
            @Param("categoryId") UUID categoryId,
            Pageable pageable);

    @Query("SELECT b FROM InventoryBalance b " +
           "LEFT JOIN FETCH b.product p " +
           "WHERE p.active = true " +
           "AND p.minimumStock IS NOT NULL " +
           "AND b.quantity < p.minimumStock " +
           "ORDER BY p.code")
    List<InventoryBalance> findLowStockProducts();

    @Query("SELECT b FROM InventoryBalance b " +
           "LEFT JOIN FETCH b.product p " +
           "WHERE p.active = true AND b.quantity > 0 " +
           "ORDER BY p.code")
    List<InventoryBalance> findAllWithStock();

    @Query("SELECT COALESCE(SUM(b.totalCost), 0) FROM InventoryBalance b " +
           "WHERE b.product.active = true")
    BigDecimal getTotalInventoryValue();

    @Query("SELECT b FROM InventoryBalance b " +
           "LEFT JOIN FETCH b.product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.active = true " +
           "ORDER BY p.code")
    List<InventoryBalance> findAllWithProduct();

    @Query("SELECT b FROM InventoryBalance b " +
           "LEFT JOIN FETCH b.product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.category.id = :categoryId " +
           "AND p.active = true " +
           "ORDER BY p.code")
    List<InventoryBalance> findByProductCategoryId(@Param("categoryId") UUID categoryId);

    @Query("SELECT b FROM InventoryBalance b " +
           "LEFT JOIN FETCH b.product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE (LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND p.active = true " +
           "ORDER BY p.code")
    List<InventoryBalance> findBySearch(@Param("search") String search);

    @Query("SELECT b FROM InventoryBalance b " +
           "LEFT JOIN FETCH b.product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.category.id = :categoryId " +
           "AND (LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND p.active = true " +
           "ORDER BY p.code")
    List<InventoryBalance> findByProductCategoryIdAndSearch(@Param("categoryId") UUID categoryId, @Param("search") String search);
}
