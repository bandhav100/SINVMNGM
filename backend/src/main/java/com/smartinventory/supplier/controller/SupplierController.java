package com.smartinventory.supplier.controller;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.supplier.dto.SupplierRequestDto;
import com.smartinventory.supplier.dto.SupplierResponseDto;
import com.smartinventory.supplier.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/suppliers")
@Tag(name = "Suppliers", description = "Endpoints for managing inventory suppliers and vendors")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @Operation(summary = "Create supplier", description = "Creates a new supplier or vendor record")
    public ResponseEntity<SupplierResponseDto> createSupplier(@Valid @RequestBody SupplierRequestDto request) {
        SupplierResponseDto created = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List suppliers", description = "Retrieves a paginated list of suppliers with optional search and active filter")
    public ResponseEntity<PageResponseDto<SupplierResponseDto>> getSuppliers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @SortDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponseDto<SupplierResponseDto> suppliers = supplierService.getSuppliers(search, active, pageable);
        return ResponseEntity.ok(suppliers);
    }

    @GetMapping("/active")
    @Operation(summary = "List active suppliers", description = "Retrieves all active suppliers for purchasing workflows")
    public ResponseEntity<List<SupplierResponseDto>> getActiveSuppliers() {
        return ResponseEntity.ok(supplierService.getActiveSuppliers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get supplier by ID", description = "Retrieves details for a single supplier")
    public ResponseEntity<SupplierResponseDto> getSupplierById(@PathVariable Long id) {
        SupplierResponseDto supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(supplier);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update supplier", description = "Updates supplier profile, contact information, or status")
    public ResponseEntity<SupplierResponseDto> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequestDto request
    ) {
        SupplierResponseDto updated = supplierService.updateSupplier(id, request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Toggle supplier status", description = "Activates or deactivates a supplier record")
    public ResponseEntity<SupplierResponseDto> toggleSupplierStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> statusUpdate
    ) {
        boolean active = statusUpdate.getOrDefault("active", true);
        SupplierResponseDto updated = supplierService.toggleSupplierStatus(id, active);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete supplier", description = "Removes a supplier record")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}
