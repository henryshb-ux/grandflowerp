package com.artivisi.accountingfinance.entity;

import com.artivisi.accountingfinance.enums.QuotationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "quotations")
@Getter @Setter @NoArgsConstructor
public class Quotation extends BaseEntity {

    @NotBlank
    @Size(max = 50)
    @Column(name = "quotation_number", nullable = false, unique = true, length = 50)
    private String quotationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rfq")
    private Rfq rfq;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_client", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_project")
    private Project project;

    @NotNull
    @Column(name = "quotation_date", nullable = false)
    private LocalDate quotationDate;

    @NotNull
    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuotationStatus status = QuotationStatus.DRAFT;

    @Size(max = 500)
    @Column(name = "subject", length = 500)
    private String subject;

    @Size(max = 200)
    @Column(name = "payment_terms", length = 200)
    private String paymentTerms;

    @Size(max = 200)
    @Column(name = "delivery_terms", length = 200)
    private String deliveryTerms;

    @Column(name = "delivery_days")
    private Integer deliveryDays;

    @Column(name = "currency", length = 10)
    private String currency = "IDR";

    @Column(name = "subtotal", precision = 19, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "won_at")
    private LocalDateTime wonAt;

    @Column(name = "lost_at")
    private LocalDateTime lostAt;

    @Column(name = "lost_reason", columnDefinition = "TEXT")
    private String lostReason;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<QuotationLine> lines = new ArrayList<>();

    // ── helpers ──────────────────────────────────────────────
    public boolean isDraft()     { return status == QuotationStatus.DRAFT; }
    public boolean isSent()      { return status == QuotationStatus.SENT; }
    public boolean isWon()       { return status == QuotationStatus.WON; }
    public boolean isLost()      { return status == QuotationStatus.LOST; }
    public boolean isCancelled() { return status == QuotationStatus.CANCELLED; }

    public boolean isExpired() {
        return validUntil != null && LocalDate.now().isAfter(validUntil)
               && status == QuotationStatus.SENT;
    }

    public String getClientLabel() {
        if (client == null) return "";
        return client.getCode() + " - " + client.getName();
    }

    public void recalculate() {
        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (QuotationLine l : lines) {
            l.calculateAmounts();
            sub = sub.add(l.getSubtotal());
            tax = tax.add(l.getTaxAmount());
        }
        this.subtotal     = sub;
        this.taxAmount    = tax;
        this.totalAmount  = sub.subtract(discountAmount).add(tax);
    }
}
