package com.smartinventory.report.dto;

import com.smartinventory.common.dto.PageResponseDto;

public record InventoryReportDto(
        InventoryReportSummaryDto summary,
        PageResponseDto<InventoryReportItemDto> items
) {}
