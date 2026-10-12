package com.smartinventory.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.auth.dto.LoginRequestDto;
import com.smartinventory.auth.dto.SetupAdminRequestDto;
import com.smartinventory.common.exception.LastActiveAdminException;
import com.smartinventory.security.jwt.JwtService;
import com.smartinventory.user.dto.CreateUserRequestDto;
import com.smartinventory.user.dto.UpdateUserRequestDto;
import com.smartinventory.user.entity.User;
import com.smartinventory.user.model.Role;
import com.smartinventory.user.repository.UserRepository;
import com.smartinventory.user.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private User createTestUser(String username, String email, Role role, boolean active) {
        User user = new User();
        user.setUsername(username.toLowerCase());
        user.setEmail(email.toLowerCase());
        user.setFullName("Test " + username);
        user.setPasswordHash(passwordEncoder.encode("Password@123"));
        user.setRole(role);
        user.setActive(active);
        return userRepository.save(user);
    }

    private String getBearerToken(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    @Test
    @DisplayName("Initial admin setup creates only ADMIN, and permanently rejects further setup attempts")
    void testInitialAdminSetup_lifecycle() throws Exception {
        // 1. Initial status: setup required
        mockMvc.perform(get("/api/v1/auth/setup-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(true));

        // 2. Perform setup
        SetupAdminRequestDto setupDto = new SetupAdminRequestDto(
                "firstadmin", "admin@sims.local", "Initial Administrator", "Admin@12345"
        );

        mockMvc.perform(post("/api/v1/auth/setup-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setupDto)))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("jwt"))
                .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
                .andExpect(jsonPath("$.user.username").value("firstadmin"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));

        // 3. Status now indicates setup completed
        mockMvc.perform(get("/api/v1/auth/setup-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(false));

        // 4. Repeated setup attempt returns 409 Conflict
        SetupAdminRequestDto repeatedDto = new SetupAdminRequestDto(
                "secondadmin", "second@sims.local", "Intruder Admin", "Intruder@12345"
        );

        mockMvc.perform(post("/api/v1/auth/setup-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(repeatedDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already been completed")));

        // Only 1 admin in repository
        assertThat(userRepository.countByRole(Role.ADMIN)).isEqualTo(1);
    }

    @Test
    @DisplayName("Concurrent setup attempts are race-safe: exactly one succeeds and others return 409 Conflict")
    void testConcurrentAdminSetup_raceSafe() throws Exception {
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                startLatch.await();
                SetupAdminRequestDto dto = new SetupAdminRequestDto(
                        "admin" + index, "admin" + index + "@sims.local", "Admin " + index, "Admin@12345"
                );
                MvcResult result = mockMvc.perform(post("/api/v1/auth/setup-admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andReturn();

                int status = result.getResponse().getStatus();
                if (status == 201) {
                    successCount.incrementAndGet();
                } else if (status == 409) {
                    conflictCount.incrementAndGet();
                }
                return null;
            }));
        }

        // Trigger simultaneous execution
        startLatch.countDown();
        for (Future<Void> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(threads - 1);
        assertThat(userRepository.countByRole(Role.ADMIN)).isEqualTo(1);
    }

    @Test
    @DisplayName("Login fails on bad credentials and rejects disabled accounts")
    void testLogin_failureAndDisabled() throws Exception {
        User activeUser = createTestUser("active_staff", "active@sims.local", Role.INVENTORY_MANAGER, true);
        User disabledUser = createTestUser("disabled_staff", "disabled@sims.local", Role.SALES_STAFF, false);

        // 1. Invalid password -> 401 Unauthorized
        LoginRequestDto badPass = new LoginRequestDto("active_staff", "WrongPassword");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badPass)))
                .andExpect(status().isUnauthorized());

        // 2. Non-existent username -> 401 Unauthorized
        LoginRequestDto nonExistent = new LoginRequestDto("ghost_user", "AnyPassword");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistent)))
                .andExpect(status().isUnauthorized());

        // 3. Disabled account -> 403 Forbidden
        LoginRequestDto disabledLogin = new LoginRequestDto("disabled_staff", "Password@123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disabledLogin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("deactivated")));

        // 4. Valid credentials -> 200 OK with HttpOnly cookie
        LoginRequestDto validLogin = new LoginRequestDto("active_staff", "Password@123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt"))
                .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
                .andExpect(jsonPath("$.token").doesNotExist()) // Concealed in body for browsers
                .andExpect(jsonPath("$.user.username").value("active_staff"));
    }

    @Test
    @DisplayName("Invalid, expired, and revoked tokens after logout return 401 Unauthorized")
    void testTokenValidationAndLogout() throws Exception {
        User admin = createTestUser("sysadmin", "sysadmin@sims.local", Role.ADMIN, true);
        String token = jwtService.generateToken(admin);

        // 1. Valid token -> 200 OK on /me
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("sysadmin"));

        // 2. Malformed token -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer malformed.token.here"))
                .andExpect(status().isUnauthorized());

        // 3. Logout with token -> 200 OK and cookie cleared
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(cookie().value("jwt", ""));

        // 4. Reuse revoked token -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CSRF protection: cookie-based POST without CSRF is rejected, Bearer token is exempted")
    void testCsrfProtection_cookieVsBearer() throws Exception {
        User admin = createTestUser("csrf_admin", "csrf@sims.local", Role.ADMIN, true);
        String token = jwtService.generateToken(admin);

        CreateUserRequestDto newUser = new CreateUserRequestDto(
                "newguy", "newguy@sims.local", "New Guy", "Password@123", Role.SALES_STAFF
        );

        // 1. Cookie authentication WITHOUT CSRF header on state-changing POST -> 403 Forbidden
        mockMvc.perform(post("/api/v1/users")
                        .cookie(new Cookie("jwt", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isForbidden());

        // 2. Authorization Bearer header -> CSRF exempt -> 201 Created
        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newguy"));
    }

    @Test
    @DisplayName("Permission Matrix: Enforces strict access across all four roles")
    void testFourRolesPermissionMatrix() throws Exception {
        User admin = createTestUser("admin_role", "admin_role@sims.local", Role.ADMIN, true);
        User invManager = createTestUser("inv_mgr", "inv_mgr@sims.local", Role.INVENTORY_MANAGER, true);
        User purchManager = createTestUser("purch_mgr", "purch_mgr@sims.local", Role.PURCHASE_MANAGER, true);
        User salesStaff = createTestUser("sales_rep", "sales_rep@sims.local", Role.SALES_STAFF, true);

        String adminToken = getBearerToken(admin);
        String invToken = getBearerToken(invManager);
        String purchToken = getBearerToken(purchManager);
        String salesToken = getBearerToken(salesStaff);

        // 1. User Management (/api/v1/users) -> Admin only
        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, invToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, purchToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, salesToken))
                .andExpect(status().isForbidden());

        // 2. Products (/api/v1/products)
        // GET -> All 4 roles allowed
        mockMvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, adminToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, invToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, purchToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, salesToken)).andExpect(status().isOk());

        // POST -> Admin and INVENTORY_MANAGER only
        Map<String, Object> productPayload = Map.of(
                "name", "Test Item", "sku", "RBAC-SKU-1", "price", 10.0, "quantity", 5, "reorderLevel", 2
        );
        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, purchToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPayload)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, salesToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPayload)))
                .andExpect(status().isForbidden());

        // 3. Suppliers (/api/v1/suppliers)
        // GET -> Admin, INVENTORY_MANAGER, PURCHASE_MANAGER (SALES_STAFF is forbidden)
        mockMvc.perform(get("/api/v1/suppliers").header(HttpHeaders.AUTHORIZATION, adminToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/suppliers").header(HttpHeaders.AUTHORIZATION, invToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/suppliers").header(HttpHeaders.AUTHORIZATION, purchToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/suppliers").header(HttpHeaders.AUTHORIZATION, salesToken)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Last Active Admin Protection: Prevents self-demotion, self-deactivation, and demoting the only admin")
    void testLastActiveAdminProtection() throws Exception {
        User admin1 = createTestUser("admin_sole", "admin_sole@sims.local", Role.ADMIN, true);
        String admin1Token = getBearerToken(admin1);

        // 1. Admin attempts to self-demote -> 400 Bad Request
        UpdateUserRequestDto selfDemoteDto = new UpdateUserRequestDto(
                admin1.getEmail(), admin1.getFullName(), Role.INVENTORY_MANAGER, null
        );
        mockMvc.perform(put("/api/v1/users/" + admin1.getId())
                        .header(HttpHeaders.AUTHORIZATION, admin1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfDemoteDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cannot change their own role")));

        // 2. Admin attempts to self-deactivate -> 400 Bad Request
        mockMvc.perform(patch("/api/v1/users/" + admin1.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, admin1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cannot deactivate their own account")));

        // 3. Create second admin
        User admin2 = createTestUser("admin_two", "admin_two@sims.local", Role.ADMIN, true);
        String admin2Token = getBearerToken(admin2);

        // Now admin2 deactivates admin1 -> succeeds
        mockMvc.perform(patch("/api/v1/users/" + admin1.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, admin2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Now admin1 is inactive; admin2 is the sole remaining active admin.
        UpdateUserRequestDto demoteAdmin2 = new UpdateUserRequestDto(
                admin2.getEmail(), admin2.getFullName(), Role.PURCHASE_MANAGER, null
        );

        // Verify service level protection throws LastActiveAdminException when active admin count is 1
        assertThatThrownBy(() -> userService.updateUser(admin2.getId(), demoteAdmin2, "other_admin"))
                .isInstanceOf(LastActiveAdminException.class)
                .hasMessageContaining("only remaining active Administrator");

        assertThatThrownBy(() -> userService.toggleUserStatus(admin2.getId(), false, "other_admin"))
                .isInstanceOf(LastActiveAdminException.class)
                .hasMessageContaining("only remaining active Administrator");
    }
}
