package com.smartinventory.inventory.dto;

import java.time.Instant;

public record StockAdjustmentResponseDto(
        Long id,
        Long productId,
        String productName,
        String sku,
        Integer quantityDelta,
        Integer quantityBefore,
        Integer quantityAfter,
        String reason,
        String notes,
        String adjustedByUsername,
        Instant createdAt
) {}
