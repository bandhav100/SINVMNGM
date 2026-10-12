package com.smartinventory.purchase.dto;

import com.smartinventory.purchase.entity.PurchaseOrder;
import com.smartinventory.purchase.model.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderResponseDto(
        Long id,
        String orderNumber,
        Long supplierId,
        String supplierName,
        PurchaseOrderStatus status,
        BigDecimal totalAmount,
        LocalDate expectedDeliveryDate,
        String notes,
        String createdBy,
        Instant orderedAt,
        Instant receivedAt,
        String receivedBy,
        Instant createdAt,
        Instant updatedAt,
        List<PurchaseOrderItemResponseDto> items
) {
    public static PurchaseOrderResponseDto fromEntity(PurchaseOrder order) {
        List<PurchaseOrderItemResponseDto> itemDtos = order.getItems() != null
                ? order.getItems().stream().map(PurchaseOrderItemResponseDto::fromEntity).toList()
                : List.of();

        return new PurchaseOrderResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getSupplier().getId(),
                order.getSupplier().getName(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getExpectedDeliveryDate(),
                order.getNotes(),
                order.getCreatedBy() != null ? order.getCreatedBy().getUsername() : null,
                order.getOrderedAt(),
                order.getReceivedAt(),
                order.getReceivedBy() != null ? order.getReceivedBy().getUsername() : null,
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemDtos
        );
    }
}
