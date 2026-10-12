package com.smartinventory.dashboard.controller;

import com.smartinventory.dashboard.dto.DashboardSummaryDto;
import com.smartinventory.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Endpoints for real-time business KPIs, stock radar alerts, and recent operational activity")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary", description = "Returns aggregated business metrics, stock counts, sales/purchase KPIs, and recent activity")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary(Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getDashboardSummary(authentication));
    }
}
