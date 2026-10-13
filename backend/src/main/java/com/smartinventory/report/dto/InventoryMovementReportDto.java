package com.smartinventory.report.dto;

import com.smartinventory.common.dto.PageResponseDto;

import java.time.LocalDate;

public record InventoryMovementReportDto(
        LocalDate startDate,
        LocalDate endDate,
        InventoryMovementSummaryDto summary,
        PageResponseDto<InventoryMovementItemDto> items
) {}
