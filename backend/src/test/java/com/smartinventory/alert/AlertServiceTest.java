package com.smartinventory.alert;

import com.smartinventory.alert.dto.AlertSummaryDto;
import com.smartinventory.alert.dto.LowStockAlertDto;
import com.smartinventory.alert.model.AlertSeverity;
import com.smartinventory.alert.model.AlertType;
import com.smartinventory.alert.service.AlertService;
import com.smartinventory.alert.service.impl.AlertServiceImpl;
import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private ProductRepository productRepository;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertServiceImpl(productRepository);
    }

    @Test
    @DisplayName("Alert summary returns out-of-stock, low-stock, and total counts accurately")
    void getAlertSummary_Success() {
        when(productRepository.countOutOfStock()).thenReturn(3L);
        when(productRepository.countLowStock()).thenReturn(7L);
        when(productRepository.countReorderRequired()).thenReturn(10L);

        AlertSummaryDto summary = alertService.getAlertSummary();

        assertThat(summary.criticalCount()).isEqualTo(3L);
        assertThat(summary.warningCount()).isEqualTo(7L);
        assertThat(summary.totalAlerts()).isEqualTo(10L);
        assertThat(summary.reorderRequiredCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Get alerts maps out of stock as CRITICAL and low stock as WARNING")
    void getAlerts_ClassifiesSeverityCorrectly() {
        Product oos = new Product(1L, "Empty Stock Item", "SKU-OOS", "Desc", new BigDecimal("20.00"), 0, 5);
        Product low = new Product(2L, "Low Stock Item", "SKU-LOW", "Desc", new BigDecimal("30.00"), 3, 5);

        when(productRepository.findAllAlertProducts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(oos, low)));

        PageResponseDto<LowStockAlertDto> alerts = alertService.getAlerts(null, null, null, PageRequest.of(0, 10));

        assertThat(alerts.content()).hasSize(2);

        LowStockAlertDto alert1 = alerts.content().get(0);
        assertThat(alert1.alertType()).isEqualTo(AlertType.OUT_OF_STOCK);
        assertThat(alert1.severity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(alert1.id()).isEqualTo("ALERT-OOS-1");
        assertThat(alert1.message()).contains("completely out of stock");

        LowStockAlertDto alert2 = alerts.content().get(1);
        assertThat(alert2.alertType()).isEqualTo(AlertType.LOW_STOCK);
        assertThat(alert2.severity()).isEqualTo(AlertSeverity.WARNING);
        assertThat(alert2.id()).isEqualTo("ALERT-LOW-2");
        assertThat(alert2.message()).contains("low stock");
    }

    @Test
    @DisplayName("Get alerts filters by OUT_OF_STOCK type when requested")
    void getAlerts_FilterByOutOfStock() {
        Product oos = new Product(1L, "Empty Stock Item", "SKU-OOS", "Desc", new BigDecimal("20.00"), 0, 5);

        when(productRepository.findOutOfStockProducts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(oos)));

        PageResponseDto<LowStockAlertDto> alerts = alertService.getAlerts(AlertType.OUT_OF_STOCK, null, null, PageRequest.of(0, 10));

        assertThat(alerts.content()).hasSize(1);
        assertThat(alerts.content().get(0).severity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    @DisplayName("Get low stock alerts list returns all required products")
    void getLowStockAlerts_ListDirectAccess() {
        Product p = new Product(5L, "Reorder Needed", "SKU-REORDER", "Desc", new BigDecimal("10.00"), 2, 8);
        when(productRepository.findReorderRequiredProducts()).thenReturn(List.of(p));

        List<LowStockAlertDto> list = alertService.getLowStockAlerts();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).productId()).isEqualTo(5L);
        assertThat(list.get(0).suggestedReplenishmentQuantity()).isEqualTo(14); // (8 * 2) - 2 = 14
    }
}
