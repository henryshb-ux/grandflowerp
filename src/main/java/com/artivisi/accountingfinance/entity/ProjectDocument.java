package com.artivisi.accountingfinance.entity;
 
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
 
@Entity
@Table(name = "project_documents")
@Getter @Setter @NoArgsConstructor
public class ProjectDocument {
 
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
 
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_project", nullable = false)
    private Project project;
 
    @NotBlank
    @Column(name = "doc_type", nullable = false, length = 50)
    private String docType;
    // RFQ, QUOTATION, SALES_ORDER, PO_OWNER, DELIVERY_ORDER,
    // BAST, INVOICE, PURCHASE_ORDER, GOODS_RECEIPT,
    // MTC, DRAWING, SPECIFICATION, CONTRACT, OTHER
 
    @Column(name = "doc_number", length = 100)
    private String docNumber;
 
    @Column(name = "doc_date")
    private LocalDate docDate;
 
    @Column(name = "description", length = 500)
    private String description;
 
    @Column(name = "file_path", length = 500)
    private String filePath;
 
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
 
    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;
 
    @PrePersist protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}