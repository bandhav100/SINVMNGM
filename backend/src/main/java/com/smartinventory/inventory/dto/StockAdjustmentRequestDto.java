package com.smartinventory.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockAdjustmentRequestDto(
        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Quantity delta is required")
        Integer quantityDelta,

        @NotBlank(message = "Reason for stock adjustment is mandatory")
        @Size(max = 255, message = "Reason must not exceed 255 characters")
        String reason,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes
) {}
