package com.smartinventory.insight.service.impl;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.insight.dto.*;
import com.smartinventory.insight.service.InsightService;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.purchase.repository.PurchaseOrderItemRepository;
import com.smartinventory.sale.repository.SalesOrderItemRepository;
import com.smartinventory.sale.repository.projection.FastMovingProductProjection;
import com.smartinventory.sale.repository.projection.ProductSalesPeriodProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InsightServiceImpl implements InsightService {

    private final ProductRepository productRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    public InsightServiceImpl(
            ProductRepository productRepository,
            SalesOrderItemRepository salesOrderItemRepository,
            PurchaseOrderItemRepository purchaseOrderItemRepository
    ) {
        this.productRepository = productRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
    }

    @Override
    public InventoryOverviewDto getInventoryOverview() {
        long totalProducts = productRepository.countTotalProducts();
        long totalStockUnits = productRepository.sumTotalStockUnits();
        long outOfStock = productRepository.countOutOfStock();
        long lowStock = productRepository.countLowStock();
        long reorderRequired = productRepository.countReorderRequired();
        BigDecimal totalRetailValue = productRepository.sumTotalRetailValue();

        String valuationExplanation = "Reliable unit cost field is not maintained in product catalog; valuation based on purchase cost is unavailable.";

        return new InventoryOverviewDto(
                totalProducts,
                totalStockUnits,
                outOfStock,
                lowStock,
                reorderRequired,
                false,
                valuationExplanation,
                totalRetailValue
        );
    }

    @Override
    public List<FastMovingProductDto> getFastMovingProducts(LocalDate startDate, LocalDate endDate, Integer days, int limit) {
        DateRange range = resolveDateRange(startDate, endDate, days);
        int safeLimit = Math.min(Math.max(1, limit), 100);

        List<FastMovingProductProjection> projections = salesOrderItemRepository.findFastMovingProducts(
                range.startInstant(),
                range.endInstant(),
                PageRequest.of(0, safeLimit)
        );

        List<FastMovingProductDto> result = new ArrayList<>();
        for (FastMovingProductProjection p : projections) {
            result.add(new FastMovingProductDto(
                    p.getProductId(),
                    p.getProductName(),
                    p.getProductSku(),
                    p.getCategoryName(),
                    p.getUnitsSold() != null ? p.getUnitsSold() : 0L,
                    p.getTotalRevenue() != null ? p.getTotalRevenue() : BigDecimal.ZERO,
                    range.startDate(),
                    range.endDate()
            ));
        }
        return result;
    }

    @Override
    public List<SlowMovingProductDto> getSlowMovingProducts(LocalDate startDate, LocalDate endDate, Integer days, int limit) {
        DateRange range = resolveDateRange(startDate, endDate, days);
        int safeLimit = Math.min(Math.max(1, limit), 100);

        List<ProductSalesPeriodProjection> projections = salesOrderItemRepository.findSlowMovingProducts(
                range.startInstant(),
                range.endInstant(),
                PageRequest.of(0, safeLimit)
        );

        List<SlowMovingProductDto> result = new ArrayList<>();
        for (ProductSalesPeriodProjection p : projections) {
            long unitsSold = p.getUnitsSold() != null ? p.getUnitsSold() : 0L;
            String classification;
            Instant lastSoldDate = null;

            if (unitsSold == 0) {
                boolean hasLifetimeSales = salesOrderItemRepository.hasLifetimeCompletedSales(p.getProductId());
                if (!hasLifetimeSales) {
                    classification = "NEVER_SOLD";
                } else {
                    classification = "NO_RECENT_SALES";
                    lastSoldDate = salesOrderItemRepository.findLastSoldDateByProductId(p.getProductId());
                }
            } else {
                classification = "SLOW_MOVING";
                lastSoldDate = salesOrderItemRepository.findLastSoldDateByProductId(p.getProductId());
            }

            result.add(new SlowMovingProductDto(
                    p.getProductId(),
                    p.getProductName(),
                    p.getProductSku(),
                    p.getCategoryName(),
                    p.getCurrentStock(),
                    p.getReorderLevel(),
                    unitsSold,
                    classification,
                    lastSoldDate,
                    range.startDate(),
                    range.endDate()
            ));
        }
        return result;
    }

    @Override
    public StockValuationDto getStockValuation(Pageable pageable) {
        long totalStockUnits = productRepository.sumTotalStockUnits();
        BigDecimal totalRetailValue = productRepository.sumTotalRetailValue();
        Page<Product> page = productRepository.findAll(pageable);

        PageResponseDto<ProductValuationItemDto> pageDto = PageResponseDto.fromPage(page, p -> {
            BigDecimal unitPrice = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;
            BigDecimal retailValue = unitPrice.multiply(BigDecimal.valueOf(p.getQuantity() != null ? p.getQuantity() : 0));
            return new ProductValuationItemDto(
                    p.getId(),
                    p.getName(),
                    p.getSku(),
                    p.getCategory() != null ? p.getCategory().getName() : null,
                    p.getQuantity(),
                    unitPrice,
                    retailValue,
                    false
            );
        });

        String explanation = "Reliable unit cost field is not configured on products. Valuation based on purchase cost is unavailable; retail stock value is based on selling price.";

        return new StockValuationDto(
                false,
                explanation,
                "USD",
                totalStockUnits,
                totalRetailValue,
                pageDto
        );
    }

    @Override
    public PurchaseVsSalesDto getPurchaseVsSales(LocalDate startDate, LocalDate endDate, Integer days) {
        DateRange range = resolveDateRange(startDate, endDate, days);

        Long purchasedUnits = purchaseOrderItemRepository.sumReceivedPurchaseUnits(range.startInstant(), range.endInstant());
        BigDecimal purchaseCostTotal = purchaseOrderItemRepository.sumReceivedPurchaseCost(range.startInstant(), range.endInstant());

        Long soldUnits = salesOrderItemRepository.sumCompletedSalesUnits(range.startInstant(), range.endInstant());
        BigDecimal salesRevenueTotal = salesOrderItemRepository.sumCompletedSalesRevenue(range.startInstant(), range.endInstant());

        long pUnits = purchasedUnits != null ? purchasedUnits : 0L;
        long sUnits = soldUnits != null ? soldUnits : 0L;
        BigDecimal pCost = purchaseCostTotal != null ? purchaseCostTotal : BigDecimal.ZERO;
        BigDecimal sRevenue = salesRevenueTotal != null ? salesRevenueTotal : BigDecimal.ZERO;
        long netDelta = pUnits - sUnits;

        return new PurchaseVsSalesDto(
                range.startDate(),
                range.endDate(),
                pUnits,
                pCost,
                sUnits,
                sRevenue,
                netDelta
        );
    }

    @Override
    public List<ReorderRecommendationDto> getReorderRecommendations() {
        List<Product> products = productRepository.findReorderRequiredProducts();
        List<ReorderRecommendationDto> recommendations = new ArrayList<>();

        for (Product p : products) {
            int reorderLevel = p.getReorderLevel() != null ? p.getReorderLevel() : 10;
            int currentStock = p.getQuantity() != null ? p.getQuantity() : 0;
            int targetStock = Math.max(reorderLevel * 2, 1);
            int suggestedQuantity = Math.max(reorderLevel, targetStock - currentStock);

            String reason;
            if (currentStock <= 0) {
                reason = "Product is completely out of stock. Immediate replenishment of " + suggestedQuantity + " units recommended to restore safety buffer.";
            } else {
                reason = "Current stock (" + currentStock + ") is at or below reorder threshold (" + reorderLevel + "). Replenishment of " + suggestedQuantity + " units recommended.";
            }

            recommendations.add(new ReorderRecommendationDto(
                    p.getId(),
                    p.getName(),
                    p.getSku(),
                    p.getCategory() != null ? p.getCategory().getName() : null,
                    currentStock,
                    reorderLevel,
                    suggestedQuantity,
                    reason,
                    "POLICY_SAFETY_STOCK_MINIMUM"
            ));
        }
        return recommendations;
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
