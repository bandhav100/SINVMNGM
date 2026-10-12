package com.smartinventory.sale;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.category.entity.Category;
import com.smartinventory.category.repository.CategoryRepository;
import com.smartinventory.inventory.entity.InventoryTransaction;
import com.smartinventory.inventory.model.InventoryTransactionType;
import com.smartinventory.inventory.repository.InventoryTransactionRepository;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.sale.dto.CreateSalesOrderRequestDto;
import com.smartinventory.sale.dto.SalesOrderItemRequestDto;
import com.smartinventory.sale.dto.SalesOrderResponseDto;
import com.smartinventory.sale.dto.UpdateSalesOrderRequestDto;
import com.smartinventory.sale.model.SalesOrderStatus;
import com.smartinventory.sale.repository.SalesOrderItemRepository;
import com.smartinventory.sale.repository.SalesOrderRepository;
import com.smartinventory.sale.service.SaleService;
import com.smartinventory.security.jwt.JwtService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SaleIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SaleService saleService;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderItemRepository salesOrderItemRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private User salesStaffUser;
    private User purchaseManagerUser;
    private User inventoryManagerUser;
    private String salesStaffToken;
    private String purchaseManagerToken;
    private String inventoryManagerToken;
    private Category testCategory;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        salesStaffUser = new User();
        salesStaffUser.setUsername("seller_" + suffix);
        salesStaffUser.setEmail("seller_" + suffix + "@sims.local");
        salesStaffUser.setPasswordHash(passwordEncoder.encode("Password@123"));
        salesStaffUser.setFullName("Sales Staff " + suffix);
        salesStaffUser.setRole(Role.SALES_STAFF);
        salesStaffUser.setActive(true);
        salesStaffUser = userRepository.save(salesStaffUser);
        salesStaffToken = jwtService.generateToken(salesStaffUser);

        purchaseManagerUser = new User();
        purchaseManagerUser.setUsername("purchaser_" + suffix);
        purchaseManagerUser.setEmail("purchaser_" + suffix + "@sims.local");
        purchaseManagerUser.setPasswordHash(passwordEncoder.encode("Password@123"));
        purchaseManagerUser.setFullName("Purchase Manager " + suffix);
        purchaseManagerUser.setRole(Role.PURCHASE_MANAGER);
        purchaseManagerUser.setActive(true);
        purchaseManagerUser = userRepository.save(purchaseManagerUser);
        purchaseManagerToken = jwtService.generateToken(purchaseManagerUser);

        inventoryManagerUser = new User();
        inventoryManagerUser.setUsername("invmgr_" + suffix);
        inventoryManagerUser.setEmail("invmgr_" + suffix + "@sims.local");
        inventoryManagerUser.setPasswordHash(passwordEncoder.encode("Password@123"));
        inventoryManagerUser.setFullName("Inventory Manager " + suffix);
        inventoryManagerUser.setRole(Role.INVENTORY_MANAGER);
        inventoryManagerUser.setActive(true);
        inventoryManagerUser = userRepository.save(inventoryManagerUser);
        inventoryManagerToken = jwtService.generateToken(inventoryManagerUser);

        testCategory = new Category();
        testCategory.setName("Sales Cat " + suffix);
        testCategory = categoryRepository.save(testCategory);

        testProduct = new Product();
        testProduct.setName("Sales Product " + suffix);
        testProduct.setSku("SKU-SALE-" + suffix);
        testProduct.setPrice(new BigDecimal("50.00"));
        testProduct.setQuantity(20);
        testProduct.setReorderLevel(5);
        testProduct.setCategory(testCategory);
        testProduct = productRepository.save(testProduct);
    }

    @AfterEach
    void tearDown() {
        inventoryTransactionRepository.deleteAll();
        salesOrderItemRepository.deleteAll();
        salesOrderRepository.deleteAll();
        if (testProduct != null && testProduct.getId() != null) {
            productRepository.deleteById(testProduct.getId());
        }
        if (testCategory != null && testCategory.getId() != null) {
            categoryRepository.deleteById(testCategory.getId());
        }
        if (salesStaffUser != null && salesStaffUser.getId() != null) {
            userRepository.deleteById(salesStaffUser.getId());
        }
        if (purchaseManagerUser != null && purchaseManagerUser.getId() != null) {
            userRepository.deleteById(purchaseManagerUser.getId());
        }
        if (inventoryManagerUser != null && inventoryManagerUser.getId() != null) {
            userRepository.deleteById(inventoryManagerUser.getId());
        }
    }

    @Test
    @DisplayName("End-to-End: Create draft sale, complete sale, verify stock deduction, ledger record, and reject duplicate completion")
    void testSaleFullLifecycle_StockDeductionAndLedgerVerification() throws Exception {
        // 1. Create Draft Sale
        CreateSalesOrderRequestDto request = new CreateSalesOrderRequestDto(
                "Acme Corp",
                "+1-555-4321",
                "Customer pickup",
                List.of(new SalesOrderItemRequestDto(testProduct.getId(), 5, new BigDecimal("48.00")))
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", "Bearer " + salesStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(240.00))
                .andReturn();

        SalesOrderResponseDto created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                SalesOrderResponseDto.class
        );
        Long saleId = created.id();

        // Verify stock is untouched while in DRAFT
        Product prodBefore = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(prodBefore.getQuantity()).isEqualTo(20);

        // 1b. Update Draft Sale (change quantity from 5 to 8)
        UpdateSalesOrderRequestDto updateRequest = new UpdateSalesOrderRequestDto(
                "Acme Corp Updated",
                "+1-555-9999",
                "Updated notes",
                List.of(new SalesOrderItemRequestDto(testProduct.getId(), 8, new BigDecimal("48.00")))
        );

        mockMvc.perform(put("/api/v1/sales/" + saleId)
                        .header("Authorization", "Bearer " + salesStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Acme Corp Updated"))
                .andExpect(jsonPath("$.totalAmount").value(384.00))
                .andExpect(jsonPath("$.items[0].quantity").value(8));

        // 2. Complete the Sale
        mockMvc.perform(post("/api/v1/sales/" + saleId + "/complete")
                        .header("Authorization", "Bearer " + salesStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        // 3. Verify stock accurately deducted in database (20 - 8 = 12)
        Product prodAfter = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(prodAfter.getQuantity()).isEqualTo(12);

        // 4. Verify inventory ledger entry
        List<InventoryTransaction> txns = inventoryTransactionRepository
                .findByReferenceTypeAndReferenceId("SALES_ORDER", saleId);
        assertThat(txns).hasSize(1);
        InventoryTransaction txn = txns.get(0);
        assertThat(txn.getTransactionType()).isEqualTo(InventoryTransactionType.SALE_DEDUCTION);
        assertThat(txn.getQuantityDelta()).isEqualTo(-8);
        assertThat(txn.getQuantityBefore()).isEqualTo(20);
        assertThat(txn.getQuantityAfter()).isEqualTo(12);
        assertThat(txn.getProduct().getId()).isEqualTo(testProduct.getId());

        // 5. Duplicate completion attempt must be rejected with 400 Bad Request
        mockMvc.perform(post("/api/v1/sales/" + saleId + "/complete")
                        .header("Authorization", "Bearer " + salesStaffToken))
                .andExpect(status().isBadRequest());

        // Stock must remain 12, no additional ledger entry
        Product prodAfterDuplicate = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(prodAfterDuplicate.getQuantity()).isEqualTo(12);
        assertThat(inventoryTransactionRepository.findByReferenceTypeAndReferenceId("SALES_ORDER", saleId)).hasSize(1);
    }

    @Test
    @DisplayName("Complete sale with insufficient stock is rejected with 400 and rolls back without touching stock")
    void testInsufficientStock_RejectedAndRolledBack() throws Exception {
        // Create draft sale requesting 25 units (available: 20)
        CreateSalesOrderRequestDto request = new CreateSalesOrderRequestDto(
                "Big Buyer",
                null,
                null,
                List.of(new SalesOrderItemRequestDto(testProduct.getId(), 25, new BigDecimal("50.00")))
        );

        SalesOrderResponseDto draft = saleService.createSaleOrder(request, salesStaffUser.getUsername());

        // Attempt complete
        mockMvc.perform(post("/api/v1/sales/" + draft.id() + "/complete")
                        .header("Authorization", "Bearer " + salesStaffToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient stock")));

        // Stock remains 20
        Product product = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(product.getQuantity()).isEqualTo(20);

        // No ledger records
        assertThat(inventoryTransactionRepository.findByReferenceTypeAndReferenceId("SALES_ORDER", draft.id())).isEmpty();
    }

    @Test
    @DisplayName("Concurrency: Two sales competing for limited stock - exactly one succeeds and no overselling occurs")
    void testConcurrentSalesCompletion_CompetingForStock() throws Exception {
        // Set stock to exactly 10
        testProduct.setQuantity(10);
        testProduct = productRepository.save(testProduct);

        // Create two draft sales orders, each requesting 7 units (7 + 7 = 14 > 10)
        CreateSalesOrderRequestDto req1 = new CreateSalesOrderRequestDto("Buyer 1", null, null,
                List.of(new SalesOrderItemRequestDto(testProduct.getId(), 7, new BigDecimal("50.00"))));
        CreateSalesOrderRequestDto req2 = new CreateSalesOrderRequestDto("Buyer 2", null, null,
                List.of(new SalesOrderItemRequestDto(testProduct.getId(), 7, new BigDecimal("50.00"))));

        SalesOrderResponseDto order1 = saleService.createSaleOrder(req1, salesStaffUser.getUsername());
        SalesOrderResponseDto order2 = saleService.createSaleOrder(req2, salesStaffUser.getUsername());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Long[] orderIds = new Long[]{order1.id(), order2.id()};

        for (Long orderId : orderIds) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    saleService.completeSaleOrder(orderId, salesStaffUser.getUsername());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release threads simultaneously
        startLatch.countDown();
        boolean completedInTime = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completedInTime).isTrue();
        // Exactly one sale succeeds and one sale fails with insufficient stock
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        // Final stock must be exactly 3 (10 - 7 = 3)
        Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(finalProduct.getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("RBAC: SALES_STAFF can create sales, PURCHASE_MANAGER is forbidden (403), INVENTORY_MANAGER is read-only")
    void testRbac_PermissionsEnforced() throws Exception {
        CreateSalesOrderRequestDto request = new CreateSalesOrderRequestDto(
                "Customer",
                null,
                null,
                List.of(new SalesOrderItemRequestDto(testProduct.getId(), 1, new BigDecimal("50.00")))
        );

        // 1. SALES_STAFF can create
        mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", "Bearer " + salesStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 2. PURCHASE_MANAGER cannot create (403)
        mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", "Bearer " + purchaseManagerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // 3. INVENTORY_MANAGER can list/read (200)
        mockMvc.perform(get("/api/v1/sales")
                        .header("Authorization", "Bearer " + inventoryManagerToken))
                .andExpect(status().isOk());

        // 4. INVENTORY_MANAGER cannot create (403)
        mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", "Bearer " + inventoryManagerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
