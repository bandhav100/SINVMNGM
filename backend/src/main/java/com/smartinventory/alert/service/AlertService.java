package com.smartinventory.alert.service;

import com.smartinventory.alert.dto.AlertSummaryDto;
import com.smartinventory.alert.dto.LowStockAlertDto;
import com.smartinventory.alert.model.AlertSeverity;
import com.smartinventory.alert.model.AlertType;
import com.smartinventory.common.dto.PageResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AlertService {

    AlertSummaryDto getAlertSummary();

    PageResponseDto<LowStockAlertDto> getAlerts(
            AlertType alertType,
            AlertSeverity severity,
            String search,
            Pageable pageable
    );

    List<LowStockAlertDto> getLowStockAlerts();
}
