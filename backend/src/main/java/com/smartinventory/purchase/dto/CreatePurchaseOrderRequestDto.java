package com.smartinventory.purchase.dto;

import com.smartinventory.purchase.model.PurchaseOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CreatePurchaseOrderRequestDto(
        @NotNull(message = "Supplier ID is required")
        Long supplierId,

        LocalDate expectedDeliveryDate,

        String notes,

        PurchaseOrderStatus status,

        @NotEmpty(message = "Purchase order must contain at least one item")
        @Valid
        List<PurchaseOrderItemRequestDto> items
) {
}
