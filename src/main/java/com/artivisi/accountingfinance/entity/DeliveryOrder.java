package com.artivisi.accountingfinance.entity;

import com.artivisi.accountingfinance.enums.DeliveryOrderStatus;
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
import java.util.UUID;

@Entity
@Table(name = "delivery_orders")
@Getter @Setter @NoArgsConstructor
public class DeliveryOrder extends BaseEntity {

    @NotBlank @Size(max = 50)
    @Column(name = "do_number", nullable = false, unique = true, length = 50)
    private String doNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_so", nullable = false)
    private SalesOrder salesOrder;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_client", nullable = false)
    private Client client;

    @NotNull
    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryOrderStatus status = DeliveryOrderStatus.DRAFT;

    @Size(max = 200)
    @Column(name = "shipped_by", length = 200)
    private String shippedBy;

    @Size(max = 100)
    @Column(name = "tracking_no", length = 100)
    private String trackingNo;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Size(max = 200)
    @Column(name = "received_by", length = 200)
    private String receivedBy;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Size(max = 50)
    @Column(name = "bast_number", length = 50)
    private String bastNumber;

    @Column(name = "bast_signed_at")
    private LocalDate bastSignedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_invoice")
    private Invoice invoice;

    @OneToMany(mappedBy = "deliveryOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<DeliveryOrderLine> lines = new ArrayList<>();

    // ── helpers ──────────────────────────────────────────────
    public boolean isDraft()      { return status == DeliveryOrderStatus.DRAFT; }
    public boolean isShipped()    { return status == DeliveryOrderStatus.SHIPPED; }
    public boolean isDelivered()  { return status == DeliveryOrderStatus.DELIVERED; }
    public boolean isBastSigned() { return status == DeliveryOrderStatus.BAST_SIGNED; }

    public String getClientLabel() {
        if (client == null) return "";
        return client.getCode() + " - " + client.getName();
    }
}
