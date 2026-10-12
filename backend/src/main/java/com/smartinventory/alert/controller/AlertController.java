package com.smartinventory.alert.controller;

import com.smartinventory.alert.dto.AlertSummaryDto;
import com.smartinventory.alert.dto.LowStockAlertDto;
import com.smartinventory.alert.model.AlertSeverity;
import com.smartinventory.alert.model.AlertType;
import com.smartinventory.alert.service.AlertService;
import com.smartinventory.common.dto.PageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts & Notifications", description = "Low-stock detection, out-of-stock emergency alerts, and inventory notification summary")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Operation(summary = "Get paginated alerts", description = "Retrieves active stock alerts filtered by alert type, severity, and search term")
    public ResponseEntity<PageResponseDto<LowStockAlertDto>> getAlerts(
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "quantity", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(alertService.getAlerts(type, severity, search, pageable));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get alerts summary count", description = "Returns active alert count breakdown by critical out-of-stock and warning low-stock categories")
    public ResponseEntity<AlertSummaryDto> getSummary() {
        return ResponseEntity.ok(alertService.getAlertSummary());
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get all low-stock alerts", description = "Returns list of products currently at or below safety reorder thresholds for fast notification display")
    public ResponseEntity<List<LowStockAlertDto>> getLowStockAlerts() {
        return ResponseEntity.ok(alertService.getLowStockAlerts());
    }
}
