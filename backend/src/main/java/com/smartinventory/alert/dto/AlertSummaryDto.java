package com.smartinventory.alert.dto;

public record AlertSummaryDto(
        long totalAlerts,
        long criticalCount,
        long warningCount,
        long reorderRequiredCount
) {}
