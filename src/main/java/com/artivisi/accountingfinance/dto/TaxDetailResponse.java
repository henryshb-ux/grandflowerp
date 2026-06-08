package com.artivisi.accountingfinance.dto;

import com.artivisi.accountingfinance.entity.TaxTransactionDetail;
import com.artivisi.accountingfinance.enums.TaxType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TaxDetailResponse(
        UUID id,
        UUID transactionId,
        TaxType taxType,
        String counterpartyName,

        // e-Faktur fields
        String fakturNumber,
        LocalDate fakturDate,
        String transactionCode,
        BigDecimal dpp,
        BigDecimal ppn,
        BigDecimal ppnbm,

        // e-Bupot fields
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
        String counterpartyAddress,

        // Timestamps
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TaxDetailResponse from(TaxTransactionDetail detail) {
        return new TaxDetailResponse(
                detail.getId(),
                detail.getTransaction() != null ? detail.getTransaction().getId() : null,
                detail.getTaxType(),
                detail.getCounterpartyName(),
                detail.getFakturNumber(),
                detail.getFakturDate(),
                detail.getTransactionCode(),
                detail.getDpp(),
                detail.getPpn(),
                detail.getPpnbm(),
                detail.getBupotNumber(),
                detail.getTaxObjectCode(),
                detail.getGrossAmount(),
                detail.getTaxRate(),
                detail.getTaxAmount(),
                detail.getCounterpartyNpwp(),
                detail.getCounterpartyNitku(),
                detail.getCounterpartyNik(),
                detail.getCounterpartyIdType(),
                detail.getCounterpartyAddress(),
                detail.getCreatedAt(),
                detail.getUpdatedAt()
        );
    }
}
