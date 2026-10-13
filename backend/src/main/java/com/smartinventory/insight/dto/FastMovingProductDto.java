package com.smartinventory.insight.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FastMovingProductDto(
        Long productId,
        String productName,
        String sku,
        String categoryName,
        Long unitsSold,
        BigDecimal totalRevenue,
        LocalDate periodStart,
        LocalDate periodEnd
) {}
