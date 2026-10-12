package com.smartinventory.report.service.impl;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.inventory.entity.InventoryTransaction;
import com.smartinventory.inventory.model.InventoryTransactionType;
import com.smartinventory.inventory.repository.InventoryTransactionRepository;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.purchase.entity.PurchaseOrder;
import com.smartinventory.purchase.entity.PurchaseOrderItem;
import com.smartinventory.purchase.model.PurchaseOrderStatus;
import com.smartinventory.purchase.repository.PurchaseOrderItemRepository;
import com.smartinventory.purchase.repository.PurchaseOrderRepository;
import com.smartinventory.report.dto.*;
import com.smartinventory.report.service.ReportService;
import com.smartinventory.sale.entity.SalesOrder;
import com.smartinventory.sale.entity.SalesOrderItem;
import com.smartinventory.sale.repository.SalesOrderItemRepository;
import com.smartinventory.sale.repository.SalesOrderRepository;
import com.smartinventory.sale.repository.projection.FastMovingProductProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ProductRepository productRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public ReportServiceImpl(
            ProductRepository productRepository,
            SalesOrderRepository salesOrderRepository,
            SalesOrderItemRepository salesOrderItemRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderItemRepository purchaseOrderItemRepository,
            InventoryTransactionRepository inventoryTransactionRepository
    ) {
        this.productRepository = productRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @Override
    public InventoryReportDto getInventoryReport(Long categoryId, String status, String search, Pageable pageable) {
        long totalProducts = productRepository.countTotalProducts();
        long totalStockUnits = productRepository.sumTotalStockUnits();
        long outOfStock = productRepository.countOutOfStock();
        long lowStock = productRepository.countLowStock();
        long inStock = Math.max(0, totalProducts - outOfStock - lowStock);

        InventoryReportSummaryDto summary = new InventoryReportSummaryDto(
                totalProducts,
                totalStockUnits,
                inStock,
                lowStock,
                outOfStock
        );

        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : "ALL";
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<Product> page = productRepository.searchInventoryReport(categoryId, cleanStatus, cleanSearch, pageable);

        PageResponseDto<InventoryReportItemDto> items = PageResponseDto.fromPage(page, product -> {
            int qty = product.getQuantity() != null ? product.getQuantity() : 0;
            int reorder = product.getReorderLevel() != null ? product.getReorderLevel() : 0;

            String stockStatus;
            if (qty <= 0) {
                stockStatus = "OUT_OF_STOCK";
            } else if (qty <= reorder) {
                stockStatus = "LOW_STOCK";
            } else {
                stockStatus = "IN_STOCK";
            }

            return new InventoryReportItemDto(
                    product.getId(),
                    product.getName(),
                    product.getSku(),
                    product.getCategory() != null ? product.getCategory().getName() : null,
                    qty,
                    reorder,
                    product.getPrice(),
                    stockStatus
            );
        });

        return new InventoryReportDto(summary, items);
    }

    @Override
    public SalesReportDto getSalesReport(LocalDate startDate, LocalDate endDate, Integer days, String search, Pageable pageable) {
        DateRange range = resolveDateRange(startDate, endDate, days);

        long completedOrdersCount = salesOrderRepository.countCompletedOrdersInRange(range.startInstant(), range.endInstant());
        Long totalUnitsSoldLong = salesOrderItemRepository.sumCompletedSalesUnits(range.startInstant(), range.endInstant());
        long totalUnitsSold = totalUnitsSoldLong != null ? totalUnitsSoldLong : 0L;

        BigDecimal totalRevenue = salesOrderItemRepository.sumCompletedSalesRevenue(range.startInstant(), range.endInstant());
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (completedOrdersCount > 0) {
            averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(completedOrdersCount), 2, RoundingMode.HALF_UP);
        }

        SalesReportSummaryDto summary = new SalesReportSummaryDto(
                completedOrdersCount,
                totalUnitsSold,
                totalRevenue,
                averageOrderValue
        );

        // Top 10 selling products
        List<FastMovingProductProjection> projections = salesOrderItemRepository.findFastMovingProducts(
                range.startInstant(),
                range.endInstant(),
                PageRequest.of(0, 10)
        );
        List<TopSellingProductDto> topSellingProducts = new ArrayList<>();
        if (projections != null) {
            for (FastMovingProductProjection proj : projections) {
                topSellingProducts.add(new TopSellingProductDto(
                        proj.getProductId(),
                        proj.getProductName(),
                        proj.getProductSku(),
                        proj.getUnitsSold() != null ? proj.getUnitsSold() : 0L,
                        proj.getTotalRevenue() != null ? proj.getTotalRevenue() : BigDecimal.ZERO
                ));
            }
        }

        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<SalesOrder> ordersPage = salesOrderRepository.searchCompletedOrdersReport(
                range.startInstant(),
                range.endInstant(),
                cleanSearch,
                pageable
        );

        PageResponseDto<SalesReportItemDto> items = PageResponseDto.fromPage(ordersPage, order -> {
            int totalUnits = 0;
            if (order.getItems() != null) {
                for (SalesOrderItem item : order.getItems()) {
                    if (item.getQuantity() != null) {
                        totalUnits += item.getQuantity();
                    }
                }
            }
            return new SalesReportItemDto(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getCompletedAt(),
                    order.getCustomerName(),
                    order.getCustomerContact(),
                    totalUnits,
                    order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO,
                    order.getStatus().name()
            );
        });

        return new SalesReportDto(range.startDate(), range.endDate(), summary, topSellingProducts, items);
    }

    @Override
    public PurchaseReportDto getPurchaseReport(
            LocalDate startDate,
            LocalDate endDate,
            Integer days,
            PurchaseOrderStatus status,
            Long supplierId,
            String search,
            Pageable pageable
    ) {
        DateRange range = resolveDateRange(startDate, endDate, days);

        List<Object[]> statusCounts = purchaseOrderRepository.countOrdersByStatusInRange(range.startInstant(), range.endInstant());
        long totalOrders = 0;
        long receivedCount = 0;
        long orderedCount = 0;
        long draftCount = 0;
        long cancelledCount = 0;

        if (statusCounts != null) {
            for (Object[] row : statusCounts) {
                PurchaseOrderStatus rowStatus = (PurchaseOrderStatus) row[0];
                long count = ((Number) row[1]).longValue();
                totalOrders += count;
                if (rowStatus == PurchaseOrderStatus.RECEIVED) {
                    receivedCount = count;
                } else if (rowStatus == PurchaseOrderStatus.ORDERED) {
                    orderedCount = count;
                } else if (rowStatus == PurchaseOrderStatus.DRAFT) {
                    draftCount = count;
                } else if (rowStatus == PurchaseOrderStatus.CANCELLED) {
                    cancelledCount = count;
                }
            }
        }

        Long receivedUnitsLong = purchaseOrderItemRepository.sumReceivedPurchaseUnits(range.startInstant(), range.endInstant());
        long receivedUnits = receivedUnitsLong != null ? receivedUnitsLong : 0L;

        BigDecimal receivedTotalCost = purchaseOrderItemRepository.sumReceivedPurchaseCost(range.startInstant(), range.endInstant());
        if (receivedTotalCost == null) {
            receivedTotalCost = BigDecimal.ZERO;
        }

        PurchaseReportSummaryDto summary = new PurchaseReportSummaryDto(
                totalOrders,
                receivedCount,
                orderedCount,
                draftCount,
                cancelledCount,
                receivedUnits,
                receivedTotalCost
        );

        // Supplier Summaries
        List<Object[]> supplierRows = purchaseOrderRepository.getSupplierPurchaseSummaries(range.startInstant(), range.endInstant());
        List<SupplierPurchaseSummaryDto> supplierSummaries = new ArrayList<>();
        if (supplierRows != null) {
            for (Object[] row : supplierRows) {
                Long supId = ((Number) row[0]).longValue();
                String supName = (String) row[1];
                long totalSupOrders = ((Number) row[2]).longValue();
                long recSupOrders = ((Number) row[3]).longValue();
                BigDecimal totalSpent = (BigDecimal) row[4];
                supplierSummaries.add(new SupplierPurchaseSummaryDto(
                        supId,
                        supName,
                        totalSupOrders,
                        recSupOrders,
                        totalSpent != null ? totalSpent : BigDecimal.ZERO
                ));
            }
        }

        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<PurchaseOrder> ordersPage = purchaseOrderRepository.searchPurchaseOrdersReport(
                range.startInstant(),
                range.endInstant(),
                status,
                supplierId,
                cleanSearch,
                pageable
        );

        PageResponseDto<PurchaseReportItemDto> items = PageResponseDto.fromPage(ordersPage, order -> {
            int totalUnits = 0;
            if (order.getItems() != null) {
                for (PurchaseOrderItem item : order.getItems()) {
                    if (item.getQuantity() != null) {
                        totalUnits += item.getQuantity();
                    }
                }
            }
            return new PurchaseReportItemDto(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getSupplier() != null ? order.getSupplier().getName() : null,
                    order.getStatus().name(),
                    order.getOrderedAt(),
                    order.getReceivedAt(),
                    order.getExpectedDeliveryDate(),
                    totalUnits,
                    order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO
            );
        });

        return new PurchaseReportDto(range.startDate(), range.endDate(), summary, supplierSummaries, items);
    }

    @Override
    public InventoryMovementReportDto getInventoryMovementReport(
            Long productId,
            InventoryTransactionType transactionType,
            LocalDate startDate,
            LocalDate endDate,
            Integer days,
            Pageable pageable
    ) {
        DateRange range = resolveDateRange(startDate, endDate, days);

        long totalMovements = inventoryTransactionRepository.countMovementsInRange(
                range.startInstant(),
                range.endInstant(),
                productId,
                transactionType
        );

        Long inboundUnitsLong = inventoryTransactionRepository.sumInboundUnitsInRange(
                range.startInstant(),
                range.endInstant(),
                productId,
                transactionType
        );
        long totalInboundUnits = inboundUnitsLong != null ? inboundUnitsLong : 0L;

        Long outboundUnitsLong = inventoryTransactionRepository.sumOutboundUnitsInRange(
                range.startInstant(),
                range.endInstant(),
                productId,
                transactionType
        );
        long totalOutboundUnits = outboundUnitsLong != null ? outboundUnitsLong : 0L;

        long netQuantityChange = totalInboundUnits - totalOutboundUnits;

        InventoryMovementSummaryDto summary = new InventoryMovementSummaryDto(
                totalMovements,
                totalInboundUnits,
                totalOutboundUnits,
                netQuantityChange
        );

        Page<InventoryTransaction> page = inventoryTransactionRepository.findMovementsReport(
                range.startInstant(),
                range.endInstant(),
                productId,
                transactionType,
                pageable
        );

        PageResponseDto<InventoryMovementItemDto> items = PageResponseDto.fromPage(page, tx -> {
            String performedBy = null;
            if (tx.getPerformedBy() != null) {
                if (tx.getPerformedBy().getFullName() != null && !tx.getPerformedBy().getFullName().isBlank()) {
                    performedBy = tx.getPerformedBy().getFullName();
                } else {
                    performedBy = tx.getPerformedBy().getUsername();
                }
            }

            return new InventoryMovementItemDto(
                    tx.getId(),
                    tx.getProduct() != null ? tx.getProduct().getId() : null,
                    tx.getProduct() != null ? tx.getProduct().getName() : null,
                    tx.getProduct() != null ? tx.getProduct().getSku() : null,
                    tx.getTransactionType() != null ? tx.getTransactionType().name() : null,
                    tx.getQuantityDelta(),
                    tx.getQuantityBefore(),
                    tx.getQuantityAfter(),
                    tx.getReferenceType(),
                    tx.getReferenceId(),
                    performedBy,
                    tx.getNotes(),
                    tx.getCreatedAt()
            );
        });

        return new InventoryMovementReportDto(range.startDate(), range.endDate(), summary, items);
    }

    @Override
    public StockValuationReportDto getStockValuationReport(Long categoryId, Pageable pageable) {
        long totalProducts = productRepository.countTotalProducts();
        long totalStockUnits = productRepository.sumTotalStockUnits();
        BigDecimal totalPotentialRetailValue = productRepository.sumTotalRetailValue();
        if (totalPotentialRetailValue == null) {
            totalPotentialRetailValue = BigDecimal.ZERO;
        }

        StockValuationSummaryDto summary = new StockValuationSummaryDto(
                totalProducts,
                totalStockUnits,
                totalPotentialRetailValue
        );

        String explanation = "Reliable per-unit purchase cost data is not currently maintained in the product catalog. Actual purchase costs are tracked at the purchase order receipt level, but historical weighted average cost / FIFO tracking has not been enabled for catalog products.";

        Page<Product> page;
        if (categoryId != null) {
            page = productRepository.searchInventoryReport(categoryId, "ALL", null, pageable);
        } else {
            page = productRepository.findAll(pageable);
        }

        PageResponseDto<StockValuationReportItemDto> items = PageResponseDto.fromPage(page, p -> {
            int qty = p.getQuantity() != null ? p.getQuantity() : 0;
            BigDecimal price = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;
            BigDecimal potentialRetailValue = price.multiply(BigDecimal.valueOf(qty));

            return new StockValuationReportItemDto(
                    p.getId(),
                    p.getName(),
                    p.getSku(),
                    p.getCategory() != null ? p.getCategory().getName() : null,
                    qty,
                    price,
                    potentialRetailValue,
                    null, // purchaseCost unavailable
                    null  // costValuation unavailable
            );
        });

        return new StockValuationReportDto(
                false,
                explanation,
                summary,
                items
        );
    }

    private DateRange resolveDateRange(LocalDate startDate, LocalDate endDate, Integer days) {
        LocalDate end = endDate != null ? endDate : LocalDate.now(ZoneOffset.UTC);
        LocalDate start;

        if (startDate != null) {
            if (startDate.isAfter(end)) {
                throw new IllegalArgumentException("Start date cannot be after end date");
            }
            start = startDate;
        } else {
            int daysBack = (days != null && days > 0) ? days : 30;
            start = end.minusDays(daysBack);
        }

        Instant startInstant = start.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endInstant = end.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();

        return new DateRange(start, end, startInstant, endInstant);
    }

    private record DateRange(
            LocalDate startDate,
            LocalDate endDate,
            Instant startInstant,
            Instant endInstant
    ) {}
}
