// ── PurchaseRequestService.java ──────────────────────────────────────────────
package com.artivisi.accountingfinance.service;

import com.artivisi.accountingfinance.entity.*;
import com.artivisi.accountingfinance.enums.PurchaseRequestStatus;
import com.artivisi.accountingfinance.repository.PurchaseRequestRepository;
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
public class PurchaseRequestService {

    private final PurchaseRequestRepository prRepository;
    private final SequenceService           sequenceService;

    public PurchaseRequest findById(UUID id) {
        return prRepository.findByIdWithLines(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase Request tidak ditemukan: " + id));
    }

    public Page<PurchaseRequest> findByFilters(PurchaseRequestStatus status, String search, Pageable pageable) {
        return prRepository.findByFilters(status, search, pageable);
    }

    public PrSummary getSummary() {
        return new PrSummary(
            prRepository.countByStatus(PurchaseRequestStatus.DRAFT),
            prRepository.countByStatus(PurchaseRequestStatus.SUBMITTED),
            prRepository.countByStatus(PurchaseRequestStatus.APPROVED),
            prRepository.countByStatus(PurchaseRequestStatus.REJECTED)
        );
    }

    @Transactional
    public PurchaseRequest create(PurchaseRequest pr) {
        pr.setPrNumber(sequenceService.nextNumber("PURCHASE_REQUEST"));
        pr.setStatus(PurchaseRequestStatus.DRAFT);
        for (PurchaseRequestLine l : pr.getLines()) l.setPurchaseRequest(pr);
        return prRepository.save(pr);
    }

    @Transactional
    public PurchaseRequest update(UUID id, PurchaseRequest updated) {
        PurchaseRequest existing = findById(id);
        if (!existing.isDraft() && !existing.isRejected()) {
            throw new IllegalStateException("Hanya PR Draft atau Ditolak yang bisa diedit");
        }
        existing.setProject(updated.getProject());
        existing.setRequestDate(updated.getRequestDate());
        existing.setRequiredDate(updated.getRequiredDate());
        existing.setPriority(updated.getPriority());
        existing.setRequestedBy(updated.getRequestedBy());
        existing.setSubject(updated.getSubject());
        existing.setNotes(updated.getNotes());
        existing.getLines().clear();
        for (PurchaseRequestLine l : updated.getLines()) {
            l.setPurchaseRequest(existing);
            existing.getLines().add(l);
        }
        return prRepository.save(existing);
    }

    @Transactional
    public PurchaseRequest submit(UUID id) {
        PurchaseRequest pr = findById(id);
        if (!pr.isDraft()) throw new IllegalStateException("Hanya PR Draft yang bisa diajukan");
        pr.setStatus(PurchaseRequestStatus.SUBMITTED);
        return prRepository.save(pr);
    }

    @Transactional
    public PurchaseRequest approve(UUID id, String approvedBy) {
        PurchaseRequest pr = findById(id);
        if (!pr.isSubmitted()) throw new IllegalStateException("Hanya PR yang diajukan yang bisa disetujui");
        pr.setStatus(PurchaseRequestStatus.APPROVED);
        pr.setApprovedBy(approvedBy);
        pr.setApprovedAt(LocalDateTime.now());
        return prRepository.save(pr);
    }

    @Transactional
    public PurchaseRequest reject(UUID id, String reason) {
        PurchaseRequest pr = findById(id);
        if (!pr.isSubmitted()) throw new IllegalStateException("Hanya PR yang diajukan yang bisa ditolak");
        pr.setStatus(PurchaseRequestStatus.REJECTED);
        pr.setNotes((pr.getNotes() != null ? pr.getNotes() + "\n" : "") + "Alasan ditolak: " + reason);
        return prRepository.save(pr);
    }

    @Transactional
    public void cancel(UUID id) {
        PurchaseRequest pr = findById(id);
        if (pr.getStatus() == PurchaseRequestStatus.ORDERED) {
            throw new IllegalStateException("PR yang sudah dipesan tidak bisa dibatalkan");
        }
        pr.setStatus(PurchaseRequestStatus.CANCELLED);
        prRepository.save(pr);
    }

    public record PrSummary(long draftCount, long submittedCount, long approvedCount, long rejectedCount) {}
}


// ── PurchaseOrderService.java ────────────────────────────────────────────────
package com.artivisi.accountingfinance.service;

import com.artivisi.accountingfinance.entity.*;
import com.artivisi.accountingfinance.enums.PurchaseOrderStatus;
import com.artivisi.accountingfinance.enums.PurchaseRequestStatus;
import com.artivisi.accountingfinance.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private final PurchaseOrderRepository   poRepository;
    private final PurchaseRequestRepository prRepository;
    private final GoodsReceiptRepository    grRepository;
    private final SequenceService           sequenceService;

