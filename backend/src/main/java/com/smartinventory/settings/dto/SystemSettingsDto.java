package com.smartinventory.settings.dto;

import java.time.Instant;

public record SystemSettingsDto(
        Long id,
        String companyName,
        String companyEmail,
        String companyPhone,
        String currency,
        String timezone,
        Integer defaultReorderThreshold,
        Boolean lowStockAlertsEnabled,
        Boolean outOfStockAlertsEnabled,
        Boolean allowNegativeStock,
        Boolean requirePurchaseApproval,
        Instant updatedAt,
        String updatedBy
) {}
