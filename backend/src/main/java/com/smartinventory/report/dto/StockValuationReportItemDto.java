package com.smartinventory.report.dto;

import java.math.BigDecimal;

public record StockValuationReportItemDto(
        Long productId,
        String productName,
        String sku,
        String categoryName,
        Integer currentStock,
        BigDecimal retailPrice,
        BigDecimal potentialRetailValue,
        BigDecimal purchaseCost,
        BigDecimal costValuation
) {}
