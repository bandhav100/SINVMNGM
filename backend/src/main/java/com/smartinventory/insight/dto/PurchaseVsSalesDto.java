package com.smartinventory.insight.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseVsSalesDto(
        LocalDate periodStart,
        LocalDate periodEnd,
        Long purchasedUnits,
        BigDecimal purchaseCostTotal,
        Long soldUnits,
        BigDecimal salesRevenueTotal,
        Long netUnitsDelta
) {}