    public PurchaseOrder findById(UUID id) {
        return poRepository.findByIdWithLines(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase Order tidak ditemukan: " + id));
    }

    public Page<PurchaseOrder> findByFilters(PurchaseOrderStatus status, String search, Pageable pageable) {
        return poRepository.findByFilters(status, search, pageable);
    }

    public List<GoodsReceipt> findGoodsReceipts(UUID poId) {
        return grRepository.findByPurchaseOrderIdAndDeletedAtIsNull(poId);
    }

    public PoSummary getSummary() {
        return new PoSummary(
            poRepository.countByStatus(PurchaseOrderStatus.DRAFT),
            poRepository.countByStatus(PurchaseOrderStatus.SENT),
            poRepository.countByStatus(PurchaseOrderStatus.CONFIRMED),
            poRepository.countByStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED),
            poRepository.sumTotalByActiveStatus()
        );
    }

    @Transactional
    public PurchaseOrder create(PurchaseOrder po) {
        po.setPoNumber(sequenceService.nextNumber("PURCHASE_ORDER"));
        po.setStatus(PurchaseOrderStatus.DRAFT);
        for (PurchaseOrderLine l : po.getLines()) {
            l.setPurchaseOrder(po);
            l.calculateAmounts();
        }
        po.recalculate();
        PurchaseOrder saved = poRepository.save(po);

        // Update PR status → ORDERED jika dari PR
        if (po.getPurchaseRequest() != null) {
            po.getPurchaseRequest().setStatus(PurchaseRequestStatus.ORDERED);
            prRepository.save(po.getPurchaseRequest());
        }
        return saved;
    }

