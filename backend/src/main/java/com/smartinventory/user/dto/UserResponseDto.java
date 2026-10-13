package com.smartinventory.user.dto;

import com.smartinventory.user.entity.User;
import com.smartinventory.user.model.Role;

import java.time.Instant;

public record UserResponseDto(
        Long id,
        String username,
        String email,
        String fullName,
        Role role,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Long companyId,
        String companyName
) {
    public UserResponseDto(Long id, String username, String email, String fullName, Role role, boolean active, Instant createdAt, Instant updatedAt) {
        this(id, username, email, fullName, role, active, createdAt, updatedAt, 1L, "Smart Inventory Systems");
    }

    public static UserResponseDto fromEntity(User user) {
        Long companyId = (user.getCompany() != null) ? user.getCompany().getId() : 1L;
        String companyName = (user.getCompany() != null) ? user.getCompany().getName() : "Smart Inventory Systems";
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                companyId,
                companyName
        );
    }
}
