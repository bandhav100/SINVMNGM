package com.smartinventory.user.controller;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.user.dto.CreateUserRequestDto;
import com.smartinventory.user.dto.UpdateUserRequestDto;
import com.smartinventory.user.dto.UserResponseDto;
import com.smartinventory.user.model.Role;
import com.smartinventory.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Users & Roles", description = "Endpoints for managing user accounts and roles (Administrator only)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users", description = "Retrieves a paginated list of system users with optional search and role filter")
    public ResponseEntity<PageResponseDto<UserResponseDto>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @SortDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponseDto<UserResponseDto> users = userService.getUsers(search, role, active, pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves user profile details")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        UserResponseDto user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Registers a new staff user with an assigned role")
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody CreateUserRequestDto request) {
        UserResponseDto created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates user contact details and role assignment")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequestDto request,
            Authentication authentication
    ) {
        String currentUsername = authentication != null ? authentication.getName() : "";
        UserResponseDto updated = userService.updateUser(id, request, currentUsername);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Toggle user status", description = "Activates or deactivates a user account")
    public ResponseEntity<UserResponseDto> toggleUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> statusUpdate,
            Authentication authentication
    ) {
        boolean active = statusUpdate.getOrDefault("active", true);
        String currentUsername = authentication != null ? authentication.getName() : "";
        UserResponseDto updated = userService.toggleUserStatus(id, active, currentUsername);
        return ResponseEntity.ok(updated);
    }
}
