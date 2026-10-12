package com.smartinventory.product.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequestDto(
    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name cannot exceed 255 characters")
    String name,

    @NotBlank(message = "Product SKU is required")
    @Size(max = 100, message = "Product SKU cannot exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9_\\-]+$", message = "SKU must contain only alphanumeric characters, dashes, or underscores")
    String sku,

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    String description,

    @NotNull(message = "Product price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    BigDecimal price,

    @NotNull(message = "Product quantity is required")
    @Min(value = 0, message = "Quantity must be non-negative")
    Integer quantity,

    @Min(value = 0, message = "Reorder level must be non-negative")
    Integer reorderLevel,

    Long categoryId
) {}
