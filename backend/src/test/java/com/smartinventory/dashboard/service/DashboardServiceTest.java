package com.smartinventory.dashboard.service;

import com.smartinventory.dashboard.dto.DashboardSummaryDto;
import com.smartinventory.dashboard.service.impl.DashboardServiceImpl;
import com.smartinventory.inventory.entity.InventoryTransaction;
import com.smartinventory.inventory.model.InventoryTransactionType;
import com.smartinventory.inventory.repository.InventoryTransactionRepository;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.purchase.entity.PurchaseOrder;
import com.smartinventory.purchase.model.PurchaseOrderStatus;
import com.smartinventory.purchase.repository.PurchaseOrderRepository;
import com.smartinventory.sale.entity.SalesOrder;
import com.smartinventory.sale.model.SalesOrderStatus;
import com.smartinventory.sale.repository.SalesOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(
                productRepository,
                salesOrderRepository,
                purchaseOrderRepository,
                inventoryTransactionRepository
        );
    }

    @Test
    @DisplayName("getDashboardSummary returns full metrics for ADMIN role")
    void testGetDashboardSummary_Admin() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        when(productRepository.countTotalProducts()).thenReturn(15L);
        when(productRepository.sumTotalStockUnits()).thenReturn(250L);
        when(productRepository.countLowStock()).thenReturn(3L);
        when(productRepository.countOutOfStock()).thenReturn(1L);
        when(productRepository.countReorderRequired()).thenReturn(4L);
        when(productRepository.sumTotalRetailValue()).thenReturn(new BigDecimal("12500.00"));

        when(salesOrderRepository.countByStatus(SalesOrderStatus.COMPLETED)).thenReturn(8L);
        when(salesOrderRepository.countByStatus(SalesOrderStatus.DRAFT)).thenReturn(2L);
        when(salesOrderRepository.countByStatus(SalesOrderStatus.CANCELLED)).thenReturn(1L);
        when(salesOrderRepository.sumTotalAmountByStatus(SalesOrderStatus.COMPLETED)).thenReturn(new BigDecimal("4200.00"));

        when(purchaseOrderRepository.count()).thenReturn(6L);
        when(purchaseOrderRepository.countByStatus(PurchaseOrderStatus.RECEIVED)).thenReturn(4L);
        when(purchaseOrderRepository.countByStatus(PurchaseOrderStatus.ORDERED)).thenReturn(1L);
        when(purchaseOrderRepository.sumTotalAmountByStatus(PurchaseOrderStatus.RECEIVED)).thenReturn(new BigDecimal("3100.00"));

        Product pLow = new Product();
        pLow.setId(10L);
        pLow.setName("Low Item");
        pLow.setSku("SKU-LOW");
        pLow.setQuantity(2);
        pLow.setReorderLevel(10);
        when(productRepository.findReorderRequiredProducts()).thenReturn(List.of(pLow));

        SalesOrder so = new SalesOrder();
        so.setId(101L);
        so.setOrderNumber("SO-101");
        so.setCustomerName("Acme Client");
        so.setStatus(SalesOrderStatus.COMPLETED);
        so.setTotalAmount(new BigDecimal("500.00"));
        so.setCreatedAt(Instant.now());
        when(salesOrderRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of(so));

        PurchaseOrder po = new PurchaseOrder();
        po.setId(201L);
        po.setOrderNumber("PO-201");
        po.setStatus(PurchaseOrderStatus.RECEIVED);
        po.setTotalAmount(new BigDecimal("1200.00"));
        po.setCreatedAt(Instant.now());
        when(purchaseOrderRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of(po));

        InventoryTransaction tx = new InventoryTransaction();
        tx.setId(301L);
        tx.setProduct(pLow);
        tx.setTransactionType(InventoryTransactionType.SALE_DEDUCTION);
        tx.setQuantityDelta(-5);
        tx.setNotes("Sales deduction");
        tx.setCreatedAt(Instant.now());
        when(inventoryTransactionRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of(tx));

        DashboardSummaryDto result = dashboardService.getDashboardSummary(auth);

        assertNotNull(result);
        assertEquals(15L, result.totalProducts());
        assertEquals(250L, result.totalStockUnits());
        assertEquals(3L, result.lowStockCount());
        assertEquals(1L, result.outOfStockCount());
        assertEquals(4L, result.reorderRequiredCount());
        assertEquals(new BigDecimal("12500.00"), result.potentialRetailValuation());

        assertEquals(8L, result.completedSalesCount());
        assertEquals(new BigDecimal("4200.00"), result.completedSalesRevenue());

        assertTrue(result.purchaseMetricsVisible());
        assertEquals(6L, result.totalPurchasesCount());
        assertEquals(4L, result.receivedPurchasesCount());
        assertEquals(new BigDecimal("3100.00"), result.receivedPurchasesTotalCost());

        assertEquals(1, result.criticalAlerts().size());
        assertEquals("WARNING", result.criticalAlerts().get(0).severity());

        assertEquals(3, result.recentActivity().size());
    }

    @Test
    @DisplayName("getDashboardSummary masks purchase metrics for SALES_STAFF role")
    void testGetDashboardSummary_SalesStaff() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "staff", "pass", List.of(new SimpleGrantedAuthority("ROLE_SALES_STAFF"))
        );

        when(productRepository.countTotalProducts()).thenReturn(10L);
        when(productRepository.sumTotalStockUnits()).thenReturn(100L);
        when(productRepository.countLowStock()).thenReturn(0L);
        when(productRepository.countOutOfStock()).thenReturn(0L);
        when(productRepository.countReorderRequired()).thenReturn(0L);
        when(productRepository.sumTotalRetailValue()).thenReturn(new BigDecimal("5000.00"));

        when(salesOrderRepository.countByStatus(SalesOrderStatus.COMPLETED)).thenReturn(3L);
        when(salesOrderRepository.countByStatus(SalesOrderStatus.DRAFT)).thenReturn(1L);
        when(salesOrderRepository.countByStatus(SalesOrderStatus.CANCELLED)).thenReturn(0L);
        when(salesOrderRepository.sumTotalAmountByStatus(SalesOrderStatus.COMPLETED)).thenReturn(new BigDecimal("1500.00"));

        when(productRepository.findReorderRequiredProducts()).thenReturn(Collections.emptyList());
        when(salesOrderRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());
        when(inventoryTransactionRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

        DashboardSummaryDto result = dashboardService.getDashboardSummary(auth);

        assertNotNull(result);
        assertFalse(result.purchaseMetricsVisible());
        assertEquals(0L, result.totalPurchasesCount());
        assertEquals(0L, result.receivedPurchasesCount());
        assertEquals(BigDecimal.ZERO, result.receivedPurchasesTotalCost());

        verify(purchaseOrderRepository, never()).count();
        verify(purchaseOrderRepository, never()).findTop5ByOrderByCreatedAtDesc();
    }
}
