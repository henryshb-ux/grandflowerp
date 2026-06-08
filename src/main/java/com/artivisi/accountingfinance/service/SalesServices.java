// ── RfqService.java ──────────────────────────────────────────────────────────
package com.artivisi.accountingfinance.service;

import com.artivisi.accountingfinance.entity.Rfq;
import com.artivisi.accountingfinance.entity.RfqLine;
import com.artivisi.accountingfinance.enums.RfqStatus;
import com.artivisi.accountingfinance.repository.RfqRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RfqService {

    private final RfqRepository rfqRepository;
    private final SequenceService sequenceService;

    public Rfq findById(UUID id) {
        return rfqRepository.findByIdWithLines(id)
                .orElseThrow(() -> new EntityNotFoundException("RFQ tidak ditemukan: " + id));
    }

    public Page<Rfq> findByFilters(RfqStatus status, String search, Pageable pageable) {
        return rfqRepository.findByFilters(status, search, pageable);
    }

    @Transactional
    public Rfq create(Rfq rfq) {
        rfq.setRfqNumber(sequenceService.nextNumber("RFQ"));
        rfq.setStatus(RfqStatus.OPEN);
        for (RfqLine line : rfq.getLines()) line.setRfq(rfq);
        return rfqRepository.save(rfq);
    }

    @Transactional
    public Rfq update(UUID id, Rfq updated) {
        Rfq existing = findById(id);
        if (existing.isCancelled()) {
            throw new IllegalStateException("RFQ yang dibatalkan tidak bisa diedit");
        }
        existing.setClient(updated.getClient());
        existing.setProject(updated.getProject());
        existing.setRfqDate(updated.getRfqDate());
        existing.setResponseDate(updated.getResponseDate());
        existing.setSubject(updated.getSubject());
        existing.setNotes(updated.getNotes());
        existing.getLines().clear();
        for (RfqLine line : updated.getLines()) {
            line.setRfq(existing);
            existing.getLines().add(line);
        }
        return rfqRepository.save(existing);
    }

    @Transactional
    public void cancel(UUID id) {
        Rfq rfq = findById(id);
        rfq.setStatus(RfqStatus.CANCELLED);
        rfqRepository.save(rfq);
    }
}


// ── SalesOrderService.java ───────────────────────────────────────────────────
package com.artivisi.accountingfinance.service;

import com.artivisi.accountingfinance.entity.*;
import com.artivisi.accountingfinance.enums.QuotationStatus;
import com.artivisi.accountingfinance.enums.SalesOrderStatus;
import com.artivisi.accountingfinance.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesOrderService {

    private final SalesOrderRepository   salesOrderRepository;
    private final QuotationRepository    quotationRepository;
    private final SequenceService        sequenceService;

    public SalesOrder findById(UUID id) {
        return salesOrderRepository.findByIdWithLines(id)
                .orElseThrow(() -> new EntityNotFoundException("Sales Order tidak ditemukan: " + id));
    }

    public Page<SalesOrder> findByFilters(SalesOrderStatus status, String search, Pageable pageable) {
        return salesOrderRepository.findByFilters(status, search, pageable);
    }

    /** Buat SO dari Quotation yang sudah WON */
    @Transactional
    public SalesOrder createFromQuotation(UUID quotationId, String poNumberCustomer) {
        Quotation qt = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new EntityNotFoundException("Quotation tidak ditemukan"));
        if (!qt.isWon()) {
            throw new IllegalStateException("Hanya Quotation berstatus WON yang bisa dikonversi ke SO");
        }

        SalesOrder so = new SalesOrder();
        so.setSoNumber(sequenceService.nextNumber("SALES_ORDER"));
        so.setQuotation(qt);
        so.setClient(qt.getClient());
        so.setProject(qt.getProject());
        so.setSoDate(java.time.LocalDate.now());
        so.setPoNumberCustomer(poNumberCustomer);
        so.setPaymentTerms(qt.getPaymentTerms());
        so.setStatus(SalesOrderStatus.CONFIRMED);

        int order = 1;
        for (QuotationLine ql : qt.getLines()) {
            SalesOrderLine sol = new SalesOrderLine();
            sol.setSalesOrder(so);
            sol.setQuotationLine(ql);
            sol.setProduct(ql.getProduct());
            sol.setLineOrder(order++);
            sol.setDescription(ql.getDescription());
            sol.setQuantity(ql.getQuantity());
            sol.setUnit(ql.getUnit());
            sol.setUnitPrice(ql.getUnitPrice());
            sol.setDiscountPct(ql.getDiscountPct());
            sol.setTaxPct(ql.getTaxPct());
            sol.calculateAmounts();
            so.getLines().add(sol);
        }
        so.recalculate();
        return salesOrderRepository.save(so);
    }

    @Transactional
    public SalesOrder cancel(UUID id) {
        SalesOrder so = findById(id);
        if (so.isFullyDelivered()) {
            throw new IllegalStateException("SO yang sudah terkirim tidak bisa dibatalkan");
        }
        so.setStatus(SalesOrderStatus.CANCELLED);
        return salesOrderRepository.save(so);
    }
}


