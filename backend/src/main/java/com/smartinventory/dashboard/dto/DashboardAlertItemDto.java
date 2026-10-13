package com.smartinventory.dashboard.dto;

public record DashboardAlertItemDto(
        Long productId,
        String productName,
        String sku,
        int currentQuantity,
        int reorderLevel,
        String severity
) {}
