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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesOrderService {

    private final SalesOrderRepository    salesOrderRepository;
    private final QuotationRepository     quotationRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final InvoiceRepository       invoiceRepository;
    private final SequenceService         sequenceService;

    // ── Read ──────────────────────────────────────────────────

    public SalesOrder findById(UUID id) {
        return salesOrderRepository.findByIdWithLines(id)
            .orElseThrow(() -> new EntityNotFoundException("Sales Order tidak ditemukan: " + id));
    }

    public Page<SalesOrder> findByFilters(SalesOrderStatus status, String search, Pageable pageable) {
        return salesOrderRepository.findByFilters(status, search, pageable);
    }

    public List<DeliveryOrder> findDeliveryOrders(UUID soId) {
        return deliveryOrderRepository.findBySalesOrderIdAndDeletedAtIsNull(soId);
    }

    public List<Invoice> findInvoices(UUID soId) {
        return invoiceRepository.findBySalesOrderId(soId);
    }

    public SalesOrderSummary getSummary() {
        long confirmed   = salesOrderRepository.countByStatus(SalesOrderStatus.CONFIRMED);
        long inProgress  = salesOrderRepository.countByStatus(SalesOrderStatus.IN_PROGRESS);
        long partialDel  = salesOrderRepository.countByStatus(SalesOrderStatus.PARTIALLY_DELIVERED);
        long delivered   = salesOrderRepository.countByStatus(SalesOrderStatus.DELIVERED);
        BigDecimal totalActive = salesOrderRepository.sumTotalByActiveStatus();
        return new SalesOrderSummary(confirmed, inProgress, partialDel, delivered, totalActive);
    }

    // ── Write ─────────────────────────────────────────────────

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
        so.setDiscountAmount(qt.getDiscountAmount());
        so.recalculate();

        // Update status Quotation → tidak bisa diubah lagi
        SalesOrder saved = salesOrderRepository.save(so);

        // Update status SO ke IN_PROGRESS saat ada DO pertama (dilakukan di DeliveryOrderService)
        return saved;
    }

    @Transactional
    public SalesOrder cancel(UUID id) {
        SalesOrder so = findById(id);
        if (so.isFullyDelivered()) {
            throw new IllegalStateException("SO yang sudah dikirim penuh tidak bisa dibatalkan");
        }
        so.setStatus(SalesOrderStatus.CANCELLED);
        return salesOrderRepository.save(so);
    }

    // ── Summary record ────────────────────────────────────────

    public record SalesOrderSummary(
        long confirmedCount,
        long inProgressCount,
        long partialDeliveredCount,
        long deliveredCount,
        BigDecimal totalActiveValue
    ) {
        public long totalActive() {
            return confirmedCount + inProgressCount + partialDeliveredCount;
        }
    }
}
