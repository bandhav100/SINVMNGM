package com.smartinventory.purchase.dto;

import com.smartinventory.purchase.entity.PurchaseOrder;
import com.smartinventory.purchase.model.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PurchaseOrderSummaryDto(
        Long id,
        String orderNumber,
        Long supplierId,
        String supplierName,
        PurchaseOrderStatus status,
        BigDecimal totalAmount,
        Integer itemsCount,
        LocalDate expectedDeliveryDate,
        Instant orderedAt,
        Instant receivedAt,
        Instant createdAt
) {
    public static PurchaseOrderSummaryDto fromEntity(PurchaseOrder order) {
        int count = order.getItems() != null ? order.getItems().size() : 0;
        return new PurchaseOrderSummaryDto(
                order.getId(),
                order.getOrderNumber(),
                order.getSupplier().getId(),
                order.getSupplier().getName(),
                order.getStatus(),
                order.getTotalAmount(),
                count,
                order.getExpectedDeliveryDate(),
                order.getOrderedAt(),
                order.getReceivedAt(),
                order.getCreatedAt()
        );
    }
}
