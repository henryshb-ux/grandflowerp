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
import java.util.UUID;
 
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
 
    /**
     * id_bill disimpan sebagai UUID biasa (tanpa FK constraint di DB).
     * Relasi ke Bill ditangani di application layer.
     */
    @Column(name = "id_bill")
    private UUID idBill;
 
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
 
    // helpers
    public boolean isDraft()     { return status == GoodsReceiptStatus.DRAFT; }
    public boolean isConfirmed() { return status == GoodsReceiptStatus.CONFIRMED; }
    public boolean isCancelled() { return status == GoodsReceiptStatus.CANCELLED; }
}
 