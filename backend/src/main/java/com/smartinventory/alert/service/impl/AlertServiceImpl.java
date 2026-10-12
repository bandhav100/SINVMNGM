package com.smartinventory.alert.service.impl;

import com.smartinventory.alert.dto.AlertSummaryDto;
import com.smartinventory.alert.dto.LowStockAlertDto;
import com.smartinventory.alert.model.AlertSeverity;
import com.smartinventory.alert.model.AlertType;
import com.smartinventory.alert.service.AlertService;
import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AlertServiceImpl implements AlertService {

    private final ProductRepository productRepository;

    public AlertServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public AlertSummaryDto getAlertSummary() {
        long outOfStock = productRepository.countOutOfStock();
        long lowStock = productRepository.countLowStock();
        long reorderRequired = productRepository.countReorderRequired();

        return new AlertSummaryDto(
                reorderRequired,
                outOfStock,
                lowStock,
                reorderRequired
        );
    }

    @Override
    public PageResponseDto<LowStockAlertDto> getAlerts(
            AlertType alertType,
            AlertSeverity severity,
            String search,
            Pageable pageable
    ) {
        boolean filterOutOfStockOnly = (alertType == AlertType.OUT_OF_STOCK) || (severity == AlertSeverity.CRITICAL);
        boolean filterLowStockOnly = (alertType == AlertType.LOW_STOCK) || (severity == AlertSeverity.WARNING);
        boolean hasSearch = search != null && !search.isBlank();
        String query = hasSearch ? search.trim() : null;

        Page<Product> productPage;

        if (filterOutOfStockOnly) {
            productPage = hasSearch
                    ? productRepository.searchOutOfStockProducts(query, pageable)
                    : productRepository.findOutOfStockProducts(pageable);
        } else if (filterLowStockOnly) {
            productPage = hasSearch
                    ? productRepository.searchLowStockProducts(query, pageable)
                    : productRepository.findLowStockProducts(pageable);
        } else {
            productPage = hasSearch
                    ? productRepository.searchAlertProducts(query, pageable)
                    : productRepository.findAllAlertProducts(pageable);
        }

        return PageResponseDto.fromPage(productPage, this::mapToAlertDto);
    }

    @Override
    public List<LowStockAlertDto> getLowStockAlerts() {
        List<Product> products = productRepository.findReorderRequiredProducts();
        List<LowStockAlertDto> result = new ArrayList<>();
        for (Product p : products) {
            result.add(mapToAlertDto(p));
        }
        return result;
    }

    private LowStockAlertDto mapToAlertDto(Product p) {
        int qty = p.getQuantity() != null ? p.getQuantity() : 0;
        int reorderLevel = p.getReorderLevel() != null ? p.getReorderLevel() : 10;
        boolean isOos = qty <= 0;

        AlertType type = isOos ? AlertType.OUT_OF_STOCK : AlertType.LOW_STOCK;
        AlertSeverity sev = isOos ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        String id = (isOos ? "ALERT-OOS-" : "ALERT-LOW-") + p.getId();

        int target = Math.max(reorderLevel * 2, 1);
        int suggested = Math.max(reorderLevel, target - qty);

        String message = isOos
                ? "Product '" + p.getName() + "' is completely out of stock. Immediate replenishment required."
                : "Product '" + p.getName() + "' has low stock (" + qty + " units remaining, reorder threshold is " + reorderLevel + ").";

        Instant createdAt = p.getUpdatedAt() != null ? p.getUpdatedAt() : (p.getCreatedAt() != null ? p.getCreatedAt() : Instant.now());

        return new LowStockAlertDto(
                id,
                p.getId(),
                p.getName(),
                p.getSku(),
                p.getCategory() != null ? p.getCategory().getName() : null,
                qty,
                reorderLevel,
                type,
                sev,
                message,
                suggested,
                createdAt
        );
    }
}