    @Transactional
    public PurchaseOrder createFromPr(UUID prId, UUID vendorId) {
        PurchaseRequest pr = prRepository.findByIdWithLines(prId)
            .orElseThrow(() -> new EntityNotFoundException("PR tidak ditemukan"));
        if (!pr.isApproved()) {
            throw new IllegalStateException("Hanya PR yang disetujui yang bisa dibuatkan PO");
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setPurchaseRequest(pr);
        po.setProject(pr.getProject());
        po.setPoDate(java.time.LocalDate.now());
        po.setExpectedDelivery(pr.getRequiredDate());
        po.setStatus(PurchaseOrderStatus.DRAFT);

        // Vendor di-set oleh controller dari input user
        int order = 1;
        for (PurchaseRequestLine prl : pr.getLines()) {
            PurchaseOrderLine pol = new PurchaseOrderLine();
            pol.setPrLine(prl);
            pol.setProduct(prl.getProduct());
            pol.setLineOrder(order++);
            pol.setDescription(prl.getDescription());
            pol.setQuantity(prl.getQuantity());
            pol.setUnit(prl.getUnit());
            pol.setUnitPrice(prl.getEstimatedPrice() != null ? prl.getEstimatedPrice() : BigDecimal.ZERO);
            pol.setTaxPct(new BigDecimal("11"));
            pol.calculateAmounts();
            po.getLines().add(pol);
        }
        return po; // belum disimpan, controller yang simpan setelah set vendor
    }

    @Transactional
    public PurchaseOrder update(UUID id, PurchaseOrder updated) {
        PurchaseOrder existing = findById(id);
        if (!existing.isDraft()) throw new IllegalStateException("Hanya PO Draft yang bisa diedit");
        existing.setVendor(updated.getVendor());
        existing.setPoDate(updated.getPoDate());
        existing.setExpectedDelivery(updated.getExpectedDelivery());
        existing.setPaymentTerms(updated.getPaymentTerms());
        existing.setDeliveryAddress(updated.getDeliveryAddress());
        existing.setNotes(updated.getNotes());
        existing.setVendorRef(updated.getVendorRef());
        existing.setDiscountAmount(updated.getDiscountAmount());
        existing.getLines().clear();
        for (PurchaseOrderLine l : updated.getLines()) {
            l.setPurchaseOrder(existing);
            l.calculateAmounts();
            existing.getLines().add(l);
        }
        existing.recalculate();
        return poRepository.save(existing);
    }

    @Transactional
    public PurchaseOrder send(UUID id) {
        PurchaseOrder po = findById(id);
        if (!po.isDraft()) throw new IllegalStateException("Hanya PO Draft yang bisa dikirim");
        po.setStatus(PurchaseOrderStatus.SENT);
        po.setSentAt(LocalDateTime.now());
        return poRepository.save(po);
    }

    @Transactional
    public PurchaseOrder confirm(UUID id, String vendorRef) {
        PurchaseOrder po = findById(id);
        if (po.getStatus() != PurchaseOrderStatus.SENT) {
            throw new IllegalStateException("Hanya PO Terkirim yang bisa dikonfirmasi");
        }
        po.setStatus(PurchaseOrderStatus.CONFIRMED);
        po.setVendorRef(vendorRef);
        return poRepository.save(po);
    }

    @Transactional
    public void cancel(UUID id) {
        PurchaseOrder po = findById(id);
        if (po.getStatus() == PurchaseOrderStatus.RECEIVED || po.getStatus() == PurchaseOrderStatus.BILLED) {
            throw new IllegalStateException("PO yang sudah diterima/dibill tidak bisa dibatalkan");
        }
        po.setStatus(PurchaseOrderStatus.CANCELLED);
        poRepository.save(po);
    }

    // dipanggil oleh GoodsReceiptService setelah GR confirmed
    @Transactional
    public void updatePoReceivedQty(PurchaseOrder po) {
        var allGrs = grRepository.findByPurchaseOrderIdAndDeletedAtIsNull(po.getId());
        for (PurchaseOrderLine pol : po.getLines()) {
            BigDecimal totalReceived = allGrs.stream()
                .filter(g -> g.getStatus() != com.artivisi.accountingfinance.enums.GoodsReceiptStatus.CANCELLED)
                .flatMap(g -> g.getLines().stream())
                .filter(gl -> gl.getPoLine() != null && gl.getPoLine().getId().equals(pol.getId()))
                .map(GoodsReceiptLine::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            pol.setQtyReceived(totalReceived);
        }
        boolean allReceived = po.getLines().stream()
            .allMatch(l -> l.getQtyReceived().compareTo(l.getQuantity()) >= 0);
        boolean anyReceived = po.getLines().stream()
            .anyMatch(l -> l.getQtyReceived().compareTo(BigDecimal.ZERO) > 0);

        if (allReceived) po.setStatus(PurchaseOrderStatus.RECEIVED);
        else if (anyReceived) po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        poRepository.save(po);
    }

    public record PoSummary(
        long draftCount, long sentCount, long confirmedCount,
        long partialReceivedCount, BigDecimal totalActiveValue) {}
}


// ── GoodsReceiptService.java ─────────────────────────────────────────────────
package com.artivisi.accountingfinance.service;

import com.artivisi.accountingfinance.entity.*;
import com.artivisi.accountingfinance.enums.GoodsReceiptStatus;
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
public class GoodsReceiptService {

    private final GoodsReceiptRepository  grRepository;
    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderService    poService;
    private final SequenceService         sequenceService;

    public GoodsReceipt findById(UUID id) {
        return grRepository.findByIdWithLines(id)
            .orElseThrow(() -> new EntityNotFoundException("Goods Receipt tidak ditemukan: " + id));
    }

    public Page<GoodsReceipt> findByFilters(GoodsReceiptStatus status, String search, Pageable pageable) {
        return grRepository.findByFilters(status, search, pageable);
    }

    public GrSummary getSummary() {
        return new GrSummary(
            grRepository.countByStatus(GoodsReceiptStatus.DRAFT),
            grRepository.countByStatus(GoodsReceiptStatus.CONFIRMED),
            grRepository.countByStatus(GoodsReceiptStatus.BILLED)
        );
    }

    @Transactional
    public GoodsReceipt create(GoodsReceipt gr, UUID poId) {
        PurchaseOrder po = poRepository.findByIdWithLines(poId)
            .orElseThrow(() -> new EntityNotFoundException("PO tidak ditemukan"));

        gr.setGrNumber(sequenceService.nextNumber("GOODS_RECEIPT"));
        gr.setPurchaseOrder(po);
        gr.setVendor(po.getVendor());
        gr.setStatus(GoodsReceiptStatus.DRAFT);
        for (GoodsReceiptLine l : gr.getLines()) l.setGoodsReceipt(gr);
        return grRepository.save(gr);
    }

    @Transactional
    public GoodsReceipt confirm(UUID id) {
        GoodsReceipt gr = findById(id);
        if (!gr.isDraft()) throw new IllegalStateException("Hanya GR Draft yang bisa dikonfirmasi");
        gr.setStatus(GoodsReceiptStatus.CONFIRMED);
        GoodsReceipt saved = grRepository.save(gr);
        // Update qty_received pada PO
        poService.updatePoReceivedQty(gr.getPurchaseOrder());
        return saved;
    }

    @Transactional
    public void cancel(UUID id) {
        GoodsReceipt gr = findById(id);
        if (gr.isCancelled()) return;
        if (gr.getStatus() == GoodsReceiptStatus.BILLED) {
            throw new IllegalStateException("GR yang sudah dibill tidak bisa dibatalkan");
        }
        gr.setStatus(GoodsReceiptStatus.CANCELLED);
        grRepository.save(gr);
        poService.updatePoReceivedQty(gr.getPurchaseOrder());
    }

    public record GrSummary(long draftCount, long confirmedCount, long billedCount) {}
}
