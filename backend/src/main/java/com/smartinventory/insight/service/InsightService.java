package com.smartinventory.insight.service;

import com.smartinventory.insight.dto.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface InsightService {

    InventoryOverviewDto getInventoryOverview();

    List<FastMovingProductDto> getFastMovingProducts(LocalDate startDate, LocalDate endDate, Integer days, int limit);

    List<SlowMovingProductDto> getSlowMovingProducts(LocalDate startDate, LocalDate endDate, Integer days, int limit);

    StockValuationDto getStockValuation(Pageable pageable);

    PurchaseVsSalesDto getPurchaseVsSales(LocalDate startDate, LocalDate endDate, Integer days);

    List<ReorderRecommendationDto> getReorderRecommendations();
}
