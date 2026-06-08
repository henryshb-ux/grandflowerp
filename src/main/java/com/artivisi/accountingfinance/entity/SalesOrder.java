package com.artivisi.accountingfinance.entity;

import com.artivisi.accountingfinance.enums.SalesOrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_orders")
@Getter @Setter @NoArgsConstructor
public class SalesOrder extends BaseEntity {

    @NotBlank @Size(max = 50)
    @Column(name = "so_number", nullable = false, unique = true, length = 50)
    private String soNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_quotation")
    private Quotation quotation;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_client", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_project")
    private Project project;

    @Size(max = 100)
    @Column(name = "po_number_customer", length = 100)
    private String poNumberCustomer;

    @NotNull
    @Column(name = "so_date", nullable = false)
    private LocalDate soDate;

    @Column(name = "expected_delivery")
    private LocalDate expectedDelivery;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SalesOrderStatus status = SalesOrderStatus.CONFIRMED;

    @Size(max = 200)
    @Column(name = "payment_terms", length = 200)
    private String paymentTerms;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "subtotal",         precision = 19, scale = 2)
    private BigDecimal subtotal       = BigDecimal.ZERO;

    @Column(name = "discount_amount",  precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount",       precision = 19, scale = 2)
    private BigDecimal taxAmount      = BigDecimal.ZERO;

    @Column(name = "total_amount",     precision = 19, scale = 2)
    private BigDecimal totalAmount    = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<SalesOrderLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "salesOrder")
    private List<DeliveryOrder> deliveryOrders = new ArrayList<>();

    // ── helpers ──────────────────────────────────────────────
// ── Tambahkan method berikut ke dalam class SalesOrder.java ──────────────────
// Letakkan di bagian "// ── helpers ──" yang sudah ada

    /**
     * Persentase pengiriman (0-100) berdasarkan qty_delivered vs qty order.
     * Digunakan untuk progress bar di list dan detail.
     */
    public int getDeliveryProgressPct() {
        if (lines == null || lines.isEmpty()) return 0;

        java.math.BigDecimal totalQty      = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalDelivered = java.math.BigDecimal.ZERO;

        for (SalesOrderLine l : lines) {
            if (l.getQuantity() != null) {
                totalQty      = totalQty.add(l.getQuantity());
                totalDelivered = totalDelivered.add(
                    l.getQtyDelivered() != null ? l.getQtyDelivered() : java.math.BigDecimal.ZERO
                );
            }
        }

        if (totalQty.compareTo(java.math.BigDecimal.ZERO) == 0) return 0;

        return totalDelivered
            .multiply(new java.math.BigDecimal("100"))
            .divide(totalQty, 0, java.math.RoundingMode.HALF_UP)
            .intValue();
    }

    public boolean isConfirmed()           { return status == SalesOrderStatus.CONFIRMED; }
    public boolean isCancelled()           { return status == SalesOrderStatus.CANCELLED; }
    public boolean isFullyDelivered()      { return status == SalesOrderStatus.DELIVERED; }

    public String getClientLabel() {
        if (client == null) return "";
        return client.getCode() + " - " + client.getName();
    }

    public void recalculate() {
        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (SalesOrderLine l : lines) {
            sub = sub.add(l.getSubtotal());
            tax = tax.add(l.getTaxAmount());
        }
        this.subtotal    = sub;
        this.taxAmount   = tax;
        this.totalAmount = sub.subtract(discountAmount).add(tax);
    }
}
