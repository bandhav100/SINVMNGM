package com.smartinventory.report.dto;

import com.smartinventory.common.dto.PageResponseDto;

import java.time.LocalDate;
import java.util.List;

public record SalesReportDto(
        LocalDate startDate,
        LocalDate endDate,
        SalesReportSummaryDto summary,
        List<TopSellingProductDto> topSellingProducts,
        PageResponseDto<SalesReportItemDto> items
) {}
