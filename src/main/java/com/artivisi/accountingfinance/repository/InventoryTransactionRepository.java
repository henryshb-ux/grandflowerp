package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.InventoryTransaction;
import com.artivisi.accountingfinance.entity.InventoryTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    @Query(value = "SELECT it.* FROM inventory_transactions it " +
           "LEFT JOIN products p ON it.id_product = p.id " +
           "WHERE (CAST(:productId AS uuid) IS NULL OR it.id_product = CAST(:productId AS uuid)) " +
           "AND (CAST(:transactionType AS varchar) IS NULL OR it.transaction_type = CAST(:transactionType AS varchar)) " +
           "AND (CAST(:startDate AS date) IS NULL OR it.transaction_date >= CAST(:startDate AS date)) " +
           "AND (CAST(:endDate AS date) IS NULL OR it.transaction_date <= CAST(:endDate AS date)) " +
           "ORDER BY it.transaction_date DESC, it.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM inventory_transactions it " +
           "WHERE (CAST(:productId AS uuid) IS NULL OR it.id_product = CAST(:productId AS uuid)) " +
           "AND (CAST(:transactionType AS varchar) IS NULL OR it.transaction_type = CAST(:transactionType AS varchar)) " +
           "AND (CAST(:startDate AS date) IS NULL OR it.transaction_date >= CAST(:startDate AS date)) " +
           "AND (CAST(:endDate AS date) IS NULL OR it.transaction_date <= CAST(:endDate AS date))",
           nativeQuery = true)
    Page<InventoryTransaction> findByFilters(
            @Param("productId") UUID productId,
            @Param("transactionType") String transactionType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("SELECT t FROM InventoryTransaction t " +
           "LEFT JOIN FETCH t.product " +
           "WHERE t.product.id = :productId " +
           "ORDER BY t.transactionDate DESC, t.createdAt DESC")
    List<InventoryTransaction> findByProductId(@Param("productId") UUID productId);

    @Query("SELECT t FROM InventoryTransaction t " +
           "LEFT JOIN FETCH t.product " +
           "WHERE t.product.id = :productId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate, t.createdAt")
    List<InventoryTransaction> findByProductAndDateRange(
            @Param("productId") UUID productId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(t) FROM InventoryTransaction t WHERE t.product.id = :productId")
    long countByProductId(@Param("productId") UUID productId);

    @Query("SELECT t FROM InventoryTransaction t " +
           "LEFT JOIN FETCH t.product " +
           "WHERE t.id = :id")
    InventoryTransaction findByIdWithProduct(@Param("id") UUID id);

    @Query("SELECT t FROM InventoryTransaction t " +
           "LEFT JOIN FETCH t.product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate, t.createdAt")
    List<InventoryTransaction> findByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM InventoryTransaction t " +
           "LEFT JOIN FETCH t.product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE t.product.id = :productId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate, t.createdAt")
    List<InventoryTransaction> findByProductIdAndDateRange(
            @Param("productId") UUID productId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM InventoryTransaction t " +
           "LEFT JOIN FETCH t.product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.category.id = :categoryId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate, t.createdAt")
    List<InventoryTransaction> findByCategoryIdAndDateRange(
            @Param("categoryId") UUID categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM InventoryTransaction t " +
           "LEFT JOIN FETCH t.product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE t.transactionType = :transactionType " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate, t.createdAt")
    List<InventoryTransaction> findByTypeAndDateRange(
            @Param("transactionType") InventoryTransactionType transactionType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM InventoryTransaction t " +
           "LEFT JOIN FETCH t.product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE t.product.id = :productId " +
           "AND t.transactionType = :transactionType " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate, t.createdAt")
    List<InventoryTransaction> findByProductIdAndTypeAndDateRange(
            @Param("productId") UUID productId,
            @Param("transactionType") InventoryTransactionType transactionType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM InventoryTransaction t " +
           "LEFT JOIN FETCH t.product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.category.id = :categoryId " +
           "AND t.transactionType = :transactionType " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate, t.createdAt")
    List<InventoryTransaction> findByCategoryIdAndTypeAndDateRange(
            @Param("categoryId") UUID categoryId,
            @Param("transactionType") InventoryTransactionType transactionType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
