package com.smartinventory.purchase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.inventory.entity.InventoryTransaction;
import com.smartinventory.inventory.repository.InventoryTransactionRepository;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.purchase.dto.CreatePurchaseOrderRequestDto;
import com.smartinventory.purchase.dto.PurchaseOrderItemRequestDto;
import com.smartinventory.purchase.dto.PurchaseOrderResponseDto;
import com.smartinventory.purchase.model.PurchaseOrderStatus;
import com.smartinventory.purchase.repository.PurchaseOrderRepository;
import com.smartinventory.purchase.service.PurchaseService;
import com.smartinventory.security.jwt.JwtService;
import com.smartinventory.supplier.entity.Supplier;
import com.smartinventory.supplier.repository.SupplierRepository;
import com.smartinventory.user.entity.User;
import com.smartinventory.user.model.Role;
import com.smartinventory.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class PurchaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private Supplier testSupplier;
    private Product testProduct;
    private User adminUser;
    private User purchaseUser;
    private User inventoryUser;
    private User salesUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        inventoryTransactionRepository.deleteAll();
        purchaseOrderRepository.deleteAll();
        userRepository.deleteAll();

        // Setup users
        adminUser = createTestUser("po_admin", Role.ADMIN);
        purchaseUser = createTestUser("po_pm", Role.PURCHASE_MANAGER);
        inventoryUser = createTestUser("po_im", Role.INVENTORY_MANAGER);
        salesUser = createTestUser("po_sales", Role.SALES_STAFF);

        // Setup supplier and product
        testSupplier = new Supplier((Long) null, "Reliable Logistics", "Bob", "bob@reliable.local", "555-1234", "Warehouse 5", true);
        testSupplier = supplierRepository.save(testSupplier);

        testProduct = new Product((Long) null, "Test Integration Gadget", "GADGET-TEST-" + System.currentTimeMillis(), "Description", BigDecimal.valueOf(20.00), 10, 5);
        testProduct = productRepository.save(testProduct);
    }

    @AfterEach
    void tearDown() {
        inventoryTransactionRepository.deleteAll();
        purchaseOrderRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createTestUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@sims.local");
        user.setFullName("User " + username);
        user.setPasswordHash(passwordEncoder.encode("Password@123"));
        user.setRole(role);
        user.setActive(true);
        return userRepository.save(user);
    }

    private String getAuthToken(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    @Test
    @DisplayName("Complete Purchase Lifecycle: DRAFT -> ORDERED -> RECEIVED increments product stock and writes ledger")
    void testPurchaseLifecycle_StockIncrementAndLedger() {
        int initialStock = testProduct.getQuantity(); // 10
        int receivedQty = 25;

        // 1. Create Draft PO
        CreatePurchaseOrderRequestDto request = new CreatePurchaseOrderRequestDto(
                testSupplier.getId(),
                LocalDate.now().plusDays(5),
                "Restock order",
                PurchaseOrderStatus.DRAFT,
                List.of(new PurchaseOrderItemRequestDto(testProduct.getId(), receivedQty, BigDecimal.valueOf(18.50)))
        );

        PurchaseOrderResponseDto created = purchaseService.createPurchaseOrder(request, purchaseUser.getUsername());
        assertThat(created.status()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(created.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(18.50 * 25));

        // Verify product stock has NOT changed yet
        Product currentProd = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(currentProd.getQuantity()).isEqualTo(initialStock);

        // 2. Submit to ORDERED
        PurchaseOrderResponseDto submitted = purchaseService.submitPurchaseOrder(created.id(), purchaseUser.getUsername());
        assertThat(submitted.status()).isEqualTo(PurchaseOrderStatus.ORDERED);

        // 3. Receive Order
        PurchaseOrderResponseDto received = purchaseService.receivePurchaseOrder(created.id(), inventoryUser.getUsername());
        assertThat(received.status()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(received.receivedBy()).isEqualTo(inventoryUser.getUsername());

        // 4. Verify product stock quantity is incremented by exactly receivedQty
        Product updatedProd = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProd.getQuantity()).isEqualTo(initialStock + receivedQty);

        // 5. Verify inventory transaction ledger entry
        List<InventoryTransaction> transactions = inventoryTransactionRepository.findByReferenceTypeAndReferenceId("PURCHASE_ORDER", created.id());
        assertThat(transactions).hasSize(1);
        InventoryTransaction txn = transactions.get(0);
        assertThat(txn.getQuantityDelta()).isEqualTo(receivedQty);
        assertThat(txn.getQuantityBefore()).isEqualTo(initialStock);
        assertThat(txn.getQuantityAfter()).isEqualTo(initialStock + receivedQty);
        assertThat(txn.getPerformedBy().getUsername()).isEqualTo(inventoryUser.getUsername());
    }

    @Test
    @DisplayName("Concurrent receive requests must execute atomically and never increment stock twice")
    void testConcurrentReceiveRequests_Idempotent() throws Exception {
        int initialStock = testProduct.getQuantity();
        int receiveQty = 15;

        CreatePurchaseOrderRequestDto request = new CreatePurchaseOrderRequestDto(
                testSupplier.getId(),
                LocalDate.now().plusDays(5),
                "Concurrent test order",
                PurchaseOrderStatus.ORDERED,
                List.of(new PurchaseOrderItemRequestDto(testProduct.getId(), receiveQty, BigDecimal.valueOf(15.00)))
        );

        PurchaseOrderResponseDto created = purchaseService.createPurchaseOrder(request, adminUser.getUsername());
        Long orderId = created.id();

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    purchaseService.receivePurchaseOrder(orderId, adminUser.getUsername());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        boolean completed = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // Exactly one concurrent receive request should succeed
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(threadCount - 1);

        // Product stock must be incremented EXACTLY once
        Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(finalProduct.getQuantity()).isEqualTo(initialStock + receiveQty);

        // Ledger must contain EXACTLY 1 transaction entry
        List<InventoryTransaction> txns = inventoryTransactionRepository.findByReferenceTypeAndReferenceId("PURCHASE_ORDER", orderId);
        assertThat(txns).hasSize(1);
    }

    @Test
    @DisplayName("Security: Role-based authorization matrix enforcement for purchases")
    void testPurchaseSecurity_RbacMatrix() throws Exception {
        CreatePurchaseOrderRequestDto payload = new CreatePurchaseOrderRequestDto(
                testSupplier.getId(),
                LocalDate.now().plusDays(5),
                "RBAC test order",
                PurchaseOrderStatus.DRAFT,
                List.of(new PurchaseOrderItemRequestDto(testProduct.getId(), 5, BigDecimal.valueOf(10.00)))
        );
        String jsonPayload = objectMapper.writeValueAsString(payload);

        // 1. SALES_STAFF is denied access on GET -> 403
        mockMvc.perform(get("/api/v1/purchases")
                        .header("Authorization", getAuthToken(salesUser)))
                .andExpect(status().isForbidden());

        // 2. SALES_STAFF is denied access on POST -> 403
        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", getAuthToken(salesUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden());

        // 3. INVENTORY_MANAGER can read -> 200
        mockMvc.perform(get("/api/v1/purchases")
                        .header("Authorization", getAuthToken(inventoryUser)))
                .andExpect(status().isOk());

        // 4. INVENTORY_MANAGER cannot create PO -> 403
        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", getAuthToken(inventoryUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden());

        // 5. PURCHASE_MANAGER can create PO -> 201
        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", getAuthToken(purchaseUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated());
    }
}
