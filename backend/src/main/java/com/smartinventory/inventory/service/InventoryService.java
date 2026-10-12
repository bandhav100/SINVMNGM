package com.smartinventory.inventory.service;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.inventory.dto.*;
import com.smartinventory.inventory.model.InventoryTransactionType;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    PageResponseDto<InventoryStockItemDto> getInventoryStock(
            Long categoryId,
            String status,
            String search,
            Pageable pageable
    );

    InventorySummaryDto getInventorySummary();

    PageResponseDto<InventoryTransactionDto> getTransactions(
            Long productId,
            InventoryTransactionType transactionType,
            String search,
            Pageable pageable
    );

    StockAdjustmentResponseDto adjustStock(StockAdjustmentRequestDto request, String username);

    PageResponseDto<StockAdjustmentResponseDto> getAdjustments(Long productId, Pageable pageable);
}
