// ── DeliveryOrderRepository.java ────────────────────────────────────────────
package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.DeliveryOrder;
import com.artivisi.accountingfinance.enums.DeliveryOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, UUID> {

    @Query("SELECT d FROM DeliveryOrder d LEFT JOIN FETCH d.client LEFT JOIN FETCH d.lines WHERE d.id = :id AND d.deletedAt IS NULL")
    Optional<DeliveryOrder> findByIdWithLines(@Param("id") UUID id);

    @Query("""
        SELECT d FROM DeliveryOrder d LEFT JOIN FETCH d.client
        WHERE d.deletedAt IS NULL
        AND (:status IS NULL OR d.status = :status)
        AND (:search IS NULL OR :search = ''
             OR LOWER(d.doNumber) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(d.client.name) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY d.deliveryDate DESC
        """)
    Page<DeliveryOrder> findByFilters(@Param("status") DeliveryOrderStatus status,
                                      @Param("search") String search,
                                      Pageable pageable);

    List<DeliveryOrder> findBySalesOrderIdAndDeletedAtIsNull(UUID soId);

    long countByStatus(DeliveryOrderStatus status);
}
