package com.smartinventory.dashboard.service.impl;

import com.smartinventory.dashboard.dto.DashboardActivityItemDto;
import com.smartinventory.dashboard.dto.DashboardAlertItemDto;
import com.smartinventory.dashboard.dto.DashboardSummaryDto;
import com.smartinventory.dashboard.service.DashboardService;
import com.smartinventory.inventory.entity.InventoryTransaction;
import com.smartinventory.inventory.repository.InventoryTransactionRepository;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.purchase.entity.PurchaseOrder;
import com.smartinventory.purchase.model.PurchaseOrderStatus;
import com.smartinventory.purchase.repository.PurchaseOrderRepository;
import com.smartinventory.sale.entity.SalesOrder;
import com.smartinventory.sale.model.SalesOrderStatus;
import com.smartinventory.sale.repository.SalesOrderRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public DashboardServiceImpl(
            ProductRepository productRepository,
            SalesOrderRepository salesOrderRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            InventoryTransactionRepository inventoryTransactionRepository
    ) {
        this.productRepository = productRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @Override
    public DashboardSummaryDto getDashboardSummary(Authentication authentication) {
        boolean isSalesStaffOnly = false;
        if (authentication != null && authentication.getAuthorities() != null) {
            boolean hasStaff = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_SALES_STAFF".equals(a.getAuthority()));
            boolean hasHigher = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                            || "ROLE_INVENTORY_MANAGER".equals(a.getAuthority())
                            || "ROLE_PURCHASE_MANAGER".equals(a.getAuthority()));
            if (hasStaff && !hasHigher) {
                isSalesStaffOnly = true;
            }
        }

        // 1. Inventory KPIs
        long totalProducts = productRepository.countTotalProducts();
        long totalStockUnits = productRepository.sumTotalStockUnits();
        long lowStockCount = productRepository.countLowStock();
        long outOfStockCount = productRepository.countOutOfStock();
        long reorderRequiredCount = productRepository.countReorderRequired();
        BigDecimal potentialRetailValuation = productRepository.sumTotalRetailValue();
        if (potentialRetailValuation == null) {
            potentialRetailValuation = BigDecimal.ZERO;
        }

        // 2. Sales KPIs
        long completedSalesCount = salesOrderRepository.countByStatus(SalesOrderStatus.COMPLETED);
        long draftSalesCount = salesOrderRepository.countByStatus(SalesOrderStatus.DRAFT);
        long cancelledSalesCount = salesOrderRepository.countByStatus(SalesOrderStatus.CANCELLED);
        BigDecimal completedSalesRevenue = salesOrderRepository.sumTotalAmountByStatus(SalesOrderStatus.COMPLETED);
        if (completedSalesRevenue == null) {
            completedSalesRevenue = BigDecimal.ZERO;
        }

        // 3. Purchase KPIs (omitted for pure SALES_STAFF)
        boolean purchaseMetricsVisible = !isSalesStaffOnly;
        long totalPurchasesCount = 0;
        long receivedPurchasesCount = 0;
        long pendingPurchasesCount = 0;
        BigDecimal receivedPurchasesTotalCost = BigDecimal.ZERO;

        if (purchaseMetricsVisible) {
            totalPurchasesCount = purchaseOrderRepository.count();
            receivedPurchasesCount = purchaseOrderRepository.countByStatus(PurchaseOrderStatus.RECEIVED);
            pendingPurchasesCount = purchaseOrderRepository.countByStatus(PurchaseOrderStatus.ORDERED);
            receivedPurchasesTotalCost = purchaseOrderRepository.sumTotalAmountByStatus(PurchaseOrderStatus.RECEIVED);
            if (receivedPurchasesTotalCost == null) {
                receivedPurchasesTotalCost = BigDecimal.ZERO;
            }
        }

        // 4. Critical Alerts (top 5 low/out of stock items)
        List<Product> reorderItems = productRepository.findReorderRequiredProducts();
        List<DashboardAlertItemDto> criticalAlerts = new ArrayList<>();
        if (reorderItems != null) {
            for (int i = 0; i < Math.min(reorderItems.size(), 5); i++) {
                Product p = reorderItems.get(i);
                String severity = p.getQuantity() <= 0 ? "CRITICAL" : "WARNING";
                criticalAlerts.add(new DashboardAlertItemDto(
                        p.getId(),
                        p.getName(),
                        p.getSku(),
                        p.getQuantity(),
                        p.getReorderLevel(),
                        severity
                ));
            }
        }

        // 5. Recent Activity Feed
        List<DashboardActivityItemDto> activityFeed = new ArrayList<>();

        // Sales activity
        List<SalesOrder> recentSales = salesOrderRepository.findTop5ByOrderByCreatedAtDesc();
        if (recentSales != null) {
            for (SalesOrder so : recentSales) {
                Instant ts = so.getUpdatedAt() != null ? so.getUpdatedAt() : so.getCreatedAt();
                String customerInfo = (so.getCustomerName() != null && !so.getCustomerName().isBlank())
                        ? "Customer: " + so.getCustomerName()
                        : "Walk-in Customer";
                activityFeed.add(new DashboardActivityItemDto(
                        "SO-" + so.getId(),
                        "SALE",
                        "Sales Order " + so.getOrderNumber(),
                        customerInfo,
                        so.getStatus().name(),
                        so.getTotalAmount(),
                        null,
                        ts,
                        "/sales"
                ));
            }
        }

        // Purchase activity (only if permitted)
        if (purchaseMetricsVisible) {
            List<PurchaseOrder> recentPurchases = purchaseOrderRepository.findTop5ByOrderByCreatedAtDesc();
            if (recentPurchases != null) {
                for (PurchaseOrder po : recentPurchases) {
                    Instant ts = po.getUpdatedAt() != null ? po.getUpdatedAt() : po.getCreatedAt();
                    String suppName = po.getSupplier() != null ? po.getSupplier().getName() : "Unknown Supplier";
                    activityFeed.add(new DashboardActivityItemDto(
                            "PO-" + po.getId(),
                            "PURCHASE",
                            "Purchase Order " + po.getOrderNumber(),
                            "Supplier: " + suppName,
                            po.getStatus().name(),
                            po.getTotalAmount(),
                            null,
                            ts,
                            "/purchases"
                    ));
                }
            }
        }

        // Inventory movements activity
        List<InventoryTransaction> recentMovements = inventoryTransactionRepository.findTop5ByOrderByCreatedAtDesc();
        if (recentMovements != null) {
            for (InventoryTransaction tx : recentMovements) {
                String deltaSign = tx.getQuantityDelta() > 0 ? "+" : "";
                String prodName = tx.getProduct() != null ? tx.getProduct().getName() : "Product #" + tx.getId();
                String note = (tx.getNotes() != null && !tx.getNotes().isBlank()) ? tx.getNotes() : tx.getTransactionType().name();
                activityFeed.add(new DashboardActivityItemDto(
                        "TX-" + tx.getId(),
                        "INVENTORY",
                        prodName + " (" + tx.getTransactionType() + ")",
                        deltaSign + tx.getQuantityDelta() + " units • " + note,
                        tx.getTransactionType().name(),
                        null,
                        tx.getQuantityDelta(),
                        tx.getCreatedAt(),
                        "/inventory"
                ));
            }
        }

        // Sort combined feed by timestamp descending and take top 10
        activityFeed.sort(Comparator.comparing(DashboardActivityItemDto::timestamp, Comparator.nullsLast(Comparator.reverseOrder())));
        if (activityFeed.size() > 10) {
            activityFeed = activityFeed.subList(0, 10);
        }

        return new DashboardSummaryDto(
                totalProducts,
                totalStockUnits,
                lowStockCount,
                outOfStockCount,
                reorderRequiredCount,
                potentialRetailValuation,
                completedSalesCount,
                draftSalesCount,
                cancelledSalesCount,
                completedSalesRevenue,
                purchaseMetricsVisible,
                totalPurchasesCount,
                receivedPurchasesCount,
                pendingPurchasesCount,
                receivedPurchasesTotalCost,
                criticalAlerts,
                activityFeed
        );
    }
}
