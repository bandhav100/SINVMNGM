package com.smartinventory.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryDto(
        long totalProducts,
        long totalStockUnits,
        long lowStockCount,
        long outOfStockCount,
        long reorderRequiredCount,
        BigDecimal potentialRetailValuation,

        long completedSalesCount,
        long draftSalesCount,
        long cancelledSalesCount,
        BigDecimal completedSalesRevenue,

        boolean purchaseMetricsVisible,
        long totalPurchasesCount,
        long receivedPurchasesCount,
        long pendingPurchasesCount,
        BigDecimal receivedPurchasesTotalCost,

        List<DashboardAlertItemDto> criticalAlerts,
        List<DashboardActivityItemDto> recentActivity
) {}
