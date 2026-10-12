package com.smartinventory.product.dto;

import com.smartinventory.product.entity.Product;
import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponseDto(
    Long id,
    String name,
    String sku,
    String description,
    BigDecimal price,
    Integer quantity,
    Integer reorderLevel,
    Long categoryId,
    String categoryName,
    Instant createdAt,
    Instant updatedAt
) {
    public static ProductResponseDto fromEntity(Product product) {
        return new ProductResponseDto(
            product.getId(),
            product.getName(),
            product.getSku(),
            product.getDescription(),
            product.getPrice(),
            product.getQuantity(),
            product.getReorderLevel(),
            product.getCategory() != null ? product.getCategory().getId() : null,
            product.getCategory() != null ? product.getCategory().getName() : null,
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
