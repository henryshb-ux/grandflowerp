package com.artivisi.accountingfinance.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bill of Materials (BOM) - defines the recipe for producing a finished product.
 * Links a finished product to its component materials with quantities.
 */
@Entity
@Table(name = "bill_of_materials")
@Getter
@Setter
@NoArgsConstructor
public class BillOfMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Produk jadi wajib dipilih")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_product", nullable = false)
    private Product product;

    @NotBlank(message = "Kode BOM wajib diisi")
    @Size(max = 50, message = "Kode BOM maksimal 50 karakter")
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @NotBlank(message = "Nama BOM wajib diisi")
    @Size(max = 200, message = "Nama BOM maksimal 200 karakter")
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Size(max = 500, message = "Deskripsi maksimal 500 karakter")
    @Column(name = "description", length = 500)
    private String description;

    @NotNull(message = "Jumlah output wajib diisi")
    @Column(name = "output_quantity", precision = 15, scale = 4, nullable = false)
    private BigDecimal outputQuantity = BigDecimal.ONE;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "billOfMaterial", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<BillOfMaterialLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addLine(BillOfMaterialLine line) {
        lines.add(line);
        line.setBillOfMaterial(this);
    }

    public void removeLine(BillOfMaterialLine line) {
        lines.remove(line);
        line.setBillOfMaterial(null);
    }

    public void clearLines() {
        for (BillOfMaterialLine line : new ArrayList<>(lines)) {
            removeLine(line);
        }
    }
}
