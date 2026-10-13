package com.smartinventory.report.dto;

public record InventoryMovementSummaryDto(
        long totalMovements,
        long totalInboundUnits,
        long totalOutboundUnits,
        long netQuantityChange
) {}
