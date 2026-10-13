package com.smartinventory.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DashboardActivityItemDto(
        String id,
        String type,
        String title,
        String subtitle,
        String status,
        BigDecimal amount,
        Integer quantity,
        Instant timestamp,
        String link
) {}
