package com.smartinventory.report.dto;

public record InventoryReportSummaryDto(
        long totalProducts,
        long totalStockUnits,
        long inStockCount,
        long lowStockCount,
        long outOfStockCount
) {}
