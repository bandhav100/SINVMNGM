package com.smartinventory.insight.dto;

import java.math.BigDecimal;

public record ProductValuationItemDto(
        Long productId,
        String productName,
        String sku,
        String categoryName,
        Integer quantity,
        BigDecimal unitSellingPrice,
        BigDecimal totalRetailValue,
        boolean costValuationAvailable
) {}
