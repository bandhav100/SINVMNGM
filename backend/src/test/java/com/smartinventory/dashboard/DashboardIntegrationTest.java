package com.smartinventory.dashboard;

import com.smartinventory.category.entity.Category;
import com.smartinventory.category.repository.CategoryRepository;
import com.smartinventory.inventory.entity.InventoryTransaction;
import com.smartinventory.inventory.model.InventoryTransactionType;
import com.smartinventory.inventory.repository.InventoryTransactionRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class DashboardIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    private User adminUser;
    private User salesStaffUser;
    private String adminToken;
    private String salesStaffToken;

    @BeforeEach
    void setUp() {
        tearDown();

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        adminUser = createUser("dash_adm_" + suffix, Role.ADMIN);
        salesStaffUser = createUser("dash_sal_" + suffix, Role.SALES_STAFF);

        adminToken = jwtService.generateToken(adminUser);
        salesStaffToken = jwtService.generateToken(salesStaffUser);

        Category cat = new Category();
        cat.setName("Dash Cat " + suffix);
        cat = categoryRepository.save(cat);

        Supplier sup = new Supplier();
        sup.setName("Dash Sup " + suffix);
        sup.setEmail("dash_sup_" + suffix + "@supplier.local");
        sup.setActive(true);
        sup = supplierRepository.save(sup);

        Product prod = new Product();
        prod.setName("Dash Product " + suffix);
        prod.setSku("DASH-SKU-" + suffix);
        prod.setPrice(new BigDecimal("150.00"));
        prod.setQuantity(5);
        prod.setReorderLevel(10);
        prod.setCategory(cat);
        prod = productRepository.save(prod);

        // Seed received purchase order
        PurchaseOrder po = new PurchaseOrder();
        po.setOrderNumber("PO-DASH-" + suffix);
        po.setSupplier(sup);
        po.setStatus(PurchaseOrderStatus.RECEIVED);
        po.setTotalAmount(new BigDecimal("500.00"));
        po.setCreatedBy(adminUser);
        po.setReceivedBy(adminUser);
        po.setReceivedAt(Instant.now());
        PurchaseOrderItem poi = new PurchaseOrderItem();
        poi.setProduct(prod);
        poi.setQuantity(10);
        poi.setUnitCost(new BigDecimal("50.00"));
        poi.setTotalCost(new BigDecimal("500.00"));
        po.addItem(poi);
        purchaseOrderRepository.save(po);

        // Seed completed sales order
        SalesOrder so = new SalesOrder();
        so.setOrderNumber("SO-DASH-" + suffix);
        so.setStatus(SalesOrderStatus.COMPLETED);
        so.setTotalAmount(new BigDecimal("300.00"));
        so.setCreatedBy(adminUser);
        so.setCompletedBy(adminUser);
        so.setCompletedAt(Instant.now());
        SalesOrderItem soi = new SalesOrderItem();
        sooi(so, prod, 2, new BigDecimal("150.00"));
        salesOrderRepository.save(so);

        // Seed inventory transaction
        InventoryTransaction tx = new InventoryTransaction();
        tx.setProduct(prod);
        tx.setTransactionType(InventoryTransactionType.SALE_DEDUCTION);
        tx.setQuantityDelta(-2);
        tx.setQuantityBefore(7);
        tx.setQuantityAfter(5);
        tx.setReferenceType("SALES_ORDER");
        tx.setReferenceId(so.getId());
        tx.setNotes("Sales deduction");
        tx.setPerformedBy(adminUser);
        inventoryTransactionRepository.save(tx);
    }

    private void sooi(SalesOrder so, Product prod, int qty, BigDecimal price) {
        SalesOrderItem soi = new SalesOrderItem();
        soi.setProduct(prod);
        soi.setQuantity(qty);
        soi.setUnitPrice(price);
        soi.setLineTotal(price.multiply(BigDecimal.valueOf(qty)));
        so.addItem(soi);
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
    @DisplayName("GET /api/v1/dashboard/summary returns full KPIs and recent activity for ADMIN")
    void testGetDashboardSummary_Admin_Success() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalStockUnits").value(greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.lowStockCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.completedSalesCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.completedSalesRevenue").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.purchaseMetricsVisible").value(true))
                .andExpect(jsonPath("$.receivedPurchasesCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.receivedPurchasesTotalCost").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.criticalAlerts", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.recentActivity", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/summary masks purchase metrics for SALES_STAFF")
    void testGetDashboardSummary_SalesStaff_Success() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + salesStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.purchaseMetricsVisible").value(false))
                .andExpect(jsonPath("$.totalPurchasesCount").value(0))
                .andExpect(jsonPath("$.receivedPurchasesTotalCost").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/summary returns 401 when unauthenticated")
    void testGetDashboardSummary_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }
}
