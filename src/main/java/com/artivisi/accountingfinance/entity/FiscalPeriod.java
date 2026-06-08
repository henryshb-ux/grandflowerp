package com.artivisi.accountingfinance.entity;

import com.artivisi.accountingfinance.enums.FiscalPeriodStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Entity
@Table(name = "fiscal_periods", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"year", "month"})
})
@Getter
@Setter
@NoArgsConstructor
public class FiscalPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Tahun wajib diisi")
    @Min(value = 2000, message = "Tahun minimal 2000")
    @Max(value = 2100, message = "Tahun maksimal 2100")
    @Column(name = "year", nullable = false)
    private Integer year;

    @NotNull(message = "Bulan wajib diisi")
    @Min(value = 1, message = "Bulan minimal 1")
    @Max(value = 12, message = "Bulan maksimal 12")
    @Column(name = "month", nullable = false)
    private Integer month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FiscalPeriodStatus status = FiscalPeriodStatus.OPEN;

    @Column(name = "month_closed_at")
    private LocalDateTime monthClosedAt;

    @Column(name = "month_closed_by")
    private String monthClosedBy;

    @Column(name = "tax_filed_at")
    private LocalDateTime taxFiledAt;

    @Column(name = "tax_filed_by")
    private String taxFiledBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOpen() {
        return status == FiscalPeriodStatus.OPEN;
    }

    public boolean isMonthClosed() {
        return status == FiscalPeriodStatus.MONTH_CLOSED;
    }

    public boolean isTaxFiled() {
        return status == FiscalPeriodStatus.TAX_FILED;
    }

    public boolean canPostJournalEntry() {
        return status == FiscalPeriodStatus.OPEN;
    }

    public boolean canCloseMonth() {
        return status == FiscalPeriodStatus.OPEN;
    }

    public boolean canFileTax() {
        return status == FiscalPeriodStatus.MONTH_CLOSED;
    }

    public boolean canReopen() {
        return status == FiscalPeriodStatus.MONTH_CLOSED;
    }

    public LocalDate getStartDate() {
        return YearMonth.of(year, month).atDay(1);
    }

    public LocalDate getEndDate() {
        return YearMonth.of(year, month).atEndOfMonth();
    }

    public String getPeriodName() {
        return String.format("%04d-%02d", year, month);
    }

    public String getPeriodDisplayName() {
        String[] monthNames = {"", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
        return monthNames[month] + " " + year;
    }
}
