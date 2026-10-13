package com.smartinventory.inventory.controller;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.inventory.dto.*;
import com.smartinventory.inventory.model.InventoryTransactionType;
import com.smartinventory.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory Management", description = "Endpoints for inventory stock levels, transaction ledger, and authorized stock adjustments")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/stock")
    @Operation(summary = "Get inventory stock levels", description = "Retrieves paginated product stock levels with status and category filtering")
    public ResponseEntity<PageResponseDto<InventoryStockItemDto>> getInventoryStock(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String search,
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(inventoryService.getInventoryStock(categoryId, status, search, pageable));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get inventory summary counters", description = "Returns aggregated counts of products, units, out-of-stock, and total valuation")
    public ResponseEntity<InventorySummaryDto> getInventorySummary() {
        return ResponseEntity.ok(inventoryService.getInventorySummary());
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get inventory transaction ledger", description = "Returns paginated movement history for auditing stock ins and outs")
    public ResponseEntity<PageResponseDto<InventoryTransactionDto>> getTransactions(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) InventoryTransactionType transactionType,
            @RequestParam(required = false) String search,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(inventoryService.getTransactions(productId, transactionType, search, pageable));
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    @Operation(summary = "Perform authorized stock adjustment", description = "Atomically updates stock level and logs audit adjustment and transaction ledger entries")
    public ResponseEntity<StockAdjustmentResponseDto> adjustStock(
            @Valid @RequestBody StockAdjustmentRequestDto request,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        StockAdjustmentResponseDto response = inventoryService.adjustStock(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/adjustments")
    @Operation(summary = "List stock adjustments", description = "Retrieves audit log of manual stock adjustments")
    public ResponseEntity<PageResponseDto<StockAdjustmentResponseDto>> getAdjustments(
            @RequestParam(required = false) Long productId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(inventoryService.getAdjustments(productId, pageable));
    }
}
