package com.smartinventory.inventory.service;

import com.smartinventory.common.exception.InsufficientStockException;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.inventory.dto.StockAdjustmentRequestDto;
import com.smartinventory.inventory.dto.StockAdjustmentResponseDto;
import com.smartinventory.inventory.entity.InventoryTransaction;
import com.smartinventory.inventory.entity.StockAdjustment;
import com.smartinventory.inventory.repository.InventoryTransactionRepository;
import com.smartinventory.inventory.repository.StockAdjustmentRepository;
import com.smartinventory.inventory.service.impl.InventoryServiceImpl;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.user.entity.User;
import com.smartinventory.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock
    private StockAdjustmentRepository stockAdjustmentRepository;

    @Mock
    private UserRepository userRepository;

    private InventoryServiceImpl inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryServiceImpl(
                productRepository,
                inventoryTransactionRepository,
                stockAdjustmentRepository,
                userRepository
        );
    }

    @Test
    @DisplayName("adjustStock increments stock and saves adjustment & ledger records")
    void testAdjustStock_PositiveDelta_Success() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Widget");
        product.setSku("WDG-001");
        product.setQuantity(20);
        product.setPrice(new BigDecimal("15.00"));

        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(product));
        when(stockAdjustmentRepository.save(any(StockAdjustment.class))).thenAnswer(inv -> {
            StockAdjustment sa = inv.getArgument(0);
            sa.setId(101L);
            return sa;
        });

        StockAdjustmentRequestDto request = new StockAdjustmentRequestDto(
                1L, 10, "Found extra stock during audit", "Box mislabeled"
        );

        StockAdjustmentResponseDto response = inventoryService.adjustStock(request, "admin");

        assertNotNull(response);
        assertEquals(101L, response.id());
        assertEquals(10, response.quantityDelta());
        assertEquals(20, response.quantityBefore());
        assertEquals(30, response.quantityAfter());
        assertEquals("Found extra stock during audit", response.reason());

        assertEquals(30, product.getQuantity());
        verify(productRepository).save(product);
        verify(stockAdjustmentRepository).save(any(StockAdjustment.class));
        verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    @DisplayName("adjustStock decrements stock safely when stock is sufficient")
    void testAdjustStock_NegativeDelta_Success() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Widget");
        product.setSku("WDG-001");
        product.setQuantity(20);

        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(product));
        when(stockAdjustmentRepository.save(any(StockAdjustment.class))).thenAnswer(inv -> {
            StockAdjustment sa = inv.getArgument(0);
            sa.setId(102L);
            return sa;
        });

        StockAdjustmentRequestDto request = new StockAdjustmentRequestDto(
                1L, -5, "Damaged in transit", null
        );

        StockAdjustmentResponseDto response = inventoryService.adjustStock(request, "admin");

        assertNotNull(response);
        assertEquals(-5, response.quantityDelta());
        assertEquals(20, response.quantityBefore());
        assertEquals(15, response.quantityAfter());
        assertEquals(15, product.getQuantity());

        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("adjustStock throws InsufficientStockException when delta exceeds current stock")
    void testAdjustStock_NegativeStock_Rejection() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Widget");
        product.setQuantity(5);

        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(product));

        StockAdjustmentRequestDto request = new StockAdjustmentRequestDto(
                1L, -10, "Damaged", null
        );

        assertThrows(InsufficientStockException.class, () -> inventoryService.adjustStock(request, "admin"));
        verify(stockAdjustmentRepository, never()).save(any());
        verify(inventoryTransactionRepository, never()).save(any());
        verify(productRepository, never()).save(product);
    }

    @Test
    @DisplayName("adjustStock throws IllegalArgumentException when delta is zero")
    void testAdjustStock_ZeroDelta_Rejection() {
        StockAdjustmentRequestDto request = new StockAdjustmentRequestDto(
                1L, 0, "No change", null
        );

        assertThrows(IllegalArgumentException.class, () -> inventoryService.adjustStock(request, "admin"));
    }

    @Test
    @DisplayName("adjustStock throws ResourceNotFoundException when product does not exist")
    void testAdjustStock_ProductNotFound() {
        when(productRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        StockAdjustmentRequestDto request = new StockAdjustmentRequestDto(
                999L, 5, "Audit", null
        );

        assertThrows(ResourceNotFoundException.class, () -> inventoryService.adjustStock(request, "admin"));
    }
}
