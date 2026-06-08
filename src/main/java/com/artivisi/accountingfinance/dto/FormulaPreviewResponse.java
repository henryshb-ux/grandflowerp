package com.artivisi.accountingfinance.dto;

import java.math.BigDecimal;
import java.util.List;

public record FormulaPreviewResponse(
        boolean valid,
        BigDecimal result,
        String formattedResult,
        List<String> errors
) {
    public static FormulaPreviewResponse success(BigDecimal result, String formattedResult) {
        return new FormulaPreviewResponse(true, result, formattedResult, List.of());
    }

    public static FormulaPreviewResponse error(List<String> errors) {
        return new FormulaPreviewResponse(false, null, null, errors);
    }
}
