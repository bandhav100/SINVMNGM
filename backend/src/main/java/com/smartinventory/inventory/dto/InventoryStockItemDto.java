package com.smartinventory.inventory.dto;

import java.math.BigDecimal;

public record InventoryStockItemDto(
        Long id,
        String name,
        String sku,
        Long categoryId,
        String categoryName,
        BigDecimal price,
        Integer quantity,
        Integer reorderLevel,
        String stockStatus,
        BigDecimal potentialRetailValue
) {}
