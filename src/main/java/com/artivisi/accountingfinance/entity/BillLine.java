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
@Table(name = "bill_lines")
@Getter
@Setter
@NoArgsConstructor
public class BillLine extends DocumentLine {

    @JsonIgnore
    @NotNull(message = "Tagihan wajib diisi")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bill", nullable = false)
    private Bill bill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_expense_account")
    private ChartOfAccount expenseAccount;

    /** Transient JSON shape consumed by the bill form combobox hydration. */
    public String getExpenseAccountId() {
        return expenseAccount == null ? "" : expenseAccount.getId().toString();
    }

    /** Combobox label for the bound expense account: "code - name". */
    public String getExpenseAccountLabel() {
        if (expenseAccount == null) return "";
        String code = expenseAccount.getAccountCode() == null ? "" : expenseAccount.getAccountCode();
        String name = expenseAccount.getAccountName() == null ? "" : expenseAccount.getAccountName();
        if (code.isEmpty() && name.isEmpty()) return "";
        if (code.isEmpty()) return name;
        if (name.isEmpty()) return code;
        return code + " - " + name;
    }
}
