package com.artivisi.accountingfinance.dto;

import java.util.UUID;

public record ProductOptionDto(
        UUID id,
        String code,
        String name
) {}
