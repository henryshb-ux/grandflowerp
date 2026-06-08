package com.artivisi.accountingfinance.controller.api;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Shared response wrapper for all analysis API endpoints.
 */
public record AnalysisResponse<T>(
        String reportType,
        LocalDateTime generatedAt,
        Map<String, String> parameters,
        T data,
        Map<String, String> metadata
) {}
