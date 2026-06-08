// ── QuotationRepository.java ────────────────────────────────────────────────
package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.Quotation;
import com.artivisi.accountingfinance.enums.QuotationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface QuotationRepository extends JpaRepository<Quotation, UUID> {

    @Query("SELECT q FROM Quotation q LEFT JOIN FETCH q.client LEFT JOIN FETCH q.lines WHERE q.id = :id AND q.deletedAt IS NULL")
    Optional<Quotation> findByIdWithLines(@Param("id") UUID id);

    @Query("""
        SELECT q FROM Quotation q LEFT JOIN FETCH q.client
        WHERE q.deletedAt IS NULL
        AND (:status IS NULL OR q.status = :status)
        AND (:search IS NULL OR :search = ''
             OR LOWER(q.quotationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(q.client.name) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(q.subject) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY q.quotationDate DESC
        """)
    Page<Quotation> findByFilters(@Param("status") QuotationStatus status,
                                  @Param("search") String search,
                                  Pageable pageable);

    long countByStatus(QuotationStatus status);

    @Query("SELECT COALESCE(SUM(q.totalAmount), 0) FROM Quotation q WHERE q.status = :status AND q.deletedAt IS NULL")
    BigDecimal sumTotalByStatus(@Param("status") QuotationStatus status);
}