// ── DeliveryOrderService.java ────────────────────────────────────────────────
package com.artivisi.accountingfinance.service;

import com.artivisi.accountingfinance.entity.*;
import com.artivisi.accountingfinance.enums.DeliveryOrderStatus;
import com.artivisi.accountingfinance.enums.SalesOrderStatus;
import com.artivisi.accountingfinance.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryOrderService {

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final SalesOrderRepository    salesOrderRepository;
    private final SequenceService         sequenceService;

    public DeliveryOrder findById(UUID id) {
        return deliveryOrderRepository.findByIdWithLines(id)
                .orElseThrow(() -> new EntityNotFoundException("Delivery Order tidak ditemukan: " + id));
    }

    public Page<DeliveryOrder> findByFilters(DeliveryOrderStatus status, String search, Pageable pageable) {
        return deliveryOrderRepository.findByFilters(status, search, pageable);
    }

    @Transactional
    public DeliveryOrder create(DeliveryOrder do_, UUID soId) {
        SalesOrder so = salesOrderRepository.findByIdWithLines(soId)
                .orElseThrow(() -> new EntityNotFoundException("Sales Order tidak ditemukan"));

        do_.setDoNumber(sequenceService.nextNumber("DELIVERY_ORDER"));
        do_.setSalesOrder(so);
        do_.setClient(so.getClient());
        do_.setDeliveryAddress(so.getDeliveryAddress());
        do_.setStatus(DeliveryOrderStatus.DRAFT);

        for (DeliveryOrderLine line : do_.getLines()) line.setDeliveryOrder(do_);
        DeliveryOrder saved = deliveryOrderRepository.save(do_);

        // Update qty_delivered pada SO lines
        updateSoDeliveredQty(so);
        return saved;
    }

    @Transactional
    public DeliveryOrder markShipped(UUID id, String shippedBy, String trackingNo) {
        DeliveryOrder d = findById(id);
        d.setStatus(DeliveryOrderStatus.SHIPPED);
        d.setShippedBy(shippedBy);
        d.setTrackingNo(trackingNo);
        return deliveryOrderRepository.save(d);
    }

    @Transactional
    public DeliveryOrder markDelivered(UUID id, String receivedBy) {
        DeliveryOrder d = findById(id);
        d.setStatus(DeliveryOrderStatus.DELIVERED);
        d.setReceivedBy(receivedBy);
        d.setReceivedAt(LocalDateTime.now());

        // Update status SO
        updateSoStatus(d.getSalesOrder());
        return deliveryOrderRepository.save(d);
    }

    @Transactional
    public DeliveryOrder signBast(UUID id, String bastNumber, java.time.LocalDate bastDate) {
        DeliveryOrder d = findById(id);
        if (!d.isDelivered()) {
            throw new IllegalStateException("BAST hanya bisa ditandatangani setelah status Diterima");
        }
        d.setStatus(DeliveryOrderStatus.BAST_SIGNED);
        d.setBastNumber(bastNumber);
        d.setBastSignedAt(bastDate);
        return deliveryOrderRepository.save(d);
    }

    // ── Private helpers ───────────────────────────────────────

    private void updateSoDeliveredQty(SalesOrder so) {
        // Hitung ulang qty_delivered untuk setiap SO line
        for (SalesOrderLine sol : so.getLines()) {
            BigDecimal totalDelivered = deliveryOrderRepository
                    .findBySalesOrderIdAndDeletedAtIsNull(so.getId())
                    .stream()
                    .flatMap(d -> d.getLines().stream())
                    .filter(dl -> dl.getSoLine() != null && dl.getSoLine().getId().equals(sol.getId()))
                    .map(DeliveryOrderLine::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            sol.setQtyDelivered(totalDelivered);
        }
        salesOrderRepository.save(so);
    }

    private void updateSoStatus(SalesOrder so) {
        boolean allDelivered = so.getLines().stream()
                .allMatch(l -> l.getQtyDelivered().compareTo(l.getQuantity()) >= 0);
        boolean anyDelivered = so.getLines().stream()
                .anyMatch(l -> l.getQtyDelivered().compareTo(BigDecimal.ZERO) > 0);

        if (allDelivered) {
            so.setStatus(SalesOrderStatus.DELIVERED);
        } else if (anyDelivered) {
            so.setStatus(SalesOrderStatus.PARTIALLY_DELIVERED);
        }
        salesOrderRepository.save(so);
    }
}
