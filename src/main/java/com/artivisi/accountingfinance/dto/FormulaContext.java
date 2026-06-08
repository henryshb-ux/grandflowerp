package com.artivisi.accountingfinance.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Context object for formula evaluation.
 * Provides variables accessible within formula expressions.
 *
 * <p>Core variables:
 * <ul>
 *   <li>{@code amount} - the transaction amount (always available)</li>
 * </ul>
 *
 * <p>Extended variables via Map:
 * External modules (payroll, inventory, etc.) can provide additional
 * variables through the variables map without modifying this core class.
 *
 * <p>Usage in formulas:
 * <ul>
 *   <li>{@code amount} - direct access to amount field</li>
 *   <li>{@code amount * 0.11} - percentage calculation</li>
 *   <li>{@code variables['grossSalary']} - access extended variable</li>
 *   <li>{@code get('grossSalary')} - convenience method for extended variable</li>
 * </ul>
 */
public record FormulaContext(
        BigDecimal amount,
        Map<String, BigDecimal> variables
) {
    /**
     * Factory method for creating context with amount only.
     * Used by core accounting transactions.
     */
    public static FormulaContext of(BigDecimal amount) {
        return new FormulaContext(amount, Map.of());
    }

    /**
     * Factory method for creating context with amount as long.
     * Used by core accounting transactions.
     */
    public static FormulaContext of(long amount) {
        return new FormulaContext(BigDecimal.valueOf(amount), Map.of());
    }

    /**
     * Factory method for creating context with amount and extended variables.
     * Used by external modules (payroll, inventory, etc.) to provide
     * module-specific variables without coupling to core accounting.
     *
     * @param amount the primary transaction amount
     * @param variables module-specific variables (e.g., grossSalary, companyBpjs)
     */
    public static FormulaContext of(BigDecimal amount, Map<String, BigDecimal> variables) {
        return new FormulaContext(amount, Map.copyOf(variables));
    }

    /**
     * Convenience method to get an extended variable by name.
     * Returns BigDecimal.ZERO if not found.
     */
    public BigDecimal get(String name) {
        return variables.getOrDefault(name, BigDecimal.ZERO);
    }

    /**
     * Builder for creating context with fluent API.
     * Useful for modules that need to set multiple variables.
     */
    public static Builder builder(BigDecimal amount) {
        return new Builder(amount);
    }

    public static class Builder {
        private final BigDecimal amount;
        private final Map<String, BigDecimal> variables = new HashMap<>();

        private Builder(BigDecimal amount) {
            this.amount = amount;
        }

        public Builder variable(String name, BigDecimal value) {
            variables.put(name, value);
            return this;
        }

        public FormulaContext build() {
            return new FormulaContext(amount, Map.copyOf(variables));
        }
    }
}
