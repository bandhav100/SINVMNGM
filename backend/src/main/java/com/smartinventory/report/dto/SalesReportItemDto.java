package com.smartinventory.report.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SalesReportItemDto(
        Long orderId,
        String orderNumber,
        Instant completedAt,
        String customerName,
        String customerContact,
        int totalUnits,
        BigDecimal totalAmount,
        String status
) {}
