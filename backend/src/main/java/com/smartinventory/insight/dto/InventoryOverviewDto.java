package com.smartinventory.insight.dto;

import java.math.BigDecimal;

public record InventoryOverviewDto(
        long totalProducts,
        long totalStockUnits,
        long outOfStockCount,
        long lowStockCount,
        long reorderRequiredCount,
        boolean costValuationAvailable,
        String valuationExplanation,
        BigDecimal totalPotentialRetailValue
) {}
