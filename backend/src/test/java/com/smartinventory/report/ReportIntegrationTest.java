package com.smartinventory.report;

import com.smartinventory.category.entity.Category;
import com.smartinventory.category.repository.CategoryRepository;
import com.smartinventory.inventory.entity.InventoryTransaction;
import com.smartinventory.inventory.model.InventoryTransactionType;
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
class ReportIntegrationTest {

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
    private InventoryTransaction movementTx;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        adminUser = createUser("rep_adm_" + suffix, Role.ADMIN);
        inventoryManagerUser = createUser("rep_inv_" + suffix, Role.INVENTORY_MANAGER);
        purchaseManagerUser = createUser("rep_pur_" + suffix, Role.PURCHASE_MANAGER);
        salesStaffUser = createUser("rep_sal_" + suffix, Role.SALES_STAFF);

        adminToken = jwtService.generateToken(adminUser);
        inventoryManagerToken = jwtService.generateToken(inventoryManagerUser);
        purchaseManagerToken = jwtService.generateToken(purchaseManagerUser);
        salesStaffToken = jwtService.generateToken(salesStaffUser);

        testCategory = new Category();
        testCategory.setName("Report Cat " + suffix);
        testCategory = categoryRepository.save(testCategory);

        testSupplier = new Supplier();
        testSupplier.setName("Report Supplier " + suffix);
        testSupplier.setEmail("rep_sup_" + suffix + "@supplier.local");
        testSupplier.setActive(true);
        testSupplier = supplierRepository.save(testSupplier);

        // 1. In-stock product: 50 units, reorder 10, price 100
        productInStock = new Product();
        productInStock.setName("Product InStock " + suffix);
        productInStock.setSku("REP-INS-" + suffix);
        productInStock.setPrice(new BigDecimal("100.00"));
        productInStock.setQuantity(50);
        productInStock.setReorderLevel(10);
        productInStock.setCategory(testCategory);
        productInStock = productRepository.save(productInStock);

        // 2. Low-stock product: 4 units, reorder 10, price 40
        productLowStock = new Product();
        productLowStock.setName("Product LowStock " + suffix);
        productLowStock.setSku("REP-LOW-" + suffix);
        productLowStock.setPrice(new BigDecimal("40.00"));
        productLowStock.setQuantity(4);
        productLowStock.setReorderLevel(10);
        productLowStock.setCategory(testCategory);
        productLowStock = productRepository.save(productLowStock);

        // 3. Out-of-stock product: 0 units, reorder 15, price 60
        productOutOfStock = new Product();
        productOutOfStock.setName("Product OutOfStock " + suffix);
        productOutOfStock.setSku("REP-OOS-" + suffix);
        productOutOfStock.setPrice(new BigDecimal("60.00"));
        productOutOfStock.setQuantity(0);
        productOutOfStock.setReorderLevel(15);
        productOutOfStock.setCategory(testCategory);
        productOutOfStock = productRepository.save(productOutOfStock);

        // Seed a RECEIVED purchase order (30 units received, 1500 total cost)
        receivedPurchaseOrder = new PurchaseOrder();
        receivedPurchaseOrder.setOrderNumber("RPO-" + suffix);
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

        // Seed a COMPLETED sales order (10 units sold, 1000 revenue)
        completedSalesOrder = new SalesOrder();
        completedSalesOrder.setOrderNumber("RSO-" + suffix);
        completedSalesOrder.setStatus(SalesOrderStatus.COMPLETED);
        completedSalesOrder.setTotalAmount(new BigDecimal("1000.00"));
        completedSalesOrder.setCustomerName("Acme Customer");
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

