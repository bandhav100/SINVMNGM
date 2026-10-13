package com.smartinventory.category.dto;

import com.smartinventory.category.entity.Category;
import java.time.Instant;

public record CategoryResponseDto(
    Long id,
    String name,
    String description,
    long productCount,
    Instant createdAt,
    Instant updatedAt
) {
    public static CategoryResponseDto fromEntity(Category category, long productCount) {
        return new CategoryResponseDto(
            category.getId(),
            category.getName(),
            category.getDescription(),
            productCount,
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }
}
