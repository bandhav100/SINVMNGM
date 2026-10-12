package com.smartinventory.insight.controller;

import com.smartinventory.insight.dto.*;
import com.smartinventory.insight.service.InsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/insights")
@Tag(name = "Smart Insights", description = "Inventory intelligence, velocity analytics, stock valuation, and demand recommendations")
public class InsightController {

    private final InsightService insightService;

    public InsightController(InsightService insightService) {
        this.insightService = insightService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get inventory overview metrics", description = "Retrieves total active products, stock units, out-of-stock, low-stock, and potential retail valuation")
    public ResponseEntity<InventoryOverviewDto> getOverview() {
        return ResponseEntity.ok(insightService.getInventoryOverview());
    }

    @GetMapping("/fast-moving-products")
    @Operation(summary = "Get fast-moving products", description = "Ranks products by total quantities sold in completed sales orders over a selected time window")
    public ResponseEntity<List<FastMovingProductDto>> getFastMovingProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer days,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(insightService.getFastMovingProducts(startDate, endDate, days, limit));
    }

    @GetMapping("/slow-moving-products")
    @Operation(summary = "Get slow-moving and dead-stock products", description = "Identifies products with low or zero sales during the selected period, distinguishing never-sold items")
    public ResponseEntity<List<SlowMovingProductDto>> getSlowMovingProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer days,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(insightService.getSlowMovingProducts(startDate, endDate, days, limit));
    }

    @GetMapping("/stock-valuation")
    @Operation(summary = "Get inventory stock valuation", description = "Calculates inventory valuation reporting reliable cost availability status and retail stock value")
    public ResponseEntity<StockValuationDto> getStockValuation(
            @PageableDefault(size = 20, sort = "quantity", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(insightService.getStockValuation(pageable));
    }

    @GetMapping("/purchase-vs-sales")
    @Operation(summary = "Compare purchase receipt vs completed sales", description = "Compares received purchase quantities/spend against completed sales quantities/revenue over a date range")
    public ResponseEntity<PurchaseVsSalesDto> getPurchaseVsSales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer days
    ) {
        return ResponseEntity.ok(insightService.getPurchaseVsSales(startDate, endDate, days));
    }

    @GetMapping("/reorder-recommendations")
    @Operation(summary = "Get policy-based reorder recommendations", description = "Recommends suggested replenishment quantities for products at or below their configured safety reorder thresholds")
    public ResponseEntity<List<ReorderRecommendationDto>> getReorderRecommendations() {
        return ResponseEntity.ok(insightService.getReorderRecommendations());
    }
}
