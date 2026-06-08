// ── RfqRepository.java ──────────────────────────────────────────────────────
package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.Rfq;
import com.artivisi.accountingfinance.enums.RfqStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RfqRepository extends JpaRepository<Rfq, UUID> {

    @Query("SELECT r FROM Rfq r LEFT JOIN FETCH r.client LEFT JOIN FETCH r.lines WHERE r.id = :id AND r.deletedAt IS NULL")
    Optional<Rfq> findByIdWithLines(@Param("id") UUID id);

    @Query("SELECT r FROM Rfq r LEFT JOIN FETCH r.client WHERE r.deletedAt IS NULL ORDER BY r.rfqDate DESC")
    Page<Rfq> findAllActive(Pageable pageable);

    @Query("""
        SELECT r FROM Rfq r LEFT JOIN FETCH r.client
        WHERE r.deletedAt IS NULL
        AND (:status IS NULL OR r.status = :status)
        AND (:search IS NULL OR :search = ''
             OR LOWER(r.rfqNumber) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(r.client.name) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY r.rfqDate DESC
        """)
    Page<Rfq> findByFilters(@Param("status") RfqStatus status,
                            @Param("search") String search,
                            Pageable pageable);

    long countByStatus(RfqStatus status);
}
