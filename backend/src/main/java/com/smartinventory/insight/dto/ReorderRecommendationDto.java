package com.smartinventory.insight.dto;

public record ReorderRecommendationDto(
        Long productId,
        String productName,
        String sku,
        String categoryName,
        Integer currentStock,
        Integer reorderLevel,
        Integer suggestedReplenishmentQuantity,
        String recommendationReason,
        String recommendationType
) {}
