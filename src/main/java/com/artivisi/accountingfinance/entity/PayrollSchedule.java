package com.artivisi.accountingfinance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "payroll_schedule")
@Getter
@Setter
@NoArgsConstructor
public class PayrollSchedule extends TimestampedEntity {

    @NotNull
    @Min(1)
    @Max(28)
    @Column(name = "day_of_month", nullable = false)
    private Integer dayOfMonth;

    @NotNull
    @Column(name = "base_salary", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseSalary;

    @NotNull
    @Min(1)
    @Max(5)
    @Column(name = "jkk_risk_class", nullable = false)
    private Integer jkkRiskClass;

    @NotNull
    @Column(name = "auto_calculate", nullable = false)
    private Boolean autoCalculate = true;

    @NotNull
    @Column(name = "auto_approve", nullable = false)
    private Boolean autoApprove = false;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
