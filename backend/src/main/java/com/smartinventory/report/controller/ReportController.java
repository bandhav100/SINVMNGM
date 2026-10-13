package com.smartinventory.report.controller;

import com.smartinventory.inventory.model.InventoryTransactionType;
import com.smartinventory.purchase.model.PurchaseOrderStatus;
import com.smartinventory.report.dto.*;
import com.smartinventory.report.service.ReportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/inventory")
    public ResponseEntity<InventoryReportDto> getInventoryReport(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String safeSortBy = ("price".equalsIgnoreCase(sortBy) || "quantity".equalsIgnoreCase(sortBy) || "sku".equalsIgnoreCase(sortBy)) ? sortBy.toLowerCase() : "name";

        InventoryReportDto report = reportService.getInventoryReport(
                categoryId,
                status,
                search,
                PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy))
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/sales")
    public ResponseEntity<SalesReportDto> getSalesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "30") Integer days,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);

        SalesReportDto report = reportService.getSalesReport(
                startDate,
                endDate,
                days,
                search,
                PageRequest.of(safePage, safeSize)
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/purchases")
    public ResponseEntity<PurchaseReportDto> getPurchaseReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "30") Integer days,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);

        PurchaseReportDto report = reportService.getPurchaseReport(
                startDate,
                endDate,
                days,
                status,
                supplierId,
                search,
                PageRequest.of(safePage, safeSize)
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/inventory-movements")
    public ResponseEntity<InventoryMovementReportDto> getInventoryMovementReport(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) InventoryTransactionType transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "30") Integer days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);

        InventoryMovementReportDto report = reportService.getInventoryMovementReport(
                productId,
                transactionType,
                startDate,
                endDate,
                days,
                PageRequest.of(safePage, safeSize)
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/stock-valuation")
    public ResponseEntity<StockValuationReportDto> getStockValuationReport(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);

        StockValuationReportDto report = reportService.getStockValuationReport(
                categoryId,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "name"))
        );
        return ResponseEntity.ok(report);
    }
}
