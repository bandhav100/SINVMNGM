package com.smartinventory.insight;

import com.smartinventory.category.entity.Category;
import com.smartinventory.category.repository.CategoryRepository;
import com.smartinventory.inventory.repository.InventoryTransactionRepository;
import com.smartinventory.inventory.repository.StockAdjustmentRepository;
import com.smartinventory.product.entity.Product;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.purchase.entity.PurchaseOrder;
import com.smartinventory.purchase.entity.PurchaseOrderItem;
import com.smartinventory.purchase.model.PurchaseOrderStatus;
import com.smartinventory.purchase.repository.PurchaseOrderItemRepository;
import com.smartinventory.purchase.repository.PurchaseOrderRepository;
import com.smartinventory.sale.entity.SalesOrder;
import com.smartinventory.sale.entity.SalesOrderItem;
import com.smartinventory.sale.model.SalesOrderStatus;
import com.smartinventory.sale.repository.SalesOrderItemRepository;
import com.smartinventory.sale.repository.SalesOrderRepository;
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
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class InsightAndAlertIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderItemRepository salesOrderItemRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private StockAdjustmentRepository stockAdjustmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    private User adminUser;
    private User inventoryManagerUser;
    private User purchaseManagerUser;
    private User salesStaffUser;

    private String adminToken;
    private String inventoryManagerToken;
    private String purchaseManagerToken;
    private String salesStaffToken;

    private Category testCategory;
    private Supplier testSupplier;
    private Product productInStock;
    private Product productLowStock;
    private Product productOutOfStock;
    private PurchaseOrder receivedPurchaseOrder;
    private SalesOrder completedSalesOrder;

    @BeforeEach
    void setUp() {
        tearDown();

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        adminUser = createUser("admin_" + suffix, Role.ADMIN);
        inventoryManagerUser = createUser("inv_" + suffix, Role.INVENTORY_MANAGER);
        purchaseManagerUser = createUser("purch_" + suffix, Role.PURCHASE_MANAGER);
        salesStaffUser = createUser("sales_" + suffix, Role.SALES_STAFF);

        adminToken = jwtService.generateToken(adminUser);
        inventoryManagerToken = jwtService.generateToken(inventoryManagerUser);
        purchaseManagerToken = jwtService.generateToken(purchaseManagerUser);
        salesStaffToken = jwtService.generateToken(salesStaffUser);

        testCategory = new Category();
        testCategory.setName("Category " + suffix);
        testCategory = categoryRepository.save(testCategory);

        testSupplier = new Supplier();
        testSupplier.setName("Supplier " + suffix);
        testSupplier.setEmail("supp_" + suffix + "@supplier.local");
        testSupplier.setActive(true);
        testSupplier = supplierRepository.save(testSupplier);

        // 1. In-stock product: 50 units, reorder 10
        productInStock = new Product();
        productInStock.setName("Product InStock " + suffix);
        productInStock.setSku("SKU-INS-" + suffix);
        productInStock.setPrice(new BigDecimal("100.00"));
        productInStock.setQuantity(50);
        productInStock.setReorderLevel(10);
        productInStock.setCategory(testCategory);
        productInStock = productRepository.save(productInStock);

        // 2. Low-stock product: 4 units, reorder 10
        productLowStock = new Product();
        productLowStock.setName("Product LowStock " + suffix);
        productLowStock.setSku("SKU-LOW-" + suffix);
        productLowStock.setPrice(new BigDecimal("40.00"));
        productLowStock.setQuantity(4);
        productLowStock.setReorderLevel(10);
        productLowStock.setCategory(testCategory);
        productLowStock = productRepository.save(productLowStock);

        // 3. Out-of-stock product: 0 units, reorder 15
        productOutOfStock = new Product();
        productOutOfStock.setName("Product OutOfStock " + suffix);
        productOutOfStock.setSku("SKU-OOS-" + suffix);
        productOutOfStock.setPrice(new BigDecimal("60.00"));
        productOutOfStock.setQuantity(0);
        productOutOfStock.setReorderLevel(15);
        productOutOfStock.setCategory(testCategory);
        productOutOfStock = productRepository.save(productOutOfStock);

        // Seed a RECEIVED purchase order (30 units received)
        receivedPurchaseOrder = new PurchaseOrder();
        receivedPurchaseOrder.setOrderNumber("PO-" + suffix);
        receivedPurchaseOrder.setSupplier(testSupplier);
        receivedPurchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
        receivedPurchaseOrder.setTotalAmount(new BigDecimal("1500.00"));
        receivedPurchaseOrder.setCreatedBy(adminUser);
        receivedPurchaseOrder.setReceivedBy(adminUser);
        receivedPurchaseOrder.setReceivedAt(Instant.now());

        PurchaseOrderItem poi = new PurchaseOrderItem();
        poi.setProduct(productInStock);
        poi.setQuantity(30);
        poi.setUnitCost(new BigDecimal("50.00"));
        poi.setTotalCost(new BigDecimal("1500.00"));
        receivedPurchaseOrder.addItem(poi);
        receivedPurchaseOrder = purchaseOrderRepository.save(receivedPurchaseOrder);

        // Seed a COMPLETED sales order (10 units sold)
        completedSalesOrder = new SalesOrder();
        completedSalesOrder.setOrderNumber("SO-" + suffix);
        completedSalesOrder.setStatus(SalesOrderStatus.COMPLETED);
        completedSalesOrder.setTotalAmount(new BigDecimal("1000.00"));
        completedSalesOrder.setCreatedBy(adminUser);
        completedSalesOrder.setCompletedBy(adminUser);
        completedSalesOrder.setCompletedAt(Instant.now());

        SalesOrderItem soi = new SalesOrderItem();
        soi.setProduct(productInStock);
        soi.setQuantity(10);
        soi.setUnitPrice(new BigDecimal("100.00"));
        soi.setLineTotal(new BigDecimal("1000.00"));
        completedSalesOrder.addItem(soi);
        completedSalesOrder = salesOrderRepository.save(completedSalesOrder);
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
        inventoryTransactionRepository.deleteAll();
        stockAdjustmentRepository.deleteAll();
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
    @DisplayName("GET /api/v1/insights/overview returns inventory counts and reports cost valuation unavailable")
    void testGetInventoryOverview_Success() throws Exception {
        mockMvc.perform(get("/api/v1/insights/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.outOfStockCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.lowStockCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.reorderRequiredCount").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.costValuationAvailable").value(false))
                .andExpect(jsonPath("$.valuationExplanation").isNotEmpty())
                .andExpect(jsonPath("$.totalPotentialRetailValue").value(greaterThan(0.0)));
    }

    @Test
    @DisplayName("GET /api/v1/insights/fast-moving-products returns sold items and excludes non-completed orders")
    void testGetFastMovingProducts_Success() throws Exception {
        mockMvc.perform(get("/api/v1/insights/fast-moving-products?days=30")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].productName").value(productInStock.getName()))
                .andExpect(jsonPath("$[0].unitsSold").value(10))
                .andExpect(jsonPath("$[0].totalRevenue").value(1000.00));
    }

    @Test
    @DisplayName("GET /api/v1/insights/slow-moving-products identifies NEVER_SOLD products")
    void testGetSlowMovingProducts_Success() throws Exception {
        mockMvc.perform(get("/api/v1/insights/slow-moving-products?days=30")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].classification", hasItem("NEVER_SOLD")));
    }

    @Test
    @DisplayName("GET /api/v1/insights/purchase-vs-sales compares received PO vs completed SO")
    void testGetPurchaseVsSales_Success() throws Exception {
        mockMvc.perform(get("/api/v1/insights/purchase-vs-sales?days=30")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchasedUnits").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.purchaseCostTotal").value(greaterThanOrEqualTo(1500.00)))
                .andExpect(jsonPath("$.soldUnits").value(greaterThanOrEqualTo(10)))
                .andExpect(jsonPath("$.salesRevenueTotal").value(greaterThanOrEqualTo(1000.00)))
                .andExpect(jsonPath("$.netUnitsDelta").value(greaterThanOrEqualTo(20)));
    }

    @Test
    @DisplayName("GET /api/v1/insights/reorder-recommendations recommends replenishments for breached thresholds")
    void testGetReorderRecommendations_Success() throws Exception {
        mockMvc.perform(get("/api/v1/insights/reorder-recommendations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].sku", hasItems(productLowStock.getSku(), productOutOfStock.getSku())));
    }

    @Test
    @DisplayName("GET /api/v1/alerts/summary and GET /api/v1/alerts returns classified low-stock alerts")
    void testAlerts_SummaryAndList() throws Exception {
        // 1. Summary
        mockMvc.perform(get("/api/v1/alerts/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAlerts").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.criticalCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.warningCount").value(greaterThanOrEqualTo(1)));

        // 2. Paginated list
        mockMvc.perform(get("/api/v1/alerts?size=20")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.content[*].severity", hasItems("CRITICAL", "WARNING")));
    }

    @Test
    @DisplayName("RBAC matrix: SALES_STAFF is forbidden from valuation and reorder recommendations, but allowed for overview, fast-moving, and alerts")
    void testRbacPermissions() throws Exception {
        // SALES_STAFF allowed overview
        mockMvc.perform(get("/api/v1/insights/overview")
                        .header("Authorization", "Bearer " + salesStaffToken))
                .andExpect(status().isOk());

        // SALES_STAFF allowed fast-moving
        mockMvc.perform(get("/api/v1/insights/fast-moving-products")
                        .header("Authorization", "Bearer " + salesStaffToken))
                .andExpect(status().isOk());

        // SALES_STAFF allowed alerts
        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + salesStaffToken))
                .andExpect(status().isOk());

        // SALES_STAFF forbidden from stock-valuation
        mockMvc.perform(get("/api/v1/insights/stock-valuation")
                        .header("Authorization", "Bearer " + salesStaffToken))
                .andExpect(status().isForbidden());

        // SALES_STAFF forbidden from reorder-recommendations
        mockMvc.perform(get("/api/v1/insights/reorder-recommendations")
                        .header("Authorization", "Bearer " + salesStaffToken))
                .andExpect(status().isForbidden());

        // INVENTORY_MANAGER allowed stock-valuation and recommendations
        mockMvc.perform(get("/api/v1/insights/stock-valuation")
                        .header("Authorization", "Bearer " + inventoryManagerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/insights/reorder-recommendations")
                        .header("Authorization", "Bearer " + inventoryManagerToken))
                .andExpect(status().isOk());

        // PURCHASE_MANAGER allowed purchase-vs-sales and recommendations
        mockMvc.perform(get("/api/v1/insights/purchase-vs-sales")
                        .header("Authorization", "Bearer " + purchaseManagerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/insights/reorder-recommendations")
                        .header("Authorization", "Bearer " + purchaseManagerToken))
                .andExpect(status().isOk());
    }
}
