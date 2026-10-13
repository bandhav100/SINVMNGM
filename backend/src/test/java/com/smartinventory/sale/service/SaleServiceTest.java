package com.smartinventory.sale.service;

import com.smartinventory.common.exception.InsufficientStockException;
import com.smartinventory.common.exception.InvalidStateTransitionException;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.inventory.entity.InventoryTransaction;
import com.smartinventory.inventory.model.InventoryTransactionType;
import com.smartinventory.inventory.repository.InventoryTransactionRepository;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.sale.dto.*;
import com.smartinventory.sale.entity.SalesOrder;
import com.smartinventory.sale.entity.SalesOrderItem;
import com.smartinventory.sale.model.SalesOrderStatus;
import com.smartinventory.sale.repository.SalesOrderRepository;
import com.smartinventory.sale.service.impl.SaleServiceImpl;
import com.smartinventory.user.entity.User;
import com.smartinventory.user.model.Role;
import com.smartinventory.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SaleServiceImpl saleService;

    private User testUser;
    private Product testProduct1;
    private Product testProduct2;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("seller");
        testUser.setRole(Role.SALES_STAFF);

        testProduct1 = new Product(1L, "Widget A", "SKU-W-A", "Widget A Description", new BigDecimal("25.00"), 50, 10);
        testProduct2 = new Product(2L, "Widget B", "SKU-W-B", "Widget B Description", new BigDecimal("40.00"), 30, 5);
    }

    @Test
    @DisplayName("Create valid draft sales order with explicit unit prices calculates total amount correctly")
    void createSaleOrder_ExplicitPrices_Success() {
        when(userRepository.findByUsernameIgnoreCase("seller")).thenReturn(Optional.of(testUser));
        when(productRepository.findAllById(any())).thenReturn(List.of(testProduct1, testProduct2));
        when(salesOrderRepository.existsByOrderNumber(any())).thenReturn(false);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(invocation -> {
            SalesOrder so = invocation.getArgument(0);
            so.setId(100L);
            return so;
        });

        CreateSalesOrderRequestDto request = new CreateSalesOrderRequestDto(
                "Acme Corp",
                "555-1234",
                "Urgent order",
                List.of(
                        new SalesOrderItemRequestDto(1L, 2, new BigDecimal("24.50")),
                        new SalesOrderItemRequestDto(2L, 3, new BigDecimal("39.00"))
                )
        );

        SalesOrderResponseDto response = saleService.createSaleOrder(request, "seller");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(SalesOrderStatus.DRAFT);
        assertThat(response.customerName()).isEqualTo("Acme Corp");
        // 2 * 24.50 = 49.00; 3 * 39.00 = 117.00; Total = 166.00
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("166.00"));
        assertThat(response.items()).hasSize(2);
    }

    @Test
    @DisplayName("Create draft sales order with null unit price falls back to server-side product price")
    void createSaleOrder_NullPrice_FallsBackToProductPrice() {
        when(userRepository.findByUsernameIgnoreCase("seller")).thenReturn(Optional.of(testUser));
        when(productRepository.findAllById(any())).thenReturn(List.of(testProduct1));
        when(salesOrderRepository.existsByOrderNumber(any())).thenReturn(false);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(invocation -> {
            SalesOrder so = invocation.getArgument(0);
            so.setId(101L);
            return so;
        });

        CreateSalesOrderRequestDto request = new CreateSalesOrderRequestDto(
                "Retail Customer",
                null,
                null,
                List.of(new SalesOrderItemRequestDto(1L, 4, null))
        );

        SalesOrderResponseDto response = saleService.createSaleOrder(request, "seller");

        assertThat(response.status()).isEqualTo(SalesOrderStatus.DRAFT);
        // 4 * 25.00 = 100.00
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.items().get(0).unitPrice()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("Create sales order with duplicate product IDs throws IllegalArgumentException")
    void createSaleOrder_DuplicateProducts_ThrowsException() {
        CreateSalesOrderRequestDto request = new CreateSalesOrderRequestDto(
                "Test",
                null,
                null,
                List.of(
                        new SalesOrderItemRequestDto(1L, 2, new BigDecimal("25.00")),
                        new SalesOrderItemRequestDto(1L, 3, new BigDecimal("25.00"))
                )
        );

        assertThatThrownBy(() -> saleService.createSaleOrder(request, "seller"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate product ID found");
    }

    @Test
    @DisplayName("Create sales order with empty items throws IllegalArgumentException")
    void createSaleOrder_EmptyItems_ThrowsException() {
        CreateSalesOrderRequestDto request = new CreateSalesOrderRequestDto("Test", null, null, List.of());

        assertThatThrownBy(() -> saleService.createSaleOrder(request, "seller"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one item");
    }

    @Test
    @DisplayName("Create sales order with non-existent product throws ResourceNotFoundException")
    void createSaleOrder_NonexistentProduct_ThrowsException() {
        when(userRepository.findByUsernameIgnoreCase("seller")).thenReturn(Optional.of(testUser));
        when(productRepository.findAllById(any())).thenReturn(List.of()); // product 999 not found

        CreateSalesOrderRequestDto request = new CreateSalesOrderRequestDto(
                "Test",
                null,
                null,
                List.of(new SalesOrderItemRequestDto(999L, 1, new BigDecimal("10.00")))
        );

        assertThatThrownBy(() -> saleService.createSaleOrder(request, "seller"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Update sales order in DRAFT status updates items and totals successfully")
    void updateSaleOrder_ValidDraft_Success() {
        SalesOrder draftOrder = new SalesOrder();
        draftOrder.setId(10L);
        draftOrder.setOrderNumber("SO-2026-000001");
        draftOrder.setStatus(SalesOrderStatus.DRAFT);

        when(salesOrderRepository.findById(10L)).thenReturn(Optional.of(draftOrder));
        when(productRepository.findAllById(any())).thenReturn(List.of(testProduct2));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateSalesOrderRequestDto request = new UpdateSalesOrderRequestDto(
                "Updated Customer",
                "999-8888",
                "Updated notes",
                List.of(new SalesOrderItemRequestDto(2L, 5, new BigDecimal("40.00")))
        );

        SalesOrderResponseDto updated = saleService.updateSaleOrder(10L, request, "seller");

        assertThat(updated.customerName()).isEqualTo("Updated Customer");
        assertThat(updated.totalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("Update sales order in COMPLETED status throws InvalidStateTransitionException")
    void updateSaleOrder_CompletedOrder_ThrowsException() {
        SalesOrder completedOrder = new SalesOrder();
        completedOrder.setId(10L);
        completedOrder.setStatus(SalesOrderStatus.COMPLETED);

        when(salesOrderRepository.findById(10L)).thenReturn(Optional.of(completedOrder));

        UpdateSalesOrderRequestDto request = new UpdateSalesOrderRequestDto(
                "Customer", null, null,
                List.of(new SalesOrderItemRequestDto(1L, 1, new BigDecimal("25.00")))
        );

        assertThatThrownBy(() -> saleService.updateSaleOrder(10L, request, "seller"))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Only DRAFT sales orders can be updated");
    }

    @Test
    @DisplayName("Delete sales order in DRAFT status succeeds")
    void deleteSaleOrder_Draft_Success() {
        SalesOrder draftOrder = new SalesOrder();
        draftOrder.setId(10L);
        draftOrder.setStatus(SalesOrderStatus.DRAFT);

        when(salesOrderRepository.findById(10L)).thenReturn(Optional.of(draftOrder));

        saleService.deleteSaleOrder(10L);

        verify(salesOrderRepository, times(1)).delete(draftOrder);
    }

    @Test
    @DisplayName("Delete sales order in COMPLETED status throws InvalidStateTransitionException")
    void deleteSaleOrder_Completed_ThrowsException() {
        SalesOrder completedOrder = new SalesOrder();
        completedOrder.setId(10L);
        completedOrder.setStatus(SalesOrderStatus.COMPLETED);

        when(salesOrderRepository.findById(10L)).thenReturn(Optional.of(completedOrder));

        assertThatThrownBy(() -> saleService.deleteSaleOrder(10L))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Only DRAFT sales orders can be deleted");
    }

    @Test
    @DisplayName("Complete sales order with sufficient stock deducts inventory and writes SALE_DEDUCTION ledger")
    void completeSaleOrder_SufficientStock_Success() {
        SalesOrder order = new SalesOrder();
        order.setId(20L);
        order.setOrderNumber("SO-2026-COMPLETE");
        order.setStatus(SalesOrderStatus.DRAFT);

        SalesOrderItem item = new SalesOrderItem();
        item.setId(201L);
        item.setProduct(testProduct1); // quantity = 50
        item.setQuantity(15);
        item.setUnitPrice(new BigDecimal("25.00"));
        item.setLineTotal(new BigDecimal("375.00"));
        order.addItem(item);

        when(salesOrderRepository.findByIdWithLock(20L)).thenReturn(Optional.of(order));
        when(userRepository.findByUsernameIgnoreCase("seller")).thenReturn(Optional.of(testUser));
        when(productRepository.findAllByIdInOrderByIdAscForUpdate(List.of(1L))).thenReturn(List.of(testProduct1));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SalesOrderResponseDto completed = saleService.completeSaleOrder(20L, "seller");

        assertThat(completed.status()).isEqualTo(SalesOrderStatus.COMPLETED);
        assertThat(completed.completedAt()).isNotNull();

        // Product stock deducted from 50 to 35
        assertThat(testProduct1.getQuantity()).isEqualTo(35);
        verify(productRepository, times(1)).save(testProduct1);

        // Ledger entry verification
        ArgumentCaptor<InventoryTransaction> txnCaptor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(inventoryTransactionRepository, times(1)).save(txnCaptor.capture());
        InventoryTransaction txn = txnCaptor.getValue();
        assertThat(txn.getTransactionType()).isEqualTo(InventoryTransactionType.SALE_DEDUCTION);
        assertThat(txn.getQuantityDelta()).isEqualTo(-15);
        assertThat(txn.getQuantityBefore()).isEqualTo(50);
        assertThat(txn.getQuantityAfter()).isEqualTo(35);
        assertThat(txn.getReferenceType()).isEqualTo("SALES_ORDER");
        assertThat(txn.getReferenceId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Complete sales order with insufficient stock throws InsufficientStockException and does not deduct stock")
    void completeSaleOrder_InsufficientStock_ThrowsException() {
        SalesOrder order = new SalesOrder();
        order.setId(21L);
        order.setOrderNumber("SO-2026-INSUFFICIENT");
        order.setStatus(SalesOrderStatus.DRAFT);

        SalesOrderItem item = new SalesOrderItem();
        item.setId(202L);
        item.setProduct(testProduct1); // stock is 50
        item.setQuantity(75);          // requested 75 > 50
        item.setUnitPrice(new BigDecimal("25.00"));
        item.setLineTotal(new BigDecimal("1875.00"));
        order.addItem(item);

        when(salesOrderRepository.findByIdWithLock(21L)).thenReturn(Optional.of(order));
        when(userRepository.findByUsernameIgnoreCase("seller")).thenReturn(Optional.of(testUser));
        when(productRepository.findAllByIdInOrderByIdAscForUpdate(List.of(1L))).thenReturn(List.of(testProduct1));

        assertThatThrownBy(() -> saleService.completeSaleOrder(21L, "seller"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock for product 'Widget A'");

        // Stock remains untouched
        assertThat(testProduct1.getQuantity()).isEqualTo(50);
        verify(inventoryTransactionRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Complete sales order that is already COMPLETED throws InvalidStateTransitionException")
    void completeSaleOrder_AlreadyCompleted_ThrowsException() {
        SalesOrder order = new SalesOrder();
        order.setId(22L);
        order.setOrderNumber("SO-2026-ALREADY");
        order.setStatus(SalesOrderStatus.COMPLETED);

        when(salesOrderRepository.findByIdWithLock(22L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> saleService.completeSaleOrder(22L, "seller"))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("has already been completed");
    }

    @Test
    @DisplayName("Cancel draft sales order transitions status to CANCELLED")
    void cancelSaleOrder_Draft_Success() {
        SalesOrder order = new SalesOrder();
        order.setId(23L);
        order.setStatus(SalesOrderStatus.DRAFT);

        when(salesOrderRepository.findById(23L)).thenReturn(Optional.of(order));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SalesOrderResponseDto cancelled = saleService.cancelSaleOrder(23L, "seller");

        assertThat(cancelled.status()).isEqualTo(SalesOrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("Cancel completed sales order throws InvalidStateTransitionException")
    void cancelSaleOrder_Completed_ThrowsException() {
        SalesOrder order = new SalesOrder();
        order.setId(24L);
        order.setStatus(SalesOrderStatus.COMPLETED);

        when(salesOrderRepository.findById(24L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> saleService.cancelSaleOrder(24L, "seller"))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot cancel a sales order that has already been COMPLETED");
    }
}
