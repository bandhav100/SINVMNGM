package com.smartinventory.report.dto;

import java.math.BigDecimal;

public record SalesReportSummaryDto(
        long completedOrdersCount,
        long totalUnitsSold,
        BigDecimal totalRevenue,
        BigDecimal averageOrderValue
) {}
