package com.smartinventory.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PurchaseReportItemDto(
        Long orderId,
        String orderNumber,
        String supplierName,
        String status,
        Instant orderedAt,
        Instant receivedAt,
        LocalDate expectedDeliveryDate,
        int totalUnits,
        BigDecimal totalAmount
) {}
