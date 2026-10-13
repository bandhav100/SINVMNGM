package com.smartinventory.purchase.controller;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.purchase.dto.CreatePurchaseOrderRequestDto;
import com.smartinventory.purchase.dto.PurchaseOrderResponseDto;
import com.smartinventory.purchase.dto.PurchaseOrderSummaryDto;
import com.smartinventory.purchase.dto.UpdatePurchaseOrderRequestDto;
import com.smartinventory.purchase.model.PurchaseOrderStatus;
import com.smartinventory.purchase.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/purchases")
@Tag(name = "Purchases", description = "Endpoints for managing purchase orders, supplier procurement, and goods receiving")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    @Operation(summary = "Create purchase order", description = "Creates a new purchase order with line items in DRAFT or ORDERED status")
    public ResponseEntity<PurchaseOrderResponseDto> createPurchaseOrder(
            @Valid @RequestBody CreatePurchaseOrderRequestDto request,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        PurchaseOrderResponseDto created = purchaseService.createPurchaseOrder(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List and search purchase orders", description = "Retrieves a paginated list of purchase orders with optional filtering")
    public ResponseEntity<PageResponseDto<PurchaseOrderSummaryDto>> getPurchaseOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @SortDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponseDto<PurchaseOrderSummaryDto> page = purchaseService.getPurchaseOrders(
                search,
                status,
                supplierId,
                startDate,
                endDate,
                pageable
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase order by ID", description = "Retrieves complete purchase order details including items and audit info")
    public ResponseEntity<PurchaseOrderResponseDto> getPurchaseOrderById(@PathVariable Long id) {
        PurchaseOrderResponseDto order = purchaseService.getPurchaseOrderById(id);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update purchase order", description = "Updates a draft purchase order's items, supplier, delivery date, or notes")
    public ResponseEntity<PurchaseOrderResponseDto> updatePurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePurchaseOrderRequestDto request,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        PurchaseOrderResponseDto updated = purchaseService.updatePurchaseOrder(id, request, username);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete draft purchase order", description = "Deletes a draft purchase order. Only allowed for orders in DRAFT status.")
    public ResponseEntity<Void> deletePurchaseOrder(@PathVariable Long id) {
        purchaseService.deletePurchaseOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit purchase order", description = "Transitions a draft purchase order to ORDERED status")
    public ResponseEntity<PurchaseOrderResponseDto> submitPurchaseOrder(
            @PathVariable Long id,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        PurchaseOrderResponseDto submitted = purchaseService.submitPurchaseOrder(id, username);
        return ResponseEntity.ok(submitted);
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "Receive purchase order goods", description = "Transitions an ORDERED purchase order to RECEIVED, increments stock, and writes to the inventory ledger")
    public ResponseEntity<PurchaseOrderResponseDto> receivePurchaseOrder(
            @PathVariable Long id,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        PurchaseOrderResponseDto received = purchaseService.receivePurchaseOrder(id, username);
        return ResponseEntity.ok(received);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel purchase order", description = "Cancels a DRAFT or ORDERED purchase order")
    public ResponseEntity<PurchaseOrderResponseDto> cancelPurchaseOrder(
            @PathVariable Long id,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        PurchaseOrderResponseDto cancelled = purchaseService.cancelPurchaseOrder(id, username);
        return ResponseEntity.ok(cancelled);
    }
}
