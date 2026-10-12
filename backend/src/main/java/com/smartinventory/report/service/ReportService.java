package com.smartinventory.report.service;

import com.smartinventory.inventory.model.InventoryTransactionType;
import com.smartinventory.purchase.model.PurchaseOrderStatus;
import com.smartinventory.report.dto.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ReportService {

    InventoryReportDto getInventoryReport(Long categoryId, String status, String search, Pageable pageable);

    SalesReportDto getSalesReport(LocalDate startDate, LocalDate endDate, Integer days, String search, Pageable pageable);

    PurchaseReportDto getPurchaseReport(LocalDate startDate, LocalDate endDate, Integer days, PurchaseOrderStatus status, Long supplierId, String search, Pageable pageable);

    InventoryMovementReportDto getInventoryMovementReport(Long productId, InventoryTransactionType transactionType, LocalDate startDate, LocalDate endDate, Integer days, Pageable pageable);

    StockValuationReportDto getStockValuationReport(Long categoryId, Pageable pageable);
}
