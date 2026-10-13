package com.smartinventory.supplier.controller;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.GlobalExceptionHandler;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.supplier.dto.SupplierRequestDto;
import com.smartinventory.supplier.dto.SupplierResponseDto;
import com.smartinventory.supplier.service.SupplierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SupplierControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SupplierService supplierService;

    @InjectMocks
    private SupplierController supplierController;

    private SupplierResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(supplierController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        sampleResponse = new SupplierResponseDto(
                1L, "Global Logistics", "John Smith", "john@global.com", "+1-555-0100", "Industrial Way 10", true, Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/suppliers - Returns 201 Created")
    void createSupplier_Success() throws Exception {
        String json = """
                {
                    "name": "Global Logistics",
                    "contactPerson": "John Smith",
                    "email": "john@global.com",
                    "phone": "+1-555-0100",
                    "address": "Industrial Way 10",
                    "active": true
                }
                """;

        when(supplierService.createSupplier(any(SupplierRequestDto.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Global Logistics"));
    }

    @Test
    @DisplayName("POST /api/v1/suppliers - Invalid email returns 400 Bad Request")
    void createSupplier_InvalidEmail_ReturnsBadRequest() throws Exception {
        String json = """
                {
                    "name": "Global Logistics",
                    "email": "not-a-valid-email"
                }
                """;

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("GET /api/v1/suppliers - Returns 200 OK paginated")
    void getSuppliers_Success() throws Exception {
        PageResponseDto<SupplierResponseDto> page = new PageResponseDto<>(
                List.of(sampleResponse), 0, 10, 1L, 1, true, true
        );
        when(supplierService.getSuppliers(any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Global Logistics"));
    }

    @Test
    @DisplayName("GET /api/v1/suppliers/{id} - Not found returns 404")
    void getSupplierById_NotFound() throws Exception {
        when(supplierService.getSupplierById(999L))
                .thenThrow(new ResourceNotFoundException("Supplier not found with id: 999"));

        mockMvc.perform(get("/api/v1/suppliers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PATCH /api/v1/suppliers/{id}/status - Returns 200 OK")
    void toggleSupplierStatus_Success() throws Exception {
        when(supplierService.toggleSupplierStatus(eq(1L), eq(false))).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/suppliers/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{id} - Returns 204 No Content")
    void deleteSupplier_Success() throws Exception {
        doNothing().when(supplierService).deleteSupplier(1L);

        mockMvc.perform(delete("/api/v1/suppliers/1"))
                .andExpect(status().isNoContent());

        verify(supplierService).deleteSupplier(1L);
    }
}
