package com.artivisi.accountingfinance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Request to create a free-form journal entry (no template required)")
public record JournalEntryRequest(

        @NotNull(message = "Transaction date is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Transaction date", example = "2025-12-31")
        LocalDate transactionDate,

        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must not exceed 500 characters")
        @Schema(description = "Journal entry description", example = "Closing journal - accrued expenses")
        String description,

        @Schema(description = "Optional category stored in transaction notes", example = "CLOSING")
        String category,

        @NotNull(message = "Journal lines are required")
        @Size(min = 2, message = "At least 2 journal lines are required")
        @Valid
        @Schema(description = "Debit/credit lines (min 2)")
        List<JournalLineRequest> lines
) {

    @Schema(description = "A single debit or credit line in the journal entry")
    public record JournalLineRequest(

            @NotNull(message = "Account ID is required")
            @Schema(description = "Chart of account UUID (must be a leaf account, not header)")
            UUID accountId,

            @NotNull(message = "Debit amount is required")
            @DecimalMin(value = "0", message = "Debit amount cannot be negative")
            @Schema(description = "Debit amount (set 0 if this is a credit line)", example = "1000000")
            BigDecimal debit,

            @NotNull(message = "Credit amount is required")
            @DecimalMin(value = "0", message = "Credit amount cannot be negative")
            @Schema(description = "Credit amount (set 0 if this is a debit line)", example = "0")
            BigDecimal credit
    ) {}
}
