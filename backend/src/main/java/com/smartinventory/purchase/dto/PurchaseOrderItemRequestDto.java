package com.smartinventory.purchase.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PurchaseOrderItemRequestDto(
        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "Unit cost is required")
        @DecimalMin(value = "0.0", message = "Unit cost cannot be negative")
        BigDecimal unitCost
) {
}
