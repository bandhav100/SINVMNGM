package com.smartinventory.sale.dto;

import com.smartinventory.sale.entity.SalesOrder;
import com.smartinventory.sale.entity.SalesOrderItem;
import com.smartinventory.sale.model.SalesOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record SalesOrderSummaryDto(
        Long id,
        String orderNumber,
        SalesOrderStatus status,
        BigDecimal totalAmount,
        String customerName,
        String customerContact,
        Integer totalItems,
        String createdBy,
        String completedBy,
        Instant completedAt,
        Instant createdAt
) {
    public static SalesOrderSummaryDto fromEntity(SalesOrder order) {
        int totalItems = order.getItems() != null
                ? order.getItems().stream().mapToInt(SalesOrderItem::getQuantity).sum()
                : 0;

        return new SalesOrderSummaryDto(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCustomerName(),
                order.getCustomerContact(),
                totalItems,
                order.getCreatedBy() != null ? order.getCreatedBy().getUsername() : null,
                order.getCompletedBy() != null ? order.getCompletedBy().getUsername() : null,
                order.getCompletedAt(),
                order.getCreatedAt()
        );
    }
}
