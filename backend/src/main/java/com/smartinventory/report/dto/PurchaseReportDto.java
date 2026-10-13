package com.smartinventory.report.dto;

import com.smartinventory.common.dto.PageResponseDto;

import java.time.LocalDate;
import java.util.List;

public record PurchaseReportDto(
        LocalDate startDate,
        LocalDate endDate,
        PurchaseReportSummaryDto summary,
        List<SupplierPurchaseSummaryDto> supplierSummaries,
        PageResponseDto<PurchaseReportItemDto> items
) {}
