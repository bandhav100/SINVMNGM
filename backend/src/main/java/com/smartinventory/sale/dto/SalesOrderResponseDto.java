package com.smartinventory.sale.dto;

import com.smartinventory.sale.entity.SalesOrder;
import com.smartinventory.sale.model.SalesOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SalesOrderResponseDto(
        Long id,
        String orderNumber,
        SalesOrderStatus status,
        BigDecimal totalAmount,
        String customerName,
        String customerContact,
        String notes,
        String createdBy,
        String completedBy,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        List<SalesOrderItemResponseDto> items
) {
    public static SalesOrderResponseDto fromEntity(SalesOrder order) {
        List<SalesOrderItemResponseDto> itemDtos = order.getItems() != null
                ? order.getItems().stream().map(SalesOrderItemResponseDto::fromEntity).toList()
                : List.of();

        return new SalesOrderResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCustomerName(),
                order.getCustomerContact(),
                order.getNotes(),
                order.getCreatedBy() != null ? order.getCreatedBy().getUsername() : null,
                order.getCompletedBy() != null ? order.getCompletedBy().getUsername() : null,
                order.getCompletedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemDtos
        );
    }
}
