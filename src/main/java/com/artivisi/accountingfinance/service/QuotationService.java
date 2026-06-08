package com.artivisi.accountingfinance.service;

import com.artivisi.accountingfinance.entity.*;
import com.artivisi.accountingfinance.enums.QuotationStatus;
import com.artivisi.accountingfinance.enums.RfqStatus;
import com.artivisi.accountingfinance.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final RfqRepository       rfqRepository;
    private final SequenceService      sequenceService;

    // ── Read ──────────────────────────────────────────────────

    public Quotation findById(UUID id) {
        return quotationRepository.findByIdWithLines(id)
                .orElseThrow(() -> new EntityNotFoundException("Quotation tidak ditemukan: " + id));
    }

    public Page<Quotation> findByFilters(QuotationStatus status, String search, Pageable pageable) {
        return quotationRepository.findByFilters(status, search, pageable);
    }

    public QuotationSummary getSummary() {
        return new QuotationSummary(
            quotationRepository.countByStatus(QuotationStatus.DRAFT),
            quotationRepository.countByStatus(QuotationStatus.SENT),
            quotationRepository.countByStatus(QuotationStatus.WON),
            quotationRepository.countByStatus(QuotationStatus.LOST),
            quotationRepository.sumTotalByStatus(QuotationStatus.SENT),
            quotationRepository.sumTotalByStatus(QuotationStatus.WON)
        );
    }

    // ── Write ─────────────────────────────────────────────────

    @Transactional
    public Quotation create(Quotation quotation) {
        String number = sequenceService.nextNumber("QUOTATION");
        quotation.setQuotationNumber(number);
        quotation.setStatus(QuotationStatus.DRAFT);
        quotation.recalculate();

        // set back-reference on lines
        for (QuotationLine line : quotation.getLines()) {
            line.setQuotation(quotation);
            line.calculateAmounts();
        }
        return quotationRepository.save(quotation);
    }

    @Transactional
    public Quotation update(UUID id, Quotation updated) {
        Quotation existing = findById(id);
        if (!existing.isDraft()) {
            throw new IllegalStateException("Hanya quotation berstatus Draft yang bisa diedit");
        }

        existing.setRfq(updated.getRfq());
        existing.setClient(updated.getClient());
        existing.setProject(updated.getProject());
        existing.setQuotationDate(updated.getQuotationDate());
        existing.setValidUntil(updated.getValidUntil());
        existing.setSubject(updated.getSubject());
        existing.setPaymentTerms(updated.getPaymentTerms());
        existing.setDeliveryTerms(updated.getDeliveryTerms());
        existing.setDeliveryDays(updated.getDeliveryDays());
        existing.setNotes(updated.getNotes());
        existing.setInternalNotes(updated.getInternalNotes());
        existing.setDiscountAmount(updated.getDiscountAmount());

        existing.getLines().clear();
        for (QuotationLine line : updated.getLines()) {
            line.setQuotation(existing);
            line.calculateAmounts();
            existing.getLines().add(line);
        }
        existing.recalculate();
        return quotationRepository.save(existing);
    }

    @Transactional
    public Quotation send(UUID id) {
        Quotation q = findById(id);
        if (!q.isDraft()) {
            throw new IllegalStateException("Hanya quotation Draft yang bisa dikirim");
        }
        q.setStatus(QuotationStatus.SENT);
        q.setSentAt(LocalDateTime.now());
        return quotationRepository.save(q);
    }

    @Transactional
    public Quotation markWon(UUID id) {
        Quotation q = findById(id);
        if (q.isCancelled()) {
            throw new IllegalStateException("Quotation yang dibatalkan tidak bisa ditandai menang");
        }
        q.setStatus(QuotationStatus.WON);
        q.setWonAt(LocalDateTime.now());

        // Update status RFQ jika ada
        if (q.getRfq() != null) {
            q.getRfq().setStatus(RfqStatus.QUOTED);
            rfqRepository.save(q.getRfq());
        }
        return quotationRepository.save(q);
    }

    @Transactional
    public Quotation markLost(UUID id, String reason) {
        Quotation q = findById(id);
        if (q.isCancelled()) {
            throw new IllegalStateException("Quotation yang dibatalkan tidak bisa ditandai kalah");
        }
        q.setStatus(QuotationStatus.LOST);
        q.setLostAt(LocalDateTime.now());
        q.setLostReason(reason);
        return quotationRepository.save(q);
    }

    @Transactional
    public void delete(UUID id) {
        Quotation q = findById(id);
        if (!q.isDraft()) {
            throw new IllegalStateException("Hanya quotation Draft yang bisa dihapus");
        }
        q.softDelete();
        quotationRepository.save(q);
    }

    // ── Inner summary record ──────────────────────────────────

    public record QuotationSummary(
        long draftCount,
        long sentCount,
        long wonCount,
        long lostCount,
        java.math.BigDecimal outstandingValue,
        java.math.BigDecimal wonValue
    ) {}
}
