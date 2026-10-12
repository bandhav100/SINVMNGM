package com.smartinventory.purchase.service;

import com.smartinventory.common.exception.InvalidStateTransitionException;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.inventory.entity.InventoryTransaction;
import com.smartinventory.inventory.model.InventoryTransactionType;
import com.smartinventory.inventory.repository.InventoryTransactionRepository;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.purchase.dto.CreatePurchaseOrderRequestDto;
import com.smartinventory.purchase.dto.PurchaseOrderItemRequestDto;
import com.smartinventory.purchase.dto.PurchaseOrderResponseDto;
import com.smartinventory.purchase.entity.PurchaseOrder;
import com.smartinventory.purchase.entity.PurchaseOrderItem;
import com.smartinventory.purchase.model.PurchaseOrderStatus;
import com.smartinventory.purchase.repository.PurchaseOrderRepository;
import com.smartinventory.purchase.service.impl.PurchaseServiceImpl;
import com.smartinventory.supplier.entity.Supplier;
import com.smartinventory.supplier.repository.SupplierRepository;
import com.smartinventory.user.entity.User;
import com.smartinventory.user.model.Role;
import com.smartinventory.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @InjectMocks
    private PurchaseServiceImpl purchaseService;

    private Supplier activeSupplier;
    private Supplier inactiveSupplier;
    private Product product1;
    private Product product2;
    private User testUser;

    @BeforeEach
    void setUp() {
        activeSupplier = new Supplier(1L, "Acme Logistics", "Alice", "alice@acme.local", "123", "City", true);
        inactiveSupplier = new Supplier(2L, "Inactive Vendor", "Bob", "bob@inactive.local", "456", "City", false);

        product1 = new Product(10L, "Widget A", "SKU-A", "Desc", BigDecimal.valueOf(15.00), 50, 10);
        product2 = new Product(20L, "Widget B", "SKU-B", "Desc", BigDecimal.valueOf(30.00), 20, 5);

        testUser = new User();
        testUser.setId(100L);
        testUser.setUsername("purchaser");
        testUser.setRole(Role.PURCHASE_MANAGER);
    }

    @Test
    @DisplayName("createPurchaseOrder should create order with DRAFT status and calculate totals correctly")
    void createPurchaseOrder_Success_Draft() {
        CreatePurchaseOrderRequestDto request = new CreatePurchaseOrderRequestDto(
                1L,
                LocalDate.now().plusDays(7),
                "Restock items",
                PurchaseOrderStatus.DRAFT,
                List.of(
                        new PurchaseOrderItemRequestDto(10L, 10, BigDecimal.valueOf(12.50)),
                        new PurchaseOrderItemRequestDto(20L, 5, BigDecimal.valueOf(25.00))
                )
        );

        when(userRepository.findByUsernameIgnoreCase("purchaser")).thenReturn(Optional.of(testUser));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(activeSupplier));
        when(productRepository.findAllById(any())).thenReturn(List.of(product1, product2));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> {
            PurchaseOrder saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        PurchaseOrderResponseDto response = purchaseService.createPurchaseOrder(request, "purchaser");

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.DRAFT);
        // Total = 10 * 12.50 (125.00) + 5 * 25.00 (125.00) = 250.00
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(250.00));
        assertThat(response.items()).hasSize(2);
    }

    @Test
    @DisplayName("createPurchaseOrder should throw IllegalArgumentException when supplier is inactive")
    void createPurchaseOrder_InactiveSupplier() {
        CreatePurchaseOrderRequestDto request = new CreatePurchaseOrderRequestDto(
                2L,
                LocalDate.now().plusDays(7),
                "Restock items",
                PurchaseOrderStatus.DRAFT,
                List.of(new PurchaseOrderItemRequestDto(10L, 10, BigDecimal.valueOf(12.50)))
        );

        when(supplierRepository.findById(2L)).thenReturn(Optional.of(inactiveSupplier));

        assertThatThrownBy(() -> purchaseService.createPurchaseOrder(request, "purchaser"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot create purchase order for inactive supplier");
    }

    @Test
    @DisplayName("createPurchaseOrder should throw IllegalArgumentException when duplicate product IDs are present")
    void createPurchaseOrder_DuplicateProducts() {
        CreatePurchaseOrderRequestDto request = new CreatePurchaseOrderRequestDto(
                1L,
                LocalDate.now().plusDays(7),
                "Restock items",
                PurchaseOrderStatus.DRAFT,
                List.of(
                        new PurchaseOrderItemRequestDto(10L, 10, BigDecimal.valueOf(12.50)),
                        new PurchaseOrderItemRequestDto(10L, 5, BigDecimal.valueOf(12.50))
                )
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(activeSupplier));

        assertThatThrownBy(() -> purchaseService.createPurchaseOrder(request, "purchaser"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate product ID found");
    }

    @Test
    @DisplayName("submitPurchaseOrder should transition DRAFT order to ORDERED")
    void submitPurchaseOrder_Success() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setOrderNumber("PO-2026-TEST");
        order.setSupplier(activeSupplier);
        order.setStatus(PurchaseOrderStatus.DRAFT);
        order.addItem(new PurchaseOrderItem(product1, 10, BigDecimal.valueOf(15.0), BigDecimal.valueOf(150.0)));

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(order);

        PurchaseOrderResponseDto response = purchaseService.submitPurchaseOrder(1L, "purchaser");

        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.ORDERED);
        assertThat(order.getOrderedAt()).isNotNull();
    }

    @Test
    @DisplayName("submitPurchaseOrder should reject transition if order is not in DRAFT status")
    void submitPurchaseOrder_NotDraft() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setStatus(PurchaseOrderStatus.ORDERED);

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> purchaseService.submitPurchaseOrder(1L, "purchaser"))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Only DRAFT purchase orders can be submitted");
    }

    @Test
    @DisplayName("receivePurchaseOrder should atomically increment product stock, create ledger entries, and mark order RECEIVED")
    void receivePurchaseOrder_Success() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setOrderNumber("PO-2026-TEST");
        order.setSupplier(activeSupplier);
        order.setStatus(PurchaseOrderStatus.ORDERED);
        order.addItem(new PurchaseOrderItem(product1, 20, BigDecimal.valueOf(15.0), BigDecimal.valueOf(300.0)));

        when(purchaseOrderRepository.findByIdWithLock(1L)).thenReturn(Optional.of(order));
        when(userRepository.findByUsernameIgnoreCase("purchaser")).thenReturn(Optional.of(testUser));
        when(productRepository.findAllByIdInOrderByIdAscForUpdate(List.of(10L))).thenReturn(List.of(product1));
        when(inventoryTransactionRepository.existsByReferenceTypeAndReferenceIdAndProductId("PURCHASE_ORDER", 1L, 10L)).thenReturn(false);
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(order);

        int initialQty = product1.getQuantity(); // 50

        PurchaseOrderResponseDto response = purchaseService.receivePurchaseOrder(1L, "purchaser");

        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(product1.getQuantity()).isEqualTo(initialQty + 20); // 70
        assertThat(order.getReceivedAt()).isNotNull();

        verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
        verify(productRepository).save(product1);
    }

    @Test
    @DisplayName("receivePurchaseOrder should reject receiving if order is already in RECEIVED status")
    void receivePurchaseOrder_AlreadyReceived() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setOrderNumber("PO-2026-TEST");
        order.setStatus(PurchaseOrderStatus.RECEIVED);

        when(purchaseOrderRepository.findByIdWithLock(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> purchaseService.receivePurchaseOrder(1L, "purchaser"))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("has already been received");
    }

    @Test
    @DisplayName("cancelPurchaseOrder should transition DRAFT or ORDERED to CANCELLED")
    void cancelPurchaseOrder_Success() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setSupplier(activeSupplier);
        order.setStatus(PurchaseOrderStatus.ORDERED);

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(order);

        PurchaseOrderResponseDto response = purchaseService.cancelPurchaseOrder(1L, "purchaser");

        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelPurchaseOrder should reject cancellation if order is already RECEIVED")
    void cancelPurchaseOrder_AlreadyReceived() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setStatus(PurchaseOrderStatus.RECEIVED);

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> purchaseService.cancelPurchaseOrder(1L, "purchaser"))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot cancel a purchase order that has already been RECEIVED");
    }
}
