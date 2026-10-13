package com.smartinventory.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.auth.dto.AuthResponseDto;
import com.smartinventory.auth.dto.LoginRequestDto;
import com.smartinventory.auth.dto.SetupAdminRequestDto;
import com.smartinventory.auth.dto.SetupStatusResponseDto;
import com.smartinventory.auth.service.AuthService;
import com.smartinventory.common.exception.GlobalExceptionHandler;
import com.smartinventory.user.dto.UserResponseDto;
import com.smartinventory.user.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;
    private UserResponseDto sampleUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        sampleUser = new UserResponseDto(
                1L, "admin", "admin@sims.local", "System Admin", Role.ADMIN, true, Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("GET /api/v1/auth/setup-status - Returns 200 with status")
    void getSetupStatus_returns200() throws Exception {
        when(authService.getSetupStatus()).thenReturn(new SetupStatusResponseDto(true));

        mockMvc.perform(get("/api/v1/auth/setup-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/auth/setup-admin - Browser client sets HttpOnly cookie and conceals token in body")
    void setupAdmin_browser_returns201AndConcealsToken() throws Exception {
        SetupAdminRequestDto request = new SetupAdminRequestDto(
                "admin", "admin@sims.local", "System Admin", "Admin@12345"
        );
        AuthResponseDto response = AuthResponseDto.of("jwt_token_admin", sampleUser);

        when(authService.setupInitialAdmin(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/setup-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer jwt_token_admin"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.user.username").value("admin"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/setup-admin - API client with X-Client-Type: api receives token in body")
    void setupAdmin_apiClient_returnsTokenInBody() throws Exception {
        SetupAdminRequestDto request = new SetupAdminRequestDto(
                "admin", "admin@sims.local", "System Admin", "Admin@12345"
        );
        AuthResponseDto response = AuthResponseDto.of("jwt_token_admin", sampleUser);

        when(authService.setupInitialAdmin(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/setup-admin")
                        .header("X-Client-Type", "api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer jwt_token_admin"))
                .andExpect(jsonPath("$.token").value("jwt_token_admin"))
                .andExpect(jsonPath("$.user.username").value("admin"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Browser client sets HttpOnly cookie and conceals token in body")
    void login_browser_returns200AndSetsCookie() throws Exception {
        LoginRequestDto request = new LoginRequestDto("admin", "Admin@12345");
        AuthResponseDto response = AuthResponseDto.of("jwt_token_admin", sampleUser);

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer jwt_token_admin"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - API client with X-Client-Type: api receives token in body")
    void login_apiClient_returnsTokenInBody() throws Exception {
        LoginRequestDto request = new LoginRequestDto("admin", "Admin@12345");
        AuthResponseDto response = AuthResponseDto.of("jwt_token_admin", sampleUser);

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Client-Type", "api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer jwt_token_admin"))
                .andExpect(jsonPath("$.token").value("jwt_token_admin"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Authenticated user returns 200 OK")
    void getCurrentUser_returns200() throws Exception {
        when(authService.getCurrentUser("admin")).thenReturn(sampleUser);

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken("admin", null);

        mockMvc.perform(get("/api/v1/auth/me").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - Clears cookie and returns 200 OK")
    void logout_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
