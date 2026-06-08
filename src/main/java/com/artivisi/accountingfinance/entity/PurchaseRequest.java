package com.artivisi.accountingfinance.entity;

import com.artivisi.accountingfinance.enums.PurchaseRequestPriority;
import com.artivisi.accountingfinance.enums.PurchaseRequestStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_requests")
@Getter @Setter @NoArgsConstructor
public class PurchaseRequest extends BaseEntity {

    @Size(max = 50)
    @Column(name = "pr_number", nullable = false, unique = true, length = 50)
    private String prNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_project")
    private Project project;

    @NotNull
    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @Column(name = "required_date")
    private LocalDate requiredDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PurchaseRequestStatus status = PurchaseRequestStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private PurchaseRequestPriority priority = PurchaseRequestPriority.NORMAL;

    @Size(max = 200)
    @Column(name = "requested_by", length = 200)
    private String requestedBy;

    @Size(max = 200)
    @Column(name = "approved_by", length = 200)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Size(max = 500)
    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "purchaseRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<PurchaseRequestLine> lines = new ArrayList<>();

    // ── helpers ──────────────────────────────────────────────
    public boolean isDraft()     { return status == PurchaseRequestStatus.DRAFT; }
    public boolean isSubmitted() { return status == PurchaseRequestStatus.SUBMITTED; }
    public boolean isApproved()  { return status == PurchaseRequestStatus.APPROVED; }
    public boolean isRejected()  { return status == PurchaseRequestStatus.REJECTED; }
    public boolean isCancelled() { return status == PurchaseRequestStatus.CANCELLED; }

    public boolean isOverdue() {
        return requiredDate != null
            && LocalDate.now().isAfter(requiredDate)
            && status != PurchaseRequestStatus.ORDERED
            && status != PurchaseRequestStatus.CLOSED
            && status != PurchaseRequestStatus.CANCELLED;
    }
}
