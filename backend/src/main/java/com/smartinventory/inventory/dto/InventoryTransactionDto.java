package com.smartinventory.inventory.dto;

import com.smartinventory.inventory.model.InventoryTransactionType;
import java.time.Instant;

public record InventoryTransactionDto(
        Long id,
        Long productId,
        String productName,
        String sku,
        InventoryTransactionType transactionType,
        Integer quantityDelta,
        Integer quantityBefore,
        Integer quantityAfter,
        String referenceType,
        Long referenceId,
        String performedByUsername,
        String notes,
        Instant createdAt
) {}
