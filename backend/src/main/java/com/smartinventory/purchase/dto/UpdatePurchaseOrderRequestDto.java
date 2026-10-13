package com.smartinventory.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record UpdatePurchaseOrderRequestDto(
        @NotNull(message = "Supplier ID is required")
        Long supplierId,

        LocalDate expectedDeliveryDate,

        String notes,

        @NotEmpty(message = "Purchase order must contain at least one item")
        @Valid
        List<PurchaseOrderItemRequestDto> items
) {
}
