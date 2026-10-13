package com.smartinventory.report.dto;

import java.math.BigDecimal;

public record InventoryReportItemDto(
        Long productId,
        String productName,
        String sku,
        String categoryName,
        Integer currentStock,
        Integer reorderLevel,
        BigDecimal price,
        String stockStatus
) {}
