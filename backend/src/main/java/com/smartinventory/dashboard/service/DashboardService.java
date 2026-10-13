package com.smartinventory.dashboard.service;

import com.smartinventory.dashboard.dto.DashboardSummaryDto;
import org.springframework.security.core.Authentication;

public interface DashboardService {

    DashboardSummaryDto getDashboardSummary(Authentication authentication);
}
