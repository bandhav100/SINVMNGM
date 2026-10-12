package com.smartinventory.sale.controller;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.sale.dto.*;
import com.smartinventory.sale.model.SalesOrderStatus;
import com.smartinventory.sale.service.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/sales")
@Tag(name = "Sales Management", description = "Endpoints for managing sales orders and inventory stock deductions")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    @Operation(summary = "Create draft sales order", description = "Creates a new sales order in DRAFT status")
    public ResponseEntity<SalesOrderResponseDto> createSaleOrder(
            @Valid @RequestBody CreateSalesOrderRequestDto request,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        SalesOrderResponseDto created = saleService.createSaleOrder(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List sales orders", description = "Retrieves paginated sales orders with optional search, status, and date filters")
    public ResponseEntity<PageResponseDto<SalesOrderSummaryDto>> getSalesOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SalesOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponseDto<SalesOrderSummaryDto> page = saleService.getSalesOrders(search, status, startDate, endDate, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sales order by ID", description = "Retrieves complete sales order details including line items and audit info")
    public ResponseEntity<SalesOrderResponseDto> getSaleOrderById(@PathVariable Long id) {
        SalesOrderResponseDto order = saleService.getSaleOrderById(id);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update sales order", description = "Updates a draft sales order's items, customer details, or notes")
    public ResponseEntity<SalesOrderResponseDto> updateSaleOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSalesOrderRequestDto request,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        SalesOrderResponseDto updated = saleService.updateSaleOrder(id, request, username);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete draft sales order", description = "Deletes a draft sales order. Only allowed for orders in DRAFT status.")
    public ResponseEntity<Void> deleteSaleOrder(@PathVariable Long id) {
        saleService.deleteSaleOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete sales order", description = "Atomically completes a draft sales order, deducts stock, and records SALE_DEDUCTION ledger entries")
    public ResponseEntity<SalesOrderResponseDto> completeSaleOrder(
            @PathVariable Long id,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        SalesOrderResponseDto completed = saleService.completeSaleOrder(id, username);
        return ResponseEntity.ok(completed);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel sales order", description = "Cancels an eligible draft sales order")
    public ResponseEntity<SalesOrderResponseDto> cancelSaleOrder(
            @PathVariable Long id,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        SalesOrderResponseDto cancelled = saleService.cancelSaleOrder(id, username);
        return ResponseEntity.ok(cancelled);
    }
}
