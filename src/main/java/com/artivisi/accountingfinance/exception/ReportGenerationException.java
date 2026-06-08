package com.artivisi.accountingfinance.exception;

/**
 * Exception thrown when report generation (PDF/Excel) fails.
 */
public class ReportGenerationException extends RuntimeException {

    public ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
