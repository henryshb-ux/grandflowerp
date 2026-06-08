package com.artivisi.accountingfinance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for patching a PENDING draft transaction.
 * All fields are optional — only provided fields are updated.
 */
public record UpdateDraftRequest(

        @Size(max = 255, message = "Merchant name must not exceed 255 characters")
        String merchantName,

        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        UUID suggestedTemplateId,

        @Size(max = 100, message = "Category must not exceed 100 characters")
        String category
) {}
