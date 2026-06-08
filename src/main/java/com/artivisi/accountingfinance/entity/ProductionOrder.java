package com.artivisi.accountingfinance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Production Order - a work order to produce a quantity of finished goods.
 */
@Entity
@Table(name = "production_orders")
@Getter
@Setter
@NoArgsConstructor
public class ProductionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Nomor order wajib diisi")
    @Size(max = 50, message = "Nomor order maksimal 50 karakter")
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @NotNull(message = "BOM wajib dipilih")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bill_of_material", nullable = false)
    private BillOfMaterial billOfMaterial;

    @NotNull(message = "Jumlah produksi wajib diisi")
    @Column(name = "quantity", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    @NotNull(message = "Tanggal order wajib diisi")
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "planned_completion_date")
    private LocalDate plannedCompletionDate;

    @Column(name = "actual_completion_date")
    private LocalDate actualCompletionDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductionOrderStatus status = ProductionOrderStatus.DRAFT;

    @Size(max = 500, message = "Catatan maksimal 500 karakter")
    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "total_component_cost", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalComponentCost = BigDecimal.ZERO;

    @Column(name = "unit_cost", precision = 19, scale = 4, nullable = false)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_transaction")
    private Transaction transaction;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Size(max = 100)
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Size(max = 100)
    @Column(name = "completed_by", length = 100)
    private String completedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isDraft() {
        return status == ProductionOrderStatus.DRAFT;
    }

    public boolean isInProgress() {
        return status == ProductionOrderStatus.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return status == ProductionOrderStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == ProductionOrderStatus.CANCELLED;
    }

    public boolean canStart() {
        return isDraft();
    }

    public boolean canComplete() {
        return isInProgress();
    }

    public boolean canCancel() {
        return isDraft() || isInProgress();
    }
}
