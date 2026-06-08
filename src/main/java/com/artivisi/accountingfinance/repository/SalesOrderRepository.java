package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.SalesOrder;
import com.artivisi.accountingfinance.enums.SalesOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID> {

    @Query("""
        SELECT s FROM SalesOrder s
        LEFT JOIN FETCH s.client
        LEFT JOIN FETCH s.lines
        WHERE s.id = :id AND s.deletedAt IS NULL
        """)
    Optional<SalesOrder> findByIdWithLines(@Param("id") UUID id);

    @Query("""
        SELECT s FROM SalesOrder s LEFT JOIN FETCH s.client
        WHERE s.deletedAt IS NULL
        AND (:status IS NULL OR s.status = :status)
        AND (:search IS NULL OR :search = ''
             OR LOWER(s.soNumber) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(s.client.name) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(s.poNumberCustomer) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY s.soDate DESC
        """)
    Page<SalesOrder> findByFilters(@Param("status") SalesOrderStatus status,
                                   @Param("search") String search,
                                   Pageable pageable);

    long countByStatus(SalesOrderStatus status);

    @Query("""
        SELECT COALESCE(SUM(s.totalAmount), 0)
        FROM SalesOrder s
        WHERE s.deletedAt IS NULL
        AND s.status IN ('CONFIRMED', 'IN_PROGRESS', 'PARTIALLY_DELIVERED')
        """)
    BigDecimal sumTotalByActiveStatus();
}
