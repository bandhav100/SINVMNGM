package com.smartinventory.report.dto;

import java.time.Instant;

public record InventoryMovementItemDto(
        Long id,
        Long productId,
        String productName,
        String sku,
        String transactionType,
        Integer quantityDelta,
        Integer quantityBefore,
        Integer quantityAfter,
        String referenceType,
        Long referenceId,
        String performedBy,
        String notes,
        Instant createdAt
) {}
