package com.artivisi.accountingfinance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "invoice_lines")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceLine extends DocumentLine {

    @JsonIgnore
    @NotNull(message = "Invoice wajib diisi")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_invoice", nullable = false)
    private Invoice invoice;

    // Transient view helpers for the form's combobox (no JPA mapping; Jackson-friendly).
    public java.util.UUID getProductId() {
        return getProduct() != null ? getProduct().getId() : null;
    }

    public String getProductLabel() {
        var p = getProduct();
        if (p == null) return "";
        return (p.getCode() == null ? "" : p.getCode() + " - ") + (p.getName() == null ? "" : p.getName());
    }
}
