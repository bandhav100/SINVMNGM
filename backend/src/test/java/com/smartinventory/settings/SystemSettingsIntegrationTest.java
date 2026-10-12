package com.smartinventory.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.security.jwt.JwtService;
import com.smartinventory.settings.dto.UpdateSystemSettingsDto;
import com.smartinventory.settings.entity.SystemSettings;
import com.smartinventory.settings.repository.SystemSettingsRepository;
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

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SystemSettingsIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SystemSettingsRepository systemSettingsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private User adminUser;
    private User staffUser;
    private String adminToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        adminUser = new User();
        adminUser.setUsername("set_adm_" + suffix);
        adminUser.setEmail("set_adm_" + suffix + "@sims.local");
        adminUser.setPasswordHash(passwordEncoder.encode("Password@123"));
        adminUser.setFullName("Settings Admin");
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);
        adminUser = userRepository.save(adminUser);

        staffUser = new User();
        staffUser.setUsername("set_stf_" + suffix);
        staffUser.setEmail("set_stf_" + suffix + "@sims.local");
        staffUser.setPasswordHash(passwordEncoder.encode("Password@123"));
        staffUser.setFullName("Settings Staff");
        staffUser.setRole(Role.SALES_STAFF);
        staffUser.setActive(true);
        staffUser = userRepository.save(staffUser);

        adminToken = jwtService.generateToken(adminUser);
        staffToken = jwtService.generateToken(staffUser);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/v1/settings allows ADMIN and returns populated settings without secrets")
    void testGetSettingsSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.companyName", not(emptyOrNullString())))
                .andExpect(jsonPath("$.currency", not(emptyOrNullString())))
                .andExpect(jsonPath("$.defaultReorderThreshold", greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("PUT /api/v1/settings allows ADMIN to update configuration with validation")
    void testUpdateSettingsSuccess() throws Exception {
        UpdateSystemSettingsDto request = new UpdateSystemSettingsDto(
                "Updated Corp",
                "contact@updatedcorp.com",
                "+1-555-4321",
                "CAD",
                "America/Toronto",
                18,
                true,
                false,
                false,
                true
        );

        mockMvc.perform(put("/api/v1/settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName", is("Updated Corp")))
                .andExpect(jsonPath("$.currency", is("CAD")))
                .andExpect(jsonPath("$.timezone", is("America/Toronto")))
                .andExpect(jsonPath("$.defaultReorderThreshold", is(18)))
                .andExpect(jsonPath("$.outOfStockAlertsEnabled", is(false)))
                .andExpect(jsonPath("$.requirePurchaseApproval", is(true)))
                .andExpect(jsonPath("$.updatedBy", is("Settings Admin")));
    }

    @Test
    @DisplayName("PUT /api/v1/settings rejects negative threshold with 400 Bad Request")
    void testUpdateSettingsValidationFailure() throws Exception {
        UpdateSystemSettingsDto invalidRequest = new UpdateSystemSettingsDto(
                "Updated Corp",
                "contact@updatedcorp.com",
                "+1-555-4321",
                "CAD",
                "America/Toronto",
                -5, // invalid
                true,
                false,
                false,
                true
        );

        mockMvc.perform(put("/api/v1/settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RBAC restricts non-ADMIN users with 403 Forbidden")
    void testRbacRestrictions() throws Exception {
        mockMvc.perform(get("/api/v1/settings")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());

        UpdateSystemSettingsDto request = new UpdateSystemSettingsDto(
                "Updated Corp",
                "contact@updatedcorp.com",
                "+1-555-4321",
                "USD",
                "UTC",
                10,
                true,
                true,
                false,
                false
        );

        mockMvc.perform(put("/api/v1/settings")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
