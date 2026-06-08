package com.artivisi.accountingfinance.dto;

import com.artivisi.accountingfinance.enums.TaxType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxDetailRequest(
        @NotNull(message = "Jenis pajak wajib diisi")
        TaxType taxType,

        @NotBlank(message = "Nama lawan transaksi wajib diisi")
        String counterpartyName,

        // e-Faktur fields (PPN)
        String fakturNumber,
        LocalDate fakturDate,
        String transactionCode,
        BigDecimal dpp,
        BigDecimal ppn,
        BigDecimal ppnbm,

        // e-Bupot fields (PPh)
        String bupotNumber,
        String taxObjectCode,
        BigDecimal grossAmount,
        BigDecimal taxRate,
        BigDecimal taxAmount,

        // Counterparty fields
        String counterpartyNpwp,
        String counterpartyNitku,
        String counterpartyNik,
        String counterpartyIdType,
        String counterpartyAddress
) {}
