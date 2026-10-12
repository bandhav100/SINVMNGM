package com.smartinventory.sale.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.GlobalExceptionHandler;
import com.smartinventory.common.exception.InsufficientStockException;
import com.smartinventory.common.exception.InvalidStateTransitionException;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.config.JacksonConfig;
import com.smartinventory.sale.dto.*;
import com.smartinventory.sale.model.SalesOrderStatus;
import com.smartinventory.sale.service.SaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SaleControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    @Mock
    private SaleService saleService;

    @InjectMocks
    private SaleController saleController;

    private SalesOrderResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(saleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        sampleResponse = new SalesOrderResponseDto(
                1L,
                "SO-2026-TEST",
                SalesOrderStatus.DRAFT,
                new BigDecimal("150.00"),
                "Jane Doe",
                "+1-555-0100",
                "Customer requested express delivery",
                "seller",
                null,
                null,
                Instant.now(),
                Instant.now(),
                List.of(new SalesOrderItemResponseDto(
                        10L, 1L, "Widget X", "SKU-WX", 2, new BigDecimal("75.00"), new BigDecimal("150.00")
                ))
        );
    }

    @Test
    @DisplayName("POST /api/v1/sales creates draft sales order with 201 Created")
    void createSaleOrder_Valid_Returns201() throws Exception {
        when(saleService.createSaleOrder(any(), any())).thenReturn(sampleResponse);

        CreateSalesOrderRequestDto request = new CreateSalesOrderRequestDto(
                "Jane Doe",
                "+1-555-0100",
                "Customer requested express delivery",
                List.of(new SalesOrderItemRequestDto(1L, 2, new BigDecimal("75.00")))
        );

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderNumber").value("SO-2026-TEST"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(150.00));
    }

    @Test
    @DisplayName("POST /api/v1/sales with invalid quantity returns 400 Bad Request")
    void createSaleOrder_InvalidQuantity_Returns400() throws Exception {
        CreateSalesOrderRequestDto request = new CreateSalesOrderRequestDto(
                "Jane Doe", null, null,
                List.of(new SalesOrderItemRequestDto(1L, 0, new BigDecimal("75.00")))
        );

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors['items[0].quantity']").exists());
    }

    @Test
    @DisplayName("GET /api/v1/sales returns 200 with paginated sales summary")
    void getSalesOrders_Returns200() throws Exception {
        SalesOrderSummaryDto summary = new SalesOrderSummaryDto(
                1L, "SO-2026-TEST", SalesOrderStatus.DRAFT, new BigDecimal("150.00"),
                "Jane Doe", "+1-555-0100", 2, "seller", null, null, Instant.now()
        );
        PageResponseDto<SalesOrderSummaryDto> page = PageResponseDto.of(List.of(summary), 0, 10, 1L, 1, true, true);

        when(saleService.getSalesOrders(any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/sales")
                        .param("search", "Jane")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("SO-2026-TEST"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/sales/{id} returns 200 with full sale details")
    void getSaleOrderById_Found_Returns200() throws Exception {
        when(saleService.getSaleOrderById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/sales/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("Widget X"));
    }

    @Test
    @DisplayName("GET /api/v1/sales/{id} returns 404 when not found")
    void getSaleOrderById_NotFound_Returns404() throws Exception {
        when(saleService.getSaleOrderById(999L)).thenThrow(new ResourceNotFoundException("SalesOrder", "id", 999L));

        mockMvc.perform(get("/api/v1/sales/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("SalesOrder not found with id: '999'"));
    }

    @Test
    @DisplayName("PUT /api/v1/sales/{id} updates draft sales order and returns 200")
    void updateSaleOrder_Valid_Returns200() throws Exception {
        when(saleService.updateSaleOrder(eq(1L), any(), any())).thenReturn(sampleResponse);

        UpdateSalesOrderRequestDto request = new UpdateSalesOrderRequestDto(
                "Jane Doe", "+1-555-0100", "Updated notes",
                List.of(new SalesOrderItemRequestDto(1L, 2, new BigDecimal("75.00")))
        );

        mockMvc.perform(put("/api/v1/sales/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/sales/{id} deletes draft order with 204 No Content")
    void deleteSaleOrder_Draft_Returns204() throws Exception {
        doNothing().when(saleService).deleteSaleOrder(1L);

        mockMvc.perform(delete("/api/v1/sales/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/sales/{id}/complete completes sale and returns 200")
    void completeSaleOrder_Success_Returns200() throws Exception {
        SalesOrderResponseDto completed = new SalesOrderResponseDto(
                1L, "SO-2026-TEST", SalesOrderStatus.COMPLETED, new BigDecimal("150.00"),
                "Jane Doe", "+1-555-0100", "Notes", "seller", "seller", Instant.now(),
                Instant.now(), Instant.now(), List.of()
        );
        when(saleService.completeSaleOrder(eq(1L), any())).thenReturn(completed);

        mockMvc.perform(post("/api/v1/sales/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/sales/{id}/complete with insufficient stock returns 400 Bad Request")
    void completeSaleOrder_InsufficientStock_Returns400() throws Exception {
        when(saleService.completeSaleOrder(eq(1L), any()))
                .thenThrow(new InsufficientStockException("Insufficient stock for product 'Widget X'"));

        mockMvc.perform(post("/api/v1/sales/1/complete"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient stock for product 'Widget X'"));
    }

    @Test
    @DisplayName("POST /api/v1/sales/{id}/cancel cancels order and returns 200")
    void cancelSaleOrder_Success_Returns200() throws Exception {
        SalesOrderResponseDto cancelled = new SalesOrderResponseDto(
                1L, "SO-2026-TEST", SalesOrderStatus.CANCELLED, new BigDecimal("150.00"),
                "Jane Doe", "+1-555-0100", "Notes", "seller", null, null,
                Instant.now(), Instant.now(), List.of()
        );
        when(saleService.cancelSaleOrder(eq(1L), any())).thenReturn(cancelled);

        mockMvc.perform(post("/api/v1/sales/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
