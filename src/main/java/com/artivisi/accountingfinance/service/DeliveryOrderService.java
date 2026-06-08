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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryOrderService {

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final SalesOrderRepository    salesOrderRepository;
    private final SequenceService         sequenceService;

    // ── Read ──────────────────────────────────────────────────

    public DeliveryOrder findById(UUID id) {
        return deliveryOrderRepository.findByIdWithLines(id)
            .orElseThrow(() -> new EntityNotFoundException("Delivery Order tidak ditemukan: " + id));
    }

    public Page<DeliveryOrder> findByFilters(DeliveryOrderStatus status, String search, Pageable pageable) {
        return deliveryOrderRepository.findByFilters(status, search, pageable);
    }

    public DoSummary getSummary() {
        return new DoSummary(
            deliveryOrderRepository.countByStatus(DeliveryOrderStatus.DRAFT),
            deliveryOrderRepository.countByStatus(DeliveryOrderStatus.SHIPPED),
            deliveryOrderRepository.countByStatus(DeliveryOrderStatus.DELIVERED),
            deliveryOrderRepository.countByStatus(DeliveryOrderStatus.BAST_SIGNED)
        );
    }

    // ── Write ─────────────────────────────────────────────────

    @Transactional
    public DeliveryOrder create(DeliveryOrder doo, UUID soId) {
        SalesOrder so = salesOrderRepository.findByIdWithLines(soId)
            .orElseThrow(() -> new EntityNotFoundException("Sales Order tidak ditemukan"));

        if (so.getStatus() == SalesOrderStatus.CANCELLED) {
            throw new IllegalStateException("SO yang dibatalkan tidak bisa dibuat DO-nya");
        }

        doo.setDoNumber(sequenceService.nextNumber("DELIVERY_ORDER"));
        doo.setSalesOrder(so);
        doo.setClient(so.getClient());
        if (doo.getDeliveryAddress() == null) {
            doo.setDeliveryAddress(so.getDeliveryAddress());
        }
        doo.setStatus(DeliveryOrderStatus.DRAFT);
        for (DeliveryOrderLine line : doo.getLines()) line.setDeliveryOrder(doo);

        DeliveryOrder saved = deliveryOrderRepository.save(doo);
        updateSoDeliveredQty(so);

        // Update status SO → IN_PROGRESS saat DO pertama dibuat
        if (so.getStatus() == SalesOrderStatus.CONFIRMED) {
            so.setStatus(SalesOrderStatus.IN_PROGRESS);
            salesOrderRepository.save(so);
        }
        return saved;
    }

    @Transactional
    public DeliveryOrder markShipped(UUID id, String shippedBy, String trackingNo) {
        DeliveryOrder doo = findById(id);
        if (!doo.isDraft()) throw new IllegalStateException("Hanya DO berstatus Draft yang bisa dikirim");
        doo.setStatus(DeliveryOrderStatus.SHIPPED);
        doo.setShippedBy(shippedBy);
        doo.setTrackingNo(trackingNo);
        return deliveryOrderRepository.save(doo);
    }

    @Transactional
    public DeliveryOrder markDelivered(UUID id, String receivedBy) {
        DeliveryOrder doo = findById(id);
        if (doo.getStatus() != DeliveryOrderStatus.SHIPPED) {
            throw new IllegalStateException("Hanya DO berstatus Dikirim yang bisa diubah ke Diterima");
        }
        doo.setStatus(DeliveryOrderStatus.DELIVERED);
        doo.setReceivedBy(receivedBy);
        doo.setReceivedAt(LocalDateTime.now());
        DeliveryOrder saved = deliveryOrderRepository.save(doo);
        updateSoStatus(doo.getSalesOrder());
        return saved;
    }

    @Transactional
    public DeliveryOrder signBast(UUID id, String bastNumber, LocalDate bastDate) {
        DeliveryOrder doo = findById(id);
        if (!doo.isDelivered() && doo.getStatus() != DeliveryOrderStatus.DELIVERED) {
            throw new IllegalStateException("BAST hanya bisa ditandatangani setelah status Diterima");
        }
        doo.setStatus(DeliveryOrderStatus.BAST_SIGNED);
        doo.setBastNumber(bastNumber);
        doo.setBastSignedAt(bastDate);
        return deliveryOrderRepository.save(doo);
    }

    @Transactional
    public void cancel(UUID id) {
        DeliveryOrder doo = findById(id);
        if (doo.isBastSigned()) {
            throw new IllegalStateException("DO yang BAST-nya sudah ditandatangani tidak bisa dibatalkan");
        }
        doo.setStatus(DeliveryOrderStatus.CANCELLED);
        deliveryOrderRepository.save(doo);
        // Hitung ulang qty delivered pada SO
        updateSoDeliveredQty(doo.getSalesOrder());
    }

    // ── Private helpers ───────────────────────────────────────

    private void updateSoDeliveredQty(SalesOrder so) {
        var allDos = deliveryOrderRepository.findBySalesOrderIdAndDeletedAtIsNull(so.getId());
        for (SalesOrderLine sol : so.getLines()) {
            BigDecimal totalDelivered = allDos.stream()
                .filter(d -> d.getStatus() != DeliveryOrderStatus.CANCELLED)
                .flatMap(d -> d.getLines().stream())
                .filter(dl -> dl.getSoLine() != null
                    && dl.getSoLine().getId().equals(sol.getId()))
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

    // ── Summary ───────────────────────────────────────────────

    public record DoSummary(
        long draftCount,
        long shippedCount,
        long deliveredCount,
        long bastSignedCount
    ) {}
}
