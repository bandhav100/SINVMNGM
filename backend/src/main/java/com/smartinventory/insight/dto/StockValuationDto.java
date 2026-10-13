package com.smartinventory.insight.dto;

import com.smartinventory.common.dto.PageResponseDto;
import java.math.BigDecimal;

public record StockValuationDto(
        boolean costValuationAvailable,
        String explanation,
        String currency,
        long totalStockUnits,
        BigDecimal totalRetailValue,
        PageResponseDto<ProductValuationItemDto> products
) {}
