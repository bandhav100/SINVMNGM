package com.smartinventory.alert.dto;

import com.smartinventory.alert.model.AlertSeverity;
import com.smartinventory.alert.model.AlertType;

import java.time.Instant;

public record LowStockAlertDto(
        String id,
        Long productId,
        String productName,
        String sku,
        String categoryName,
        Integer currentStock,
        Integer reorderLevel,
        AlertType alertType,
        AlertSeverity severity,
        String message,
        Integer suggestedReplenishmentQuantity,
        Instant createdAt
) {}
