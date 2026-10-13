package com.smartinventory.purchase.service;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.purchase.dto.CreatePurchaseOrderRequestDto;
import com.smartinventory.purchase.dto.PurchaseOrderResponseDto;
import com.smartinventory.purchase.dto.PurchaseOrderSummaryDto;
import com.smartinventory.purchase.dto.UpdatePurchaseOrderRequestDto;
import com.smartinventory.purchase.model.PurchaseOrderStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface PurchaseService {

    PurchaseOrderResponseDto createPurchaseOrder(CreatePurchaseOrderRequestDto request, String currentUsername);

    PageResponseDto<PurchaseOrderSummaryDto> getPurchaseOrders(
            String search,
            PurchaseOrderStatus status,
            Long supplierId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    PurchaseOrderResponseDto getPurchaseOrderById(Long id);

    PurchaseOrderResponseDto updatePurchaseOrder(Long id, UpdatePurchaseOrderRequestDto request, String currentUsername);

    void deletePurchaseOrder(Long id);

    PurchaseOrderResponseDto submitPurchaseOrder(Long id, String currentUsername);

    PurchaseOrderResponseDto receivePurchaseOrder(Long id, String currentUsername);

    PurchaseOrderResponseDto cancelPurchaseOrder(Long id, String currentUsername);
}
