package com.smartinventory.report.dto;

import com.smartinventory.common.dto.PageResponseDto;

public record StockValuationReportDto(
        boolean costValuationAvailable,
        String explanation,
        StockValuationSummaryDto summary,
        PageResponseDto<StockValuationReportItemDto> items
) {}
