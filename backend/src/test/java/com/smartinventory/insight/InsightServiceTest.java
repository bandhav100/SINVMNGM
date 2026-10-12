package com.smartinventory.insight;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.insight.dto.*;
import com.smartinventory.insight.service.InsightService;
import com.smartinventory.insight.service.impl.InsightServiceImpl;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.purchase.repository.PurchaseOrderItemRepository;
import com.smartinventory.sale.repository.SalesOrderItemRepository;
import com.smartinventory.sale.repository.projection.FastMovingProductProjection;
import com.smartinventory.sale.repository.projection.ProductSalesPeriodProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SalesOrderItemRepository salesOrderItemRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    private InsightService insightService;

    @BeforeEach
    void setUp() {
        insightService = new InsightServiceImpl(
                productRepository,
                salesOrderItemRepository,
                purchaseOrderItemRepository
        );
    }

    @Test
    @DisplayName("Inventory Overview calculates metrics and reports cost valuation as unavailable")
    void getInventoryOverview_Success() {
        when(productRepository.countTotalProducts()).thenReturn(15L);
        when(productRepository.sumTotalStockUnits()).thenReturn(250L);
        when(productRepository.countOutOfStock()).thenReturn(2L);
        when(productRepository.countLowStock()).thenReturn(4L);
        when(productRepository.countReorderRequired()).thenReturn(6L);
        when(productRepository.sumTotalRetailValue()).thenReturn(new BigDecimal("5000.00"));

        InventoryOverviewDto overview = insightService.getInventoryOverview();

        assertThat(overview.totalProducts()).isEqualTo(15L);
        assertThat(overview.totalStockUnits()).isEqualTo(250L);
        assertThat(overview.outOfStockCount()).isEqualTo(2L);
        assertThat(overview.lowStockCount()).isEqualTo(4L);
        assertThat(overview.reorderRequiredCount()).isEqualTo(6L);
        assertThat(overview.costValuationAvailable()).isFalse();
        assertThat(overview.valuationExplanation()).contains("Reliable unit cost field is not maintained");
        assertThat(overview.totalPotentialRetailValue()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("Fast moving products maps projections and sorts by units sold")
    void getFastMovingProducts_Success() {
        FastMovingProductProjection proj = new FastMovingProductProjection() {
            @Override
            public Long getProductId() { return 1L; }
            @Override
            public String getProductName() { return "Widget A"; }
            @Override
            public String getProductSku() { return "SKU-A"; }
            @Override
            public String getCategoryName() { return "Electronics"; }
            @Override
            public Long getUnitsSold() { return 50L; }
            @Override
            public BigDecimal getTotalRevenue() { return new BigDecimal("1500.00"); }
        };

        when(salesOrderItemRepository.findFastMovingProducts(any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(proj));

        List<FastMovingProductDto> fastMoving = insightService.getFastMovingProducts(null, null, 30, 10);

        assertThat(fastMoving).hasSize(1);
        FastMovingProductDto item = fastMoving.get(0);
        assertThat(item.productId()).isEqualTo(1L);
        assertThat(item.productName()).isEqualTo("Widget A");
        assertThat(item.unitsSold()).isEqualTo(50L);
        assertThat(item.totalRevenue()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("Slow moving products distinguishes never sold from no recent sales")
    void getSlowMovingProducts_NeverSoldAndNoRecentSales() {
        ProductSalesPeriodProjection p1 = new ProductSalesPeriodProjection() {
            @Override public Long getProductId() { return 1L; }
            @Override public String getProductName() { return "Unsold Item"; }
            @Override public String getProductSku() { return "SKU-UNSOLD"; }
            @Override public Integer getCurrentStock() { return 100; }
            @Override public Integer getReorderLevel() { return 10; }
            @Override public String getCategoryName() { return "General"; }
            @Override public Long getUnitsSold() { return 0L; }
        };

        ProductSalesPeriodProjection p2 = new ProductSalesPeriodProjection() {
            @Override public Long getProductId() { return 2L; }
            @Override public String getProductName() { return "Dormant Item"; }
            @Override public String getProductSku() { return "SKU-DORMANT"; }
            @Override public Integer getCurrentStock() { return 20; }
            @Override public Integer getReorderLevel() { return 5; }
            @Override public String getCategoryName() { return "General"; }
            @Override public Long getUnitsSold() { return 0L; }
        };

        when(salesOrderItemRepository.findSlowMovingProducts(any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(p1, p2));

        when(salesOrderItemRepository.hasLifetimeCompletedSales(1L)).thenReturn(false);
        when(salesOrderItemRepository.hasLifetimeCompletedSales(2L)).thenReturn(true);
        Instant pastSold = Instant.parse("2026-08-01T10:00:00Z");
        when(salesOrderItemRepository.findLastSoldDateByProductId(2L)).thenReturn(pastSold);

        List<SlowMovingProductDto> slow = insightService.getSlowMovingProducts(null, null, 30, 10);

        assertThat(slow).hasSize(2);
        assertThat(slow.get(0).classification()).isEqualTo("NEVER_SOLD");
        assertThat(slow.get(0).lastSoldDate()).isNull();

        assertThat(slow.get(1).classification()).isEqualTo("NO_RECENT_SALES");
        assertThat(slow.get(1).lastSoldDate()).isEqualTo(pastSold);
    }

    @Test
    @DisplayName("Stock valuation calculates retail potential and reports cost valuation unavailable")
    void getStockValuation_CalculatesRetailValue() {
        Product product = new Product(1L, "Gadget", "SKU-G", "Desc", new BigDecimal("45.00"), 10, 5);

        when(productRepository.sumTotalStockUnits()).thenReturn(10L);
        when(productRepository.sumTotalRetailValue()).thenReturn(new BigDecimal("450.00"));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(product)));

        StockValuationDto valuation = insightService.getStockValuation(PageRequest.of(0, 10));

        assertThat(valuation.costValuationAvailable()).isFalse();
        assertThat(valuation.totalStockUnits()).isEqualTo(10L);
        assertThat(valuation.totalRetailValue()).isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(valuation.products().content()).hasSize(1);
        assertThat(valuation.products().content().get(0).totalRetailValue()).isEqualByComparingTo(new BigDecimal("450.00"));
    }

    @Test
    @DisplayName("Purchase vs Sales compares received purchase and completed sales units and spend")
    void getPurchaseVsSales_CalculatesUnitsAndFinancials() {
        when(purchaseOrderItemRepository.sumReceivedPurchaseUnits(any(Instant.class), any(Instant.class))).thenReturn(100L);
        when(purchaseOrderItemRepository.sumReceivedPurchaseCost(any(Instant.class), any(Instant.class))).thenReturn(new BigDecimal("2000.00"));
        when(salesOrderItemRepository.sumCompletedSalesUnits(any(Instant.class), any(Instant.class))).thenReturn(70L);
        when(salesOrderItemRepository.sumCompletedSalesRevenue(any(Instant.class), any(Instant.class))).thenReturn(new BigDecimal("2800.00"));

        PurchaseVsSalesDto comparison = insightService.getPurchaseVsSales(LocalDate.now().minusDays(30), LocalDate.now(), null);

        assertThat(comparison.purchasedUnits()).isEqualTo(100L);
        assertThat(comparison.purchaseCostTotal()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(comparison.soldUnits()).isEqualTo(70L);
        assertThat(comparison.salesRevenueTotal()).isEqualByComparingTo(new BigDecimal("2800.00"));
        assertThat(comparison.netUnitsDelta()).isEqualTo(30L); // 100 - 70 = 30 net units added
    }

    @Test
    @DisplayName("Purchase vs Sales throws IllegalArgumentException when start date is after end date")
    void getPurchaseVsSales_InvalidDateRange_ThrowsException() {
        LocalDate start = LocalDate.now();
        LocalDate end = start.minusDays(5);

        assertThatThrownBy(() -> insightService.getPurchaseVsSales(start, end, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date cannot be after end date");
    }

    @Test
    @DisplayName("Reorder recommendations applies policy safety stock calculation")
    void getReorderRecommendations_PolicyCalculations() {
        Product outOfStockProduct = new Product(1L, "Item Zero", "SKU-ZERO", "Desc", new BigDecimal("10.00"), 0, 10);
        Product lowStockProduct = new Product(2L, "Item Low", "SKU-LOW", "Desc", new BigDecimal("15.00"), 4, 10);

        when(productRepository.findReorderRequiredProducts()).thenReturn(List.of(outOfStockProduct, lowStockProduct));

        List<ReorderRecommendationDto> recommendations = insightService.getReorderRecommendations();

        assertThat(recommendations).hasSize(2);

        // Out of stock product: target = 10 * 2 = 20, suggested = 20
        ReorderRecommendationDto rec1 = recommendations.get(0);
        assertThat(rec1.productId()).isEqualTo(1L);
        assertThat(rec1.currentStock()).isEqualTo(0);
        assertThat(rec1.suggestedReplenishmentQuantity()).isEqualTo(20);
        assertThat(rec1.recommendationReason()).contains("completely out of stock");

        // Low stock product: target = 10 * 2 = 20, suggested = 20 - 4 = 16
        ReorderRecommendationDto rec2 = recommendations.get(1);
        assertThat(rec2.productId()).isEqualTo(2L);
        assertThat(rec2.currentStock()).isEqualTo(4);
        assertThat(rec2.suggestedReplenishmentQuantity()).isEqualTo(16);
        assertThat(rec2.recommendationReason()).contains("at or below reorder threshold");
    }
}
