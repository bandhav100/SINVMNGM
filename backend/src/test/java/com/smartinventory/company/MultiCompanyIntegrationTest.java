package com.smartinventory.company;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.auth.dto.LoginRequestDto;
import com.smartinventory.auth.dto.RegisterCompanyRequestDto;
import com.smartinventory.company.entity.Company;
import com.smartinventory.company.repository.CompanyRepository;
import com.smartinventory.product.dto.ProductRequestDto;
import com.smartinventory.product.repository.ProductRepository;
import com.smartinventory.user.entity.User;
import com.smartinventory.user.model.Role;
import com.smartinventory.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class MultiCompanyIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Multiple companies can register independently without blocking or overwriting")
    void testMultipleCompaniesCanRegister() throws Exception {
        String comp1Name = "Alpha Logistics " + System.currentTimeMillis();
        String comp1Email = "admin@" + System.currentTimeMillis() + "-alpha.com";

        RegisterCompanyRequestDto req1 = new RegisterCompanyRequestDto(
                comp1Name,
                "Alice Admin",
                comp1Email,
                "Password123!",
                "alpha_admin_" + System.currentTimeMillis(),
                "+1 555-0101"
        );

        MvcResult result1 = mockMvc.perform(post("/api/v1/auth/register-company")
                        .header("X-Client-Type", "api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json1 = objectMapper.readTree(result1.getResponse().getContentAsString());
        assertThat(json1.get("token").asText()).isNotBlank();
        assertThat(json1.get("user").get("role").asText()).isEqualTo("ADMIN");
        assertThat(json1.get("user").get("companyName").asText()).isEqualTo(comp1Name);

        // Register a second company while the first company already exists
        String comp2Name = "Beta Global " + System.currentTimeMillis();
        String comp2Email = "admin@" + System.currentTimeMillis() + "-beta.com";

        RegisterCompanyRequestDto req2 = new RegisterCompanyRequestDto(
                comp2Name,
                "Bob Director",
                comp2Email,
                "Password123!",
                "beta_admin_" + System.currentTimeMillis(),
                "+1 555-0202"
        );

        MvcResult result2 = mockMvc.perform(post("/api/v1/auth/register-company")
                        .header("X-Client-Type", "api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json2 = objectMapper.readTree(result2.getResponse().getContentAsString());
        assertThat(json2.get("token").asText()).isNotBlank();
        assertThat(json2.get("user").get("role").asText()).isEqualTo("ADMIN");
        assertThat(json2.get("user").get("companyName").asText()).isEqualTo(comp2Name);

        // Verify both companies exist in database
        assertThat(companyRepository.existsByNameIgnoreCase(comp1Name)).isTrue();
        assertThat(companyRepository.existsByNameIgnoreCase(comp2Name)).isTrue();
    }

    @Test
    @DisplayName("Company workspaces have complete data isolation across products and resources")
    void testCompanyDataIsolation() throws Exception {
        long ts = System.currentTimeMillis();
        String c1Name = "Workspace One " + ts;
        String c1Email = "w1_" + ts + "@tenant.com";
        String c1User = "w1_user_" + ts;

        MvcResult res1 = mockMvc.perform(post("/api/v1/auth/register-company")
                        .header("X-Client-Type", "api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterCompanyRequestDto(
                                c1Name, "Owner One", c1Email, "SecretPass123!", c1User, null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String token1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("token").asText();

        String c2Name = "Workspace Two " + ts;
        String c2Email = "w2_" + ts + "@tenant.com";
        String c2User = "w2_user_" + ts;

        MvcResult res2 = mockMvc.perform(post("/api/v1/auth/register-company")
                        .header("X-Client-Type", "api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterCompanyRequestDto(
                                c2Name, "Owner Two", c2Email, "SecretPass123!", c2User, null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String token2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("token").asText();

        // 1. Company 1 creates a product
        ProductRequestDto p1 = new ProductRequestDto(
                "Tenant 1 Special Tool",
                "SKU-T1-" + ts,
                "Exclusive to tenant 1",
                BigDecimal.valueOf(99.95),
                50,
                5,
                null
        );

        MvcResult prodCreateRes = mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p1)))
                .andExpect(status().isCreated())
                .andReturn();

        long p1Id = objectMapper.readTree(prodCreateRes.getResponse().getContentAsString()).get("id").asLong();

        // 2. Company 2 creates a product
        ProductRequestDto p2 = new ProductRequestDto(
                "Tenant 2 Gadget",
                "SKU-T2-" + ts,
                "Exclusive to tenant 2",
                BigDecimal.valueOf(49.50),
                25,
                2,
                null
        );

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p2)))
                .andExpect(status().isCreated());

        // 3. Company 1 lists products: sees p1, does NOT see p2
        MvcResult listRes1 = mockMvc.perform(get("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items1 = objectMapper.readTree(listRes1.getResponse().getContentAsString()).get("content");
        boolean hasP1inC1 = false;
        boolean hasP2inC1 = false;
        for (JsonNode item : items1) {
            if (item.get("sku").asText().equals("SKU-T1-" + ts)) hasP1inC1 = true;
            if (item.get("sku").asText().equals("SKU-T2-" + ts)) hasP2inC1 = true;
        }
        assertThat(hasP1inC1).isTrue();
        assertThat(hasP2inC1).isFalse();

        // 4. Company 2 lists products: sees p2, does NOT see p1
        MvcResult listRes2 = mockMvc.perform(get("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token2))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items2 = objectMapper.readTree(listRes2.getResponse().getContentAsString()).get("content");
        boolean hasP1inC2 = false;
        boolean hasP2inC2 = false;
        for (JsonNode item : items2) {
            if (item.get("sku").asText().equals("SKU-T1-" + ts)) hasP1inC2 = true;
            if (item.get("sku").asText().equals("SKU-T2-" + ts)) hasP2inC2 = true;
        }
        assertThat(hasP1inC2).isFalse();
        assertThat(hasP2inC2).isTrue();

        // 5. Cross-company access: Company 2 attempts to get Company 1's product by ID -> 404
        mockMvc.perform(get("/api/v1/products/" + p1Id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token2))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sign in flow succeeds with company credentials and logout functions properly")
    void testLoginAndLogoutFlow() throws Exception {
        long ts = System.currentTimeMillis();
        String email = "login_test_" + ts + "@company.com";
        String password = "SecurePassword123!";
        String username = "logintest_" + ts;

        mockMvc.perform(post("/api/v1/auth/register-company")
                        .header("X-Client-Type", "api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterCompanyRequestDto(
                                "Login Test Co " + ts, "Test Admin", email, password, username, null
                        ))))
                .andExpect(status().isCreated());

        // Perform login with email
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Client-Type", "api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginRes.getResponse().getContentAsString()).get("token").asText();
        assertThat(token).isNotBlank();

        // Authenticated request to /auth/me
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonNode -> assertThat(jsonNode.getResponse().getContentAsString()).contains(email));

        // Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("jwt", 0));
    }
}
