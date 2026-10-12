package com.smartinventory.report.dto;

import java.math.BigDecimal;

public record TopSellingProductDto(
        Long productId,
        String productName,
        String sku,
        long unitsSold,
        BigDecimal totalRevenue
) {}
