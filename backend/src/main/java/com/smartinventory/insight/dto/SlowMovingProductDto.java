package com.smartinventory.insight.dto;

import java.time.Instant;
import java.time.LocalDate;

public record SlowMovingProductDto(
        Long productId,
        String productName,
        String sku,
        String categoryName,
        Integer currentStock,
        Integer reorderLevel,
        Long unitsSoldInPeriod,
        String classification,
        Instant lastSoldDate,
        LocalDate periodStart,
        LocalDate periodEnd
) {}
