package com.smartinventory.sale.dto;

import com.smartinventory.sale.entity.SalesOrderItem;
import java.math.BigDecimal;

public record SalesOrderItemResponseDto(
        Long id,
        Long productId,
        String productName,
        String productSku,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
    public static SalesOrderItemResponseDto fromEntity(SalesOrderItem item) {
        return new SalesOrderItemResponseDto(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getSku(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()
        );
    }
}
