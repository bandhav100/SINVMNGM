package com.smartinventory.sale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateSalesOrderRequestDto(
        @Size(max = 255, message = "Customer name cannot exceed 255 characters")
        String customerName,

        @Size(max = 255, message = "Customer contact cannot exceed 255 characters")
        String customerContact,

        @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
        String notes,

        @NotEmpty(message = "Sales order must contain at least one item")
        @Valid
        List<SalesOrderItemRequestDto> items
) {}