        // Seed an inventory transaction
        movementTx = new InventoryTransaction();
        movementTx.setProduct(productInStock);
        movementTx.setTransactionType(InventoryTransactionType.PURCHASE_RECEIPT);
        movementTx.setQuantityDelta(30);
        movementTx.setQuantityBefore(20);
        movementTx.setQuantityAfter(50);
        movementTx.setReferenceType("PURCHASE_ORDER");
        movementTx.setReferenceId(receivedPurchaseOrder.getId());
        movementTx.setPerformedBy(adminUser);
        movementTx.setNotes("Initial stock receipt for report test");
        movementTx = inventoryTransactionRepository.save(movementTx);
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
    @DisplayName("GET /api/v1/reports/inventory returns summary totals and paginated items")
    void testInventoryReportEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/reports/inventory")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("categoryId", testCategory.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalProducts", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.summary.totalStockUnits", greaterThanOrEqualTo(54)))
                .andExpect(jsonPath("$.summary.outOfStockCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.summary.lowStockCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.summary.inStockCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.items.content", hasSize(greaterThanOrEqualTo(3))));
    }

    @Test
    @DisplayName("GET /api/v1/reports/sales returns completed sales metrics, top selling products, and orders")
    void testSalesReportEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/reports/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("days", "30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.completedOrdersCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.summary.totalUnitsSold", greaterThanOrEqualTo(10)))
                .andExpect(jsonPath("$.summary.totalRevenue", greaterThanOrEqualTo(1000.00)))
                .andExpect(jsonPath("$.topSellingProducts", not(empty())))
                .andExpect(jsonPath("$.items.content", not(empty())));
    }

    @Test
    @DisplayName("GET /api/v1/reports/purchases returns status grouping, received costs, and supplier summaries")
    void testPurchaseReportEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/reports/purchases")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("days", "30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.receivedOrdersCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.summary.receivedUnits", greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.summary.receivedTotalCost", greaterThanOrEqualTo(1500.00)))
                .andExpect(jsonPath("$.supplierSummaries", not(empty())))
                .andExpect(jsonPath("$.items.content", not(empty())));
    }

    @Test
    @DisplayName("GET /api/v1/reports/inventory-movements returns ledger movements, delta, and actor")
    void testInventoryMovementReportEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/reports/inventory-movements")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("productId", productInStock.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalMovements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.summary.totalInboundUnits", greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.items.content", not(empty())))
                .andExpect(jsonPath("$.items.content[0].transactionType", is("PURCHASE_RECEIPT")))
                .andExpect(jsonPath("$.items.content[0].performedBy", containsString("FullName")));
    }

    @Test
    @DisplayName("GET /api/v1/reports/stock-valuation reports costValuationAvailable false with clear explanation")
    void testStockValuationReportEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/reports/stock-valuation")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costValuationAvailable", is(false)))
                .andExpect(jsonPath("$.explanation", containsString("Reliable per-unit purchase cost data is not currently maintained")))
                .andExpect(jsonPath("$.summary.totalActiveProducts", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.items.content", not(empty())));
    }

    @Test
    @DisplayName("GET /api/v1/reports/sales rejects startDate > endDate with 400 Bad Request")
    void testInvalidDateRangeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/reports/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("startDate", "2026-12-31")
                        .param("endDate", "2026-01-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RBAC matrix restricts unauthorized roles while permitting authorized roles")
    void testRbacPermissionsMatrix() throws Exception {
        // SALES_STAFF: allowed on inventory and sales; forbidden on purchases, movements, valuation
        mockMvc.perform(get("/api/v1/reports/inventory").header("Authorization", "Bearer " + salesStaffToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/sales").header("Authorization", "Bearer " + salesStaffToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/purchases").header("Authorization", "Bearer " + salesStaffToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reports/inventory-movements").header("Authorization", "Bearer " + salesStaffToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reports/stock-valuation").header("Authorization", "Bearer " + salesStaffToken))
                .andExpect(status().isForbidden());

        // PURCHASE_MANAGER: allowed on inventory, purchases, movements, valuation; forbidden on sales
        mockMvc.perform(get("/api/v1/reports/inventory").header("Authorization", "Bearer " + purchaseManagerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/purchases").header("Authorization", "Bearer " + purchaseManagerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/inventory-movements").header("Authorization", "Bearer " + purchaseManagerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/stock-valuation").header("Authorization", "Bearer " + purchaseManagerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/sales").header("Authorization", "Bearer " + purchaseManagerToken))
                .andExpect(status().isForbidden());

        // INVENTORY_MANAGER: allowed on all 5 reports
        mockMvc.perform(get("/api/v1/reports/inventory").header("Authorization", "Bearer " + inventoryManagerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/sales").header("Authorization", "Bearer " + inventoryManagerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/purchases").header("Authorization", "Bearer " + inventoryManagerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/inventory-movements").header("Authorization", "Bearer " + inventoryManagerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/stock-valuation").header("Authorization", "Bearer " + inventoryManagerToken))
                .andExpect(status().isOk());
    }
}
