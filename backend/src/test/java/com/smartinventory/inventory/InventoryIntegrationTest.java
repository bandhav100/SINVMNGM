package com.smartinventory.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.category.entity.Category;
import com.smartinventory.category.repository.CategoryRepository;
import com.smartinventory.inventory.dto.StockAdjustmentRequestDto;
import com.smartinventory.inventory.entity.InventoryTransaction;
import com.smartinventory.inventory.entity.StockAdjustment;
import com.smartinventory.inventory.model.InventoryTransactionType;
import com.smartinventory.inventory.repository.InventoryTransactionRepository;
import com.smartinventory.inventory.repository.StockAdjustmentRepository;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.purchase.repository.PurchaseOrderItemRepository;
import com.smartinventory.purchase.repository.PurchaseOrderRepository;
import com.smartinventory.sale.repository.SalesOrderItemRepository;
import com.smartinventory.sale.repository.SalesOrderRepository;
import com.smartinventory.security.jwt.JwtService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class InventoryIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockAdjustmentRepository stockAdjustmentRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderItemRepository salesOrderItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private User adminUser;
    private User inventoryManagerUser;
    private User salesStaffUser;
    private String adminToken;
    private String inventoryManagerToken;
    private String salesStaffToken;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        tearDown();

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        adminUser = createUser("inv_adm_" + suffix, Role.ADMIN);
        inventoryManagerUser = createUser("inv_mgr_" + suffix, Role.INVENTORY_MANAGER);
        salesStaffUser = createUser("inv_stf_" + suffix, Role.SALES_STAFF);

        adminToken = jwtService.generateToken(adminUser);
        inventoryManagerToken = jwtService.generateToken(inventoryManagerUser);
        salesStaffToken = jwtService.generateToken(salesStaffUser);

        Category cat = new Category();
        cat.setName("Inv Cat " + suffix);
        cat = categoryRepository.save(cat);

        testProduct = new Product();
        testProduct.setName("Inv Test Product " + suffix);
        testProduct.setSku("INV-SKU-" + suffix);
        testProduct.setPrice(new BigDecimal("25.00"));
        testProduct.setQuantity(20);
        testProduct.setReorderLevel(10);
        testProduct.setCategory(cat);
        testProduct = productRepository.save(testProduct);
    }

    private User createUser(String username, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@sims.local");
        u.setPasswordHash(passwordEncoder.encode("Password@123"));
        u.setFullName(username + " FullName");
        u.setRole(role);
        u.setActive(true);
        return userRepository.save(u);
    }

    @AfterEach
    void tearDown() {
        stockAdjustmentRepository.deleteAll();
        inventoryTransactionRepository.deleteAll();
        salesOrderItemRepository.deleteAll();
        salesOrderRepository.deleteAll();
        purchaseOrderItemRepository.deleteAll();
        purchaseOrderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        supplierRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/inventory/adjustments increments stock and writes both audit and ledger records")
    void testAdjustStock_Admin_PositiveDelta_Success() throws Exception {
        StockAdjustmentRequestDto request = new StockAdjustmentRequestDto(
                testProduct.getId(), 15, "Annual Physical Count Audit", "Found unopened carton"
        );

        mockMvc.perform(post("/api/v1/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.productId").value(testProduct.getId()))
                .andExpect(jsonPath("$.quantityDelta").value(15))
                .andExpect(jsonPath("$.quantityBefore").value(20))
                .andExpect(jsonPath("$.quantityAfter").value(35))
                .andExpect(jsonPath("$.reason").value("Annual Physical Count Audit"))
                .andExpect(jsonPath("$.adjustedByUsername").value(adminUser.getUsername()));

        // Verify product in DB
        Product updated = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(35, updated.getQuantity());

        // Verify stock adjustment record in DB
        List<StockAdjustment> adjustments = stockAdjustmentRepository.findAll();
        assertEquals(1, adjustments.size());
        assertEquals(15, adjustments.get(0).getQuantityDelta());

        // Verify inventory transaction ledger record in DB
        List<InventoryTransaction> transactions = inventoryTransactionRepository.findAll();
        assertEquals(1, transactions.size());
        assertEquals(InventoryTransactionType.MANUAL_ADJUSTMENT, transactions.get(0).getTransactionType());
        assertEquals(15, transactions.get(0).getQuantityDelta());
        assertEquals(20, transactions.get(0).getQuantityBefore());
        assertEquals(35, transactions.get(0).getQuantityAfter());
        assertEquals("STOCK_ADJUSTMENT", transactions.get(0).getReferenceType());
        assertEquals(adjustments.get(0).getId(), transactions.get(0).getReferenceId());
    }

    @Test
    @DisplayName("POST /api/v1/inventory/adjustments decrements stock safely for INVENTORY_MANAGER")
    void testAdjustStock_InventoryManager_NegativeDelta_Success() throws Exception {
        StockAdjustmentRequestDto request = new StockAdjustmentRequestDto(
                testProduct.getId(), -5, "Damaged stock disposed", "Water leak damage"
        );

        mockMvc.perform(post("/api/v1/inventory/adjustments")
                        .header("Authorization", "Bearer " + inventoryManagerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantityDelta").value(-5))
                .andExpect(jsonPath("$.quantityBefore").value(20))
                .andExpect(jsonPath("$.quantityAfter").value(15));

        Product updated = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(15, updated.getQuantity());
    }

    @Test
    @DisplayName("POST /api/v1/inventory/adjustments returns 403 Forbidden for SALES_STAFF")
    void testAdjustStock_SalesStaff_Forbidden() throws Exception {
        StockAdjustmentRequestDto request = new StockAdjustmentRequestDto(
                testProduct.getId(), 5, "Staff unauthorized adjustment", null
        );

        mockMvc.perform(post("/api/v1/inventory/adjustments")
                        .header("Authorization", "Bearer " + salesStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/inventory/adjustments returns 400 Bad Request when delta exceeds available stock")
    void testAdjustStock_NegativeStock_Rejection() throws Exception {
        StockAdjustmentRequestDto request = new StockAdjustmentRequestDto(
                testProduct.getId(), -50, "Exceeding adjustment", null
        );

        mockMvc.perform(post("/api/v1/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Insufficient stock")));

        // Verify product was not changed
        Product unchanged = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(20, unchanged.getQuantity());
    }

    @Test
    @DisplayName("POST /api/v1/inventory/adjustments returns 400 Bad Request when mandatory reason is missing")
    void testAdjustStock_MissingReason_ValidationFailed() throws Exception {
        StockAdjustmentRequestDto request = new StockAdjustmentRequestDto(
                testProduct.getId(), 5, "", null
        );

        mockMvc.perform(post("/api/v1/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.reason").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/inventory/stock returns paginated stock items with reorder status")
    void testGetInventoryStock_Success() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].sku").value(testProduct.getSku()))
                .andExpect(jsonPath("$.content[0].stockStatus").value("IN_STOCK"));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/summary returns live overview counts")
    void testGetInventorySummary_Success() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalStockUnits").value(greaterThanOrEqualTo(20)));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/transactions returns ledger items after adjustment")
    void testGetTransactions_Success() throws Exception {
        // First make an adjustment
        StockAdjustmentRequestDto request = new StockAdjustmentRequestDto(
                testProduct.getId(), 5, "Audit count", null
        );
        mockMvc.perform(post("/api/v1/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/inventory/transactions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].transactionType").value("MANUAL_ADJUSTMENT"))
                .andExpect(jsonPath("$.content[0].referenceType").value("STOCK_ADJUSTMENT"));
    }
}
