package com.smartinventory.report.dto;

import java.math.BigDecimal;

public record StockValuationSummaryDto(
        long totalActiveProducts,
        long totalStockUnits,
        BigDecimal totalPotentialRetailValue
) {}
