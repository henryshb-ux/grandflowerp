package com.artivisi.accountingfinance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for executing journal templates.
 *
 * For SIMPLE templates: use 'amount' field
 * For DETAILED templates: use 'variables' map with formula variable names as keys
 * For templates with dynamic accounts: use 'accountMappings' to specify accounts for lines with null account
 */
public record ExecuteTemplateDto(
        @NotNull(message = "Transaction date is required")
        LocalDate transactionDate,

        // Note: No validation here - for DETAILED templates, amount can be null/0
        // For SIMPLE templates, validation is done in TemplateExecutionEngine.validate()
        BigDecimal amount,

        @NotBlank(message = "Description is required")
        String description,

        /**
         * Variables for DETAILED templates.
         * Key: formula variable name (e.g., "kas", "bankBca")
         * Value: amount for that variable
         */
        Map<String, BigDecimal> variables,

        /**
         * Account mappings for template lines with dynamic account selection (null account).
         * Key: lineOrder (Integer as String)
         * Value: selected accountId (UUID as String)
         */
        Map<String, String> accountMappings
) {
    /**
     * Returns non-null variables map (empty if null).
     */
    public Map<String, BigDecimal> safeVariables() {
        return variables != null ? variables : Map.of();
    }

    /**
     * Returns non-null account mappings (empty if null).
     */
    public Map<String, String> safeAccountMappings() {
        return accountMappings != null ? accountMappings : Map.of();
    }

    /**
     * Get account ID for a specific line order.
     * @param lineOrder the line order number
     * @return UUID of the selected account, or null if not mapped
     */
    public UUID getAccountIdForLine(int lineOrder) {
        String accountIdStr = safeAccountMappings().get(String.valueOf(lineOrder));
        if (accountIdStr == null || accountIdStr.isBlank()) {
            return null;
        }
        return UUID.fromString(accountIdStr);
    }
}
