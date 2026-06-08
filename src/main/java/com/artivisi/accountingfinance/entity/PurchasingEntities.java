// ── PurchaseRequestLine.java ─────────────────────────────────────────────────
package com.artivisi.accountingfinance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "purchase_request_lines")
@Getter @Setter @NoArgsConstructor
public class PurchaseRequestLine {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pr", nullable = false)
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_product")
    private Product product;

    @Column(name = "line_order")
    private Integer lineOrder = 1;

    @NotBlank
    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @NotNull
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "estimated_price", precision = 19, scale = 2)
    private BigDecimal estimatedPrice;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now; this.updatedAt = now;
    }
    @PreUpdate protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}


// ── PurchaseOrder.java ───────────────────────────────────────────────────────
package com.artivisi.accountingfinance.entity;

import com.artivisi.accountingfinance.enums.PurchaseOrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
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

    // ── helpers ──────────────────────────────────────────────
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
                totalRecv = totalRecv.add(
                    l.getQtyReceived() != null ? l.getQtyReceived() : BigDecimal.ZERO);
            }
        }
        if (totalQty.compareTo(BigDecimal.ZERO) == 0) return 0;
        return totalRecv.multiply(new BigDecimal("100"))
            .divide(totalQty, 0, java.math.RoundingMode.HALF_UP).intValue();
    }

    public void recalculate() {
        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
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


// ── PurchaseOrderLine.java ───────────────────────────────────────────────────
package com.artivisi.accountingfinance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "purchase_order_lines")
@Getter @Setter @NoArgsConstructor
public class PurchaseOrderLine {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_po", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pr_line")
    private PurchaseRequestLine prLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_product")
    private Product product;

    @Column(name = "line_order")
    private Integer lineOrder = 1;

    @NotBlank
    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @NotNull
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "qty_received", precision = 19, scale = 4)
    private BigDecimal qtyReceived = BigDecimal.ZERO;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "unit_price", precision = 19, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "discount_pct", precision = 5, scale = 2)
    private BigDecimal discountPct = BigDecimal.ZERO;

    @Column(name = "subtotal", precision = 19, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_pct", precision = 5, scale = 2)
    private BigDecimal taxPct = new BigDecimal("11");

    @Column(name = "tax_amount", precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total", precision = 19, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now; this.updatedAt = now;
    }
    @PreUpdate protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public void calculateAmounts() {
        BigDecimal gross   = quantity.multiply(unitPrice);
        BigDecimal disc    = gross.multiply(discountPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        this.subtotal      = gross.subtract(disc).setScale(2, RoundingMode.HALF_UP);
        this.taxAmount     = subtotal.multiply(taxPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        this.total         = subtotal.add(taxAmount);
    }

    public BigDecimal getQtyRemaining() {
        return quantity.subtract(qtyReceived != null ? qtyReceived : BigDecimal.ZERO);
    }
}


// ── GoodsReceipt.java ────────────────────────────────────────────────────────
package com.artivisi.accountingfinance.entity;

import com.artivisi.accountingfinance.enums.GoodsReceiptStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "goods_receipts")
@Getter @Setter @NoArgsConstructor
public class GoodsReceipt extends BaseEntity {

    @Size(max = 50)
    @Column(name = "gr_number", nullable = false, unique = true, length = 50)
    private String grNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_po", nullable = false)
    private PurchaseOrder purchaseOrder;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vendor", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bill")
    private Bill bill;

    @NotNull
    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GoodsReceiptStatus status = GoodsReceiptStatus.DRAFT;

    @Size(max = 200)
    @Column(name = "received_by", length = 200)
    private String receivedBy;

    @Size(max = 100)
    @Column(name = "delivery_note", length = 100)
    private String deliveryNote;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<GoodsReceiptLine> lines = new ArrayList<>();

    // ── helpers ──────────────────────────────────────────────
    public boolean isDraft()     { return status == GoodsReceiptStatus.DRAFT; }
    public boolean isConfirmed() { return status == GoodsReceiptStatus.CONFIRMED; }
    public boolean isCancelled() { return status == GoodsReceiptStatus.CANCELLED; }
}


// ── GoodsReceiptLine.java ────────────────────────────────────────────────────
package com.artivisi.accountingfinance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "goods_receipt_lines")
@Getter @Setter @NoArgsConstructor
public class GoodsReceiptLine {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_gr", nullable = false)
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_po_line")
    private PurchaseOrderLine poLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_product")
    private Product product;

    @Column(name = "line_order")
    private Integer lineOrder = 1;

    @NotBlank
    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @NotNull
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "unit_price", precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "serial_numbers", columnDefinition = "TEXT")
    private String serialNumbers;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now; this.updatedAt = now;
    }
    @PreUpdate protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
