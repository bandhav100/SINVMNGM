package com.smartinventory.report.service;

import com.smartinventory.category.entity.Category;
import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.inventory.entity.InventoryTransaction;
import com.smartinventory.inventory.model.InventoryTransactionType;
import com.smartinventory.inventory.repository.InventoryTransactionRepository;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.purchase.entity.PurchaseOrder;
import com.smartinventory.purchase.model.PurchaseOrderStatus;
import com.smartinventory.purchase.repository.PurchaseOrderItemRepository;
import com.smartinventory.purchase.repository.PurchaseOrderRepository;
import com.smartinventory.report.dto.*;
import com.smartinventory.report.service.impl.ReportServiceImpl;
import com.smartinventory.sale.entity.SalesOrder;
import com.smartinventory.sale.repository.SalesOrderItemRepository;
import com.smartinventory.sale.repository.SalesOrderRepository;
import com.smartinventory.supplier.entity.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private SalesOrderItemRepository salesOrderItemRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private Product product1;
    private Product product2;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(10L);
        category.setName("Electronics");

        product1 = new Product();
        product1.setId(1L);
        product1.setName("Product 1");
        product1.setSku("SKU-1");
        product1.setQuantity(20);
        product1.setReorderLevel(10);
        product1.setPrice(new BigDecimal("100.00"));
        product1.setCategory(category);

        product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setSku("SKU-2");
        product2.setQuantity(0);
        product2.setReorderLevel(5);
        product2.setPrice(new BigDecimal("50.00"));
        product2.setCategory(category);
    }

    @Test
    @DisplayName("Inventory report computes correct summary counts and classifies stock status")
    void testInventoryReport() {
        when(productRepository.countTotalProducts()).thenReturn(2L);
        when(productRepository.sumTotalStockUnits()).thenReturn(20L);
        when(productRepository.countOutOfStock()).thenReturn(1L);
        when(productRepository.countLowStock()).thenReturn(0L);

        List<Product> products = List.of(product1, product2);
        when(productRepository.searchInventoryReport(isNull(), eq("ALL"), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(products, PageRequest.of(0, 20), 2));

        InventoryReportDto report = reportService.getInventoryReport(null, "ALL", null, PageRequest.of(0, 20));

        assertThat(report.summary().totalProducts()).isEqualTo(2);
        assertThat(report.summary().totalStockUnits()).isEqualTo(20);
        assertThat(report.summary().outOfStockCount()).isEqualTo(1);
        assertThat(report.summary().inStockCount()).isEqualTo(1);
        assertThat(report.items().content()).hasSize(2);
        assertThat(report.items().content().get(0).stockStatus()).isEqualTo("IN_STOCK");
        assertThat(report.items().content().get(1).stockStatus()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    @DisplayName("Sales report aggregates completed orders and calculates average order value correctly")
    void testSalesReport() {
        when(salesOrderRepository.countCompletedOrdersInRange(any(Instant.class), any(Instant.class))).thenReturn(2L);
        when(salesOrderItemRepository.sumCompletedSalesUnits(any(Instant.class), any(Instant.class))).thenReturn(15L);
        when(salesOrderItemRepository.sumCompletedSalesRevenue(any(Instant.class), any(Instant.class))).thenReturn(new BigDecimal("300.00"));
        when(salesOrderItemRepository.findFastMovingProducts(any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(salesOrderRepository.searchCompletedOrdersReport(any(Instant.class), any(Instant.class), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0));

        SalesReportDto report = reportService.getSalesReport(null, null, 30, null, PageRequest.of(0, 20));

        assertThat(report.summary().completedOrdersCount()).isEqualTo(2);
        assertThat(report.summary().totalUnitsSold()).isEqualTo(15);
        assertThat(report.summary().totalRevenue()).isEqualByComparingTo("300.00");
        assertThat(report.summary().averageOrderValue()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Sales report rejects startDate after endDate with IllegalArgumentException")
    void testSalesReportInvalidDateRange() {
        LocalDate start = LocalDate.of(2026, 12, 31);
        LocalDate end = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> reportService.getSalesReport(start, end, null, null, PageRequest.of(0, 20)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date cannot be after end date");
    }

    @Test
    @DisplayName("Purchase report aggregates status counts and supplier summaries correctly")
    void testPurchaseReport() {
        List<Object[]> statusCounts = new ArrayList<>();
        statusCounts.add(new Object[]{PurchaseOrderStatus.RECEIVED, 3L});
        statusCounts.add(new Object[]{PurchaseOrderStatus.ORDERED, 1L});
        when(purchaseOrderRepository.countOrdersByStatusInRange(any(Instant.class), any(Instant.class))).thenReturn(statusCounts);

        when(purchaseOrderItemRepository.sumReceivedPurchaseUnits(any(Instant.class), any(Instant.class))).thenReturn(50L);
        when(purchaseOrderItemRepository.sumReceivedPurchaseCost(any(Instant.class), any(Instant.class))).thenReturn(new BigDecimal("2500.00"));

        List<Object[]> supplierRows = new ArrayList<>();
        supplierRows.add(new Object[]{100L, "Acme Corp", 4L, 3L, new BigDecimal("2500.00")});
        when(purchaseOrderRepository.getSupplierPurchaseSummaries(any(Instant.class), any(Instant.class))).thenReturn(supplierRows);

        when(purchaseOrderRepository.searchPurchaseOrdersReport(any(Instant.class), any(Instant.class), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0));

        PurchaseReportDto report = reportService.getPurchaseReport(null, null, 30, null, null, null, PageRequest.of(0, 20));

        assertThat(report.summary().totalOrdersCount()).isEqualTo(4);
        assertThat(report.summary().receivedOrdersCount()).isEqualTo(3);
        assertThat(report.summary().orderedPendingCount()).isEqualTo(1);
        assertThat(report.summary().receivedUnits()).isEqualTo(50);
        assertThat(report.summary().receivedTotalCost()).isEqualByComparingTo("2500.00");
        assertThat(report.supplierSummaries()).hasSize(1);
        assertThat(report.supplierSummaries().get(0).supplierName()).isEqualTo("Acme Corp");
    }

    @Test
    @DisplayName("Inventory movement report calculates net delta accurately from transaction ledger")
    void testInventoryMovementReport() {
        when(inventoryTransactionRepository.countMovementsInRange(any(Instant.class), any(Instant.class), isNull(), isNull()))
                .thenReturn(5L);
        when(inventoryTransactionRepository.sumInboundUnitsInRange(any(Instant.class), any(Instant.class), isNull(), isNull()))
                .thenReturn(100L);
        when(inventoryTransactionRepository.sumOutboundUnitsInRange(any(Instant.class), any(Instant.class), isNull(), isNull()))
                .thenReturn(40L);

        when(inventoryTransactionRepository.findMovementsReport(any(Instant.class), any(Instant.class), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0));

        InventoryMovementReportDto report = reportService.getInventoryMovementReport(null, null, null, null, 30, PageRequest.of(0, 20));

        assertThat(report.summary().totalMovements()).isEqualTo(5);
        assertThat(report.summary().totalInboundUnits()).isEqualTo(100);
        assertThat(report.summary().totalOutboundUnits()).isEqualTo(40);
        assertThat(report.summary().netQuantityChange()).isEqualTo(60);
    }

    @Test
    @DisplayName("Stock valuation report returns costValuationAvailable false with clear explanation")
    void testStockValuationReport() {
        when(productRepository.countTotalProducts()).thenReturn(2L);
        when(productRepository.sumTotalStockUnits()).thenReturn(20L);
        when(productRepository.sumTotalRetailValue()).thenReturn(new BigDecimal("2000.00"));
        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product1, product2), PageRequest.of(0, 20), 2));

        StockValuationReportDto report = reportService.getStockValuationReport(null, PageRequest.of(0, 20));

        assertThat(report.costValuationAvailable()).isFalse();
        assertThat(report.explanation()).contains("Reliable per-unit purchase cost data is not currently maintained");
        assertThat(report.summary().totalActiveProducts()).isEqualTo(2);
        assertThat(report.summary().totalStockUnits()).isEqualTo(20);
        assertThat(report.summary().totalPotentialRetailValue()).isEqualByComparingTo("2000.00");
        assertThat(report.items().content().get(0).purchaseCost()).isNull();
        assertThat(report.items().content().get(0).costValuation()).isNull();
        assertThat(report.items().content().get(0).potentialRetailValue()).isEqualByComparingTo("2000.00");
    }
}
