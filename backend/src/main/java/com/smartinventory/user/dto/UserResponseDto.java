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
        Instant updatedAt
) {
    public static UserResponseDto fromEntity(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
