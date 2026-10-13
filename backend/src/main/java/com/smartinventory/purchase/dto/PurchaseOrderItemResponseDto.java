package com.smartinventory.purchase.dto;

import com.smartinventory.purchase.entity.PurchaseOrderItem;
import java.math.BigDecimal;

public record PurchaseOrderItemResponseDto(
        Long id,
        Long productId,
        String productName,
        String productSku,
        Integer quantity,
        BigDecimal unitCost,
        BigDecimal totalCost
) {
    public static PurchaseOrderItemResponseDto fromEntity(PurchaseOrderItem item) {
        return new PurchaseOrderItemResponseDto(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getSku(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getTotalCost()
        );
    }
}
