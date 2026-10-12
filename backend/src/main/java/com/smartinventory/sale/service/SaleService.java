package com.smartinventory.sale.service;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.sale.dto.*;
import com.smartinventory.sale.model.SalesOrderStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface SaleService {

    SalesOrderResponseDto createSaleOrder(CreateSalesOrderRequestDto request, String currentUsername);

    PageResponseDto<SalesOrderSummaryDto> getSalesOrders(
            String search,
            SalesOrderStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    SalesOrderResponseDto getSaleOrderById(Long id);

    SalesOrderResponseDto updateSaleOrder(Long id, UpdateSalesOrderRequestDto request, String currentUsername);

    void deleteSaleOrder(Long id);

    SalesOrderResponseDto completeSaleOrder(Long id, String currentUsername);

    SalesOrderResponseDto cancelSaleOrder(Long id, String currentUsername);
}
