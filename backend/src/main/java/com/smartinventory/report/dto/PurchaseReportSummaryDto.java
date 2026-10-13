package com.smartinventory.report.dto;

import java.math.BigDecimal;

public record PurchaseReportSummaryDto(
        long totalOrdersCount,
        long receivedOrdersCount,
        long orderedPendingCount,
        long draftOrdersCount,
        long cancelledOrdersCount,
        long receivedUnits,
        BigDecimal receivedTotalCost
) {}
