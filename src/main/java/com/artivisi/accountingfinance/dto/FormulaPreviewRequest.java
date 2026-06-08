package com.artivisi.accountingfinance.dto;

import java.math.BigDecimal;

public record FormulaPreviewRequest(
        String formula,
        BigDecimal amount
) {
}
