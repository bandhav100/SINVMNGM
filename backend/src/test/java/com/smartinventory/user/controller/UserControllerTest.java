package com.smartinventory.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.GlobalExceptionHandler;
import com.smartinventory.user.dto.CreateUserRequestDto;
import com.smartinventory.user.dto.UpdateUserRequestDto;
import com.smartinventory.user.dto.UserResponseDto;
import com.smartinventory.user.model.Role;
import com.smartinventory.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;
    private UserResponseDto sampleUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        objectMapper = new ObjectMapper();
        sampleUser = new UserResponseDto(
                10L, "buyer1", "buyer@sims.local", "Procurement Head", Role.PURCHASE_MANAGER, true, Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("GET /api/v1/users - Returns 200 OK with paginated users")
    void getUsers_returns200() throws Exception {
        PageResponseDto<UserResponseDto> pageResponse = new PageResponseDto<>(
                List.of(sampleUser), 0, 10, 1, 1, true, true
        );
        when(userService.getUsers(any(), any(), any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("buyer1"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - Returns 200 OK with user details")
    void getUserById_returns200() throws Exception {
        when(userService.getUserById(10L)).thenReturn(sampleUser);

        mockMvc.perform(get("/api/v1/users/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.fullName").value("Procurement Head"));
    }

    @Test
    @DisplayName("POST /api/v1/users - Valid payload returns 201 Created")
    void createUser_returns201() throws Exception {
        CreateUserRequestDto request = new CreateUserRequestDto(
                "buyer1", "buyer@sims.local", "Procurement Head", "Buyer@12345", Role.PURCHASE_MANAGER
        );
        when(userService.createUser(any())).thenReturn(sampleUser);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.role").value("PURCHASE_MANAGER"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Valid update returns 200 OK")
    void updateUser_returns200() throws Exception {
        UpdateUserRequestDto request = new UpdateUserRequestDto("buyer_new@sims.local", "Updated Name", Role.PURCHASE_MANAGER, null);
        UserResponseDto updatedUser = new UserResponseDto(
                10L, "buyer1", "buyer_new@sims.local", "Updated Name", Role.PURCHASE_MANAGER, true, Instant.now(), Instant.now()
        );
        when(userService.updateUser(eq(10L), any(), eq("admin"))).thenReturn(updatedUser);

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken("admin", null);

        mockMvc.perform(put("/api/v1/users/10")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("buyer_new@sims.local"));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/status - Status toggle returns 200 OK")
    void toggleUserStatus_returns200() throws Exception {
        UserResponseDto inactiveUser = new UserResponseDto(
                10L, "buyer1", "buyer@sims.local", "Procurement Head", Role.PURCHASE_MANAGER, false, Instant.now(), Instant.now()
        );
        when(userService.toggleUserStatus(eq(10L), eq(false), eq("admin"))).thenReturn(inactiveUser);

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken("admin", null);

        mockMvc.perform(patch("/api/v1/users/10/status")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
