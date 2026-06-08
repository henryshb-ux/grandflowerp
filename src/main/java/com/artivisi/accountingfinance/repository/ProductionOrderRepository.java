package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.ProductionOrder;
import com.artivisi.accountingfinance.entity.ProductionOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, UUID> {

    Optional<ProductionOrder> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    @Query("SELECT po FROM ProductionOrder po LEFT JOIN FETCH po.billOfMaterial b LEFT JOIN FETCH b.product ORDER BY po.orderDate DESC, po.orderNumber DESC")
    List<ProductionOrder> findAllWithBom();

    @Query("SELECT po FROM ProductionOrder po LEFT JOIN FETCH po.billOfMaterial b LEFT JOIN FETCH b.product WHERE po.id = :id")
    Optional<ProductionOrder> findByIdWithBom(@Param("id") UUID id);

    @Query("SELECT po FROM ProductionOrder po LEFT JOIN FETCH po.billOfMaterial b LEFT JOIN FETCH b.product LEFT JOIN FETCH b.lines l LEFT JOIN FETCH l.component WHERE po.id = :id")
    Optional<ProductionOrder> findByIdWithBomAndLines(@Param("id") UUID id);

    @Query("SELECT po FROM ProductionOrder po WHERE po.status = :status ORDER BY po.orderDate DESC")
    List<ProductionOrder> findByStatus(@Param("status") ProductionOrderStatus status);

    @Query("SELECT po FROM ProductionOrder po WHERE po.orderDate BETWEEN :startDate AND :endDate ORDER BY po.orderDate DESC")
    List<ProductionOrder> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT po FROM ProductionOrder po WHERE po.billOfMaterial.id = :bomId ORDER BY po.orderDate DESC")
    List<ProductionOrder> findByBomId(@Param("bomId") UUID bomId);

    @Query("SELECT po FROM ProductionOrder po LEFT JOIN FETCH po.billOfMaterial b LEFT JOIN FETCH b.product " +
           "WHERE po.status = :status " +
           "ORDER BY po.orderDate DESC")
    List<ProductionOrder> findByStatusWithBom(@Param("status") ProductionOrderStatus status);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(po.orderNumber, 9, 4) AS integer)), 0) FROM ProductionOrder po " +
           "WHERE po.orderNumber LIKE CONCAT('PO-', :year, '-%')")
    Integer findMaxOrderNumberForYear(@Param("year") String year);
}
