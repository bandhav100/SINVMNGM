package com.smartinventory.report.dto;

import java.math.BigDecimal;

public record SupplierPurchaseSummaryDto(
        Long supplierId,
        String supplierName,
        long totalOrders,
        long receivedOrders,
        BigDecimal totalSpent
) {}
