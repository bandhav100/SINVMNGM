package com.smartinventory.product.controller;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.DuplicateResourceException;
import com.smartinventory.common.exception.GlobalExceptionHandler;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.product.dto.ProductRequestDto;
import com.smartinventory.product.dto.ProductResponseDto;
import com.smartinventory.product.service.ProductService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private ProductResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        sampleResponse = new ProductResponseDto(
                1L, "Mechanical Keyboard", "KB-001", "RGB Gaming Keyboard",
                new BigDecimal("89.99"), 50, 10, null, null, Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/products - Success returns 201 Created")
    void createProduct_Success() throws Exception {
        String requestJson = """
                {
                    "name": "Mechanical Keyboard",
                    "sku": "KB-001",
                    "description": "RGB Gaming Keyboard",
                    "price": 89.99,
                    "quantity": 50,
                    "reorderLevel": 10
                }
                """;

        when(productService.createProduct(any(ProductRequestDto.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.sku").value("KB-001"))
                .andExpect(jsonPath("$.name").value("Mechanical Keyboard"));
    }

    @Test
    @DisplayName("POST /api/v1/products - Duplicate SKU returns 409 Conflict")
    void createProduct_DuplicateSku_ReturnsConflict() throws Exception {
        String requestJson = """
                {
                    "name": "Mechanical Keyboard",
                    "sku": "KB-DUPLICATE",
                    "description": "RGB Gaming Keyboard",
                    "price": 89.99,
                    "quantity": 50,
                    "reorderLevel": 10
                }
                """;

        when(productService.createProduct(any(ProductRequestDto.class)))
                .thenThrow(new DuplicateResourceException("Product with SKU 'KB-DUPLICATE' already exists"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Product with SKU 'KB-DUPLICATE' already exists"));
    }

    @Test
    @DisplayName("POST /api/v1/products - Validation failure returns 400 Bad Request")
    void createProduct_ValidationError_ReturnsBadRequest() throws Exception {
        String invalidRequestJson = """
                {
                    "name": "",
                    "sku": "",
                    "description": "Invalid product",
                    "price": -5.00,
                    "quantity": -1,
                    "reorderLevel": -2
                }
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.sku").exists())
                .andExpect(jsonPath("$.validationErrors.price").exists())
                .andExpect(jsonPath("$.validationErrors.quantity").exists());
    }

    @Test
    @DisplayName("GET /api/v1/products - Returns 200 OK with paginated list")
    void getProducts_Success() throws Exception {
        PageResponseDto<ProductResponseDto> pageResponse = new PageResponseDto<>(
                List.of(sampleResponse), 0, 10, 1L, 1, true, true
        );

        when(productService.getProducts(any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/products?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].sku").value("KB-001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Success returns 200 OK")
    void getProductById_Success() throws Exception {
        when(productService.getProductById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.sku").value("KB-001"));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Nonexistent ID returns 404 Not Found")
    void getProductById_NotFound() throws Exception {
        when(productService.getProductById(999L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));

        mockMvc.perform(get("/api/v1/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found with id: 999"));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} - Success returns 200 OK")
    void updateProduct_Success() throws Exception {
        String updateRequestJson = """
                {
                    "name": "Mechanical Keyboard v2",
                    "sku": "KB-001",
                    "description": "Updated description",
                    "price": 99.99,
                    "quantity": 60,
                    "reorderLevel": 15
                }
                """;

        ProductResponseDto updatedResponse = new ProductResponseDto(
                1L, "Mechanical Keyboard v2", "KB-001", "Updated description",
                new BigDecimal("99.99"), 60, 15, null, null, Instant.now(), Instant.now()
        );

        when(productService.updateProduct(eq(1L), any(ProductRequestDto.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mechanical Keyboard v2"))
                .andExpect(jsonPath("$.price").value(99.99));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} - Success returns 204 No Content")
    void deleteProduct_Success() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }
}
