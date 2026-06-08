package com.artivisi.accountingfinance.entity;
 
import com.artivisi.accountingfinance.enums.PurchaseOrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
 
@Entity
@Table(name = "purchase_orders")
@Getter @Setter @NoArgsConstructor
public class PurchaseOrder extends BaseEntity {
 
    @Size(max = 50)
    @Column(name = "po_number", nullable = false, unique = true, length = 50)
    private String poNumber;
 
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vendor", nullable = false)
    private Vendor vendor;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pr")
    private PurchaseRequest purchaseRequest;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_project")
    private Project project;
 
    @NotNull
    @Column(name = "po_date", nullable = false)
    private LocalDate poDate;
 
    @Column(name = "expected_delivery")
    private LocalDate expectedDelivery;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;
 
    @Size(max = 200)
    @Column(name = "payment_terms", length = 200)
    private String paymentTerms;
 
    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;
 
    @Column(name = "currency", length = 10)
    private String currency = "IDR";
 
    @Column(name = "subtotal",        precision = 19, scale = 2)
    private BigDecimal subtotal       = BigDecimal.ZERO;
 
    @Column(name = "discount_amount", precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
 
    @Column(name = "tax_amount",      precision = 19, scale = 2)
    private BigDecimal taxAmount      = BigDecimal.ZERO;
 
    @Column(name = "total_amount",    precision = 19, scale = 2)
    private BigDecimal totalAmount    = BigDecimal.ZERO;
 
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
 
    @Size(max = 100)
    @Column(name = "vendor_ref", length = 100)
    private String vendorRef;
 
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
 
    @Column(name = "approved_by", length = 200)
    private String approvedBy;
 
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
 
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<PurchaseOrderLine> lines = new ArrayList<>();
 
    @OneToMany(mappedBy = "purchaseOrder")
    private List<GoodsReceipt> goodsReceipts = new ArrayList<>();
 
    // helpers
    public boolean isDraft()     { return status == PurchaseOrderStatus.DRAFT; }
    public boolean isSent()      { return status == PurchaseOrderStatus.SENT; }
    public boolean isCancelled() { return status == PurchaseOrderStatus.CANCELLED; }
 
    public int getReceiptProgressPct() {
        if (lines == null || lines.isEmpty()) return 0;
        BigDecimal totalQty  = BigDecimal.ZERO;
        BigDecimal totalRecv = BigDecimal.ZERO;
        for (PurchaseOrderLine l : lines) {
            if (l.getQuantity() != null) {
                totalQty  = totalQty.add(l.getQuantity());
                totalRecv = totalRecv.add(l.getQtyReceived() != null ? l.getQtyReceived() : BigDecimal.ZERO);
            }
        }
        if (totalQty.compareTo(BigDecimal.ZERO) == 0) return 0;
        return totalRecv.multiply(new BigDecimal("100"))
            .divide(totalQty, 0, RoundingMode.HALF_UP).intValue();
    }
 
    public void recalculate() {
        BigDecimal sub = BigDecimal.ZERO, tax = BigDecimal.ZERO;
        for (PurchaseOrderLine l : lines) {
            l.calculateAmounts();
            sub = sub.add(l.getSubtotal());
            tax = tax.add(l.getTaxAmount());
        }
        this.subtotal    = sub;
        this.taxAmount   = tax;
        this.totalAmount = sub.subtract(discountAmount).add(tax);
    }
}