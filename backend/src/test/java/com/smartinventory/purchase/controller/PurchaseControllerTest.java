package com.smartinventory.purchase.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.GlobalExceptionHandler;
import com.smartinventory.common.exception.InvalidStateTransitionException;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.config.JacksonConfig;
import com.smartinventory.purchase.dto.*;
import com.smartinventory.purchase.model.PurchaseOrderStatus;
import com.smartinventory.purchase.service.PurchaseService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PurchaseControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    @Mock
    private PurchaseService purchaseService;

    @InjectMocks
    private PurchaseController purchaseController;

    private PurchaseOrderResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(purchaseController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        sampleResponse = new PurchaseOrderResponseDto(
                1L,
                "PO-2026-TEST",
                1L,
                "Acme Logistics",
                PurchaseOrderStatus.DRAFT,
                BigDecimal.valueOf(150.00),
                LocalDate.now().plusDays(7),
                "Sample order",
                "admin",
                null,
                null,
                null,
                Instant.now(),
                Instant.now(),
                List.of(new PurchaseOrderItemResponseDto(1L, 10L, "Widget A", "SKU-A", 10, BigDecimal.valueOf(15.00), BigDecimal.valueOf(150.00)))
        );
    }

    @Test
    @DisplayName("POST /api/v1/purchases - Success returns 201 Created")
    void createPurchaseOrder_Success() throws Exception {
        CreatePurchaseOrderRequestDto request = new CreatePurchaseOrderRequestDto(
                1L,
                LocalDate.now().plusDays(7),
                "Sample order",
                PurchaseOrderStatus.DRAFT,
                List.of(new PurchaseOrderItemRequestDto(10L, 10, BigDecimal.valueOf(15.00)))
        );

        when(purchaseService.createPurchaseOrder(any(CreatePurchaseOrderRequestDto.class), any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderNumber").value("PO-2026-TEST"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(150.00));
    }

    @Test
    @DisplayName("POST /api/v1/purchases - Validation failure on empty items returns 400 Bad Request")
    void createPurchaseOrder_EmptyItems_Returns400() throws Exception {
        CreatePurchaseOrderRequestDto request = new CreatePurchaseOrderRequestDto(
                1L,
                LocalDate.now().plusDays(7),
                "Empty items order",
                PurchaseOrderStatus.DRAFT,
                List.of()
        );

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.items").exists());
    }

    @Test
    @DisplayName("GET /api/v1/purchases - Returns 200 OK paginated list")
    void getPurchaseOrders_Success() throws Exception {
        PurchaseOrderSummaryDto summary = new PurchaseOrderSummaryDto(
                1L, "PO-2026-TEST", 1L, "Acme Logistics", PurchaseOrderStatus.DRAFT,
                BigDecimal.valueOf(150.00), 1, LocalDate.now().plusDays(7), null, null, Instant.now()
        );

        PageResponseDto<PurchaseOrderSummaryDto> page = PageResponseDto.of(
                List.of(summary), 0, 20, 1L, 1, true, true
        );

        when(purchaseService.getPurchaseOrders(any(), any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/purchases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("PO-2026-TEST"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/purchases/{id} - Success returns 200 OK")
    void getPurchaseOrderById_Success() throws Exception {
        when(purchaseService.getPurchaseOrderById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/purchases/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderNumber").value("PO-2026-TEST"));
    }

    @Test
    @DisplayName("GET /api/v1/purchases/{id} - Not found returns 404")
    void getPurchaseOrderById_NotFound() throws Exception {
        when(purchaseService.getPurchaseOrderById(999L)).thenThrow(new ResourceNotFoundException("PurchaseOrder", "id", 999L));

        mockMvc.perform(get("/api/v1/purchases/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/v1/purchases/{id}/submit - Transitions DRAFT to ORDERED")
    void submitPurchaseOrder_Success() throws Exception {
        PurchaseOrderResponseDto ordered = new PurchaseOrderResponseDto(
                1L, "PO-2026-TEST", 1L, "Acme Logistics", PurchaseOrderStatus.ORDERED,
                BigDecimal.valueOf(150.00), LocalDate.now().plusDays(7), "Sample", "admin",
                Instant.now(), null, null, Instant.now(), Instant.now(), List.of()
        );

        when(purchaseService.submitPurchaseOrder(eq(1L), any())).thenReturn(ordered);

        mockMvc.perform(post("/api/v1/purchases/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ORDERED"));
    }

    @Test
    @DisplayName("POST /api/v1/purchases/{id}/receive - Transitions ORDERED to RECEIVED")
    void receivePurchaseOrder_Success() throws Exception {
        PurchaseOrderResponseDto received = new PurchaseOrderResponseDto(
                1L, "PO-2026-TEST", 1L, "Acme Logistics", PurchaseOrderStatus.RECEIVED,
                BigDecimal.valueOf(150.00), LocalDate.now().plusDays(7), "Sample", "admin",
                Instant.now(), Instant.now(), "admin", Instant.now(), Instant.now(), List.of()
        );

        when(purchaseService.receivePurchaseOrder(eq(1L), any())).thenReturn(received);

        mockMvc.perform(post("/api/v1/purchases/1/receive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    @DisplayName("POST /api/v1/purchases/{id}/receive - Illegal state transition returns 400 Bad Request")
    void receivePurchaseOrder_InvalidState_Returns400() throws Exception {
        when(purchaseService.receivePurchaseOrder(eq(1L), any()))
                .thenThrow(new InvalidStateTransitionException("Order must be in ORDERED status"));

        mockMvc.perform(post("/api/v1/purchases/1/receive"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Order must be in ORDERED status"));
    }

    @Test
    @DisplayName("POST /api/v1/purchases/{id}/cancel - Success transitions to CANCELLED")
    void cancelPurchaseOrder_Success() throws Exception {
        PurchaseOrderResponseDto cancelled = new PurchaseOrderResponseDto(
                1L, "PO-2026-TEST", 1L, "Acme Logistics", PurchaseOrderStatus.CANCELLED,
                BigDecimal.valueOf(150.00), LocalDate.now().plusDays(7), "Sample", "admin",
                null, null, null, Instant.now(), Instant.now(), List.of()
        );

        when(purchaseService.cancelPurchaseOrder(eq(1L), any())).thenReturn(cancelled);

        mockMvc.perform(post("/api/v1/purchases/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/purchases/{id} - Success returns 204 No Content")
    void deletePurchaseOrder_Success() throws Exception {
        doNothing().when(purchaseService).deletePurchaseOrder(1L);

        mockMvc.perform(delete("/api/v1/purchases/1"))
                .andExpect(status().isNoContent());
    }
}
