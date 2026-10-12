package com.smartinventory.inventory.dto;

import java.math.BigDecimal;

public record InventorySummaryDto(
        long totalProducts,
        long totalStockUnits,
        long inStockCount,
        long lowStockCount,
        long outOfStockCount,
        long reorderRequiredCount,
        BigDecimal totalRetailValue
) {}
