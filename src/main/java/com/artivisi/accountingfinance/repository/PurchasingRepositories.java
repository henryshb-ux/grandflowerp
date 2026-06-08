// ── PurchaseRequestRepository.java ──────────────────────────────────────────
package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.PurchaseRequest;
import com.artivisi.accountingfinance.enums.PurchaseRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, UUID> {

    @Query("""
        SELECT r FROM PurchaseRequest r LEFT JOIN FETCH r.lines
        WHERE r.id = :id AND r.deletedAt IS NULL
        """)
    Optional<PurchaseRequest> findByIdWithLines(@Param("id") UUID id);

    @Query("""
        SELECT r FROM PurchaseRequest r
        WHERE r.deletedAt IS NULL
        AND (:status IS NULL OR r.status = :status)
        AND (:search IS NULL OR :search = ''
             OR LOWER(r.prNumber) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(r.subject)  LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(r.requestedBy) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY r.requestDate DESC
        """)
    Page<PurchaseRequest> findByFilters(@Param("status") PurchaseRequestStatus status,
                                        @Param("search") String search,
                                        Pageable pageable);

    long countByStatus(PurchaseRequestStatus status);
}


// ── PurchaseOrderRepository.java ─────────────────────────────────────────────
package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.PurchaseOrder;
import com.artivisi.accountingfinance.enums.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    @Query("""
        SELECT p FROM PurchaseOrder p
        LEFT JOIN FETCH p.vendor
        LEFT JOIN FETCH p.lines
        WHERE p.id = :id AND p.deletedAt IS NULL
        """)
    Optional<PurchaseOrder> findByIdWithLines(@Param("id") UUID id);

    @Query("""
        SELECT p FROM PurchaseOrder p LEFT JOIN FETCH p.vendor
        WHERE p.deletedAt IS NULL
        AND (:status IS NULL OR p.status = :status)
        AND (:search IS NULL OR :search = ''
             OR LOWER(p.poNumber)       LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(p.vendor.name)    LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(p.vendorRef)      LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY p.poDate DESC
        """)
    Page<PurchaseOrder> findByFilters(@Param("status") PurchaseOrderStatus status,
                                      @Param("search") String search,
                                      Pageable pageable);

    long countByStatus(PurchaseOrderStatus status);

    @Query("""
        SELECT COALESCE(SUM(p.totalAmount), 0) FROM PurchaseOrder p
        WHERE p.deletedAt IS NULL
        AND p.status IN ('SENT','CONFIRMED','PARTIALLY_RECEIVED')
        """)
    BigDecimal sumTotalByActiveStatus();
}


// ── GoodsReceiptRepository.java ──────────────────────────────────────────────
package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.GoodsReceipt;
import com.artivisi.accountingfinance.enums.GoodsReceiptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {

    @Query("""
        SELECT g FROM GoodsReceipt g
        LEFT JOIN FETCH g.vendor
        LEFT JOIN FETCH g.lines
        WHERE g.id = :id AND g.deletedAt IS NULL
        """)
    Optional<GoodsReceipt> findByIdWithLines(@Param("id") UUID id);

    @Query("""
        SELECT g FROM GoodsReceipt g LEFT JOIN FETCH g.vendor
        WHERE g.deletedAt IS NULL
        AND (:status IS NULL OR g.status = :status)
        AND (:search IS NULL OR :search = ''
             OR LOWER(g.grNumber)    LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(g.vendor.name) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY g.receiptDate DESC
        """)
    Page<GoodsReceipt> findByFilters(@Param("status") GoodsReceiptStatus status,
                                     @Param("search") String search,
                                     Pageable pageable);

    List<GoodsReceipt> findByPurchaseOrderIdAndDeletedAtIsNull(UUID poId);

    long countByStatus(GoodsReceiptStatus status);
}
