package com.artivisi.accountingfinance.entity;
 
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
 
@Entity
@Table(name = "project_revenue_entries")
@Getter @Setter @NoArgsConstructor
public class ProjectRevenueEntry {
 
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
 
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_project", nullable = false)
    private Project project;
 
    @NotNull
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;
 
    @NotBlank
    @Column(name = "revenue_type", nullable = false, length = 50)
    private String revenueType; // MATERIAL, SERVICE, ENGINEERING, INSTALLATION, RETENTION, OTHER
 
    @NotBlank
    @Column(name = "description", nullable = false, length = 500)
    private String description;
 
    @NotNull
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
 
    @Column(name = "tax_amount", precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
 
    @Column(name = "reference_type", length = 30)
    private String referenceType; // INVOICE, SO, MANUAL
 
    @Column(name = "reference_id")
    private UUID referenceId;
 
    @Column(name = "termin_no")
    private Integer terminNo;
 
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
 
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
 
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
 
    @Column(name = "created_by", length = 100)
    private String createdBy;
 
    @PrePersist protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now; this.updatedAt = now;
    }
    @PreUpdate protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
