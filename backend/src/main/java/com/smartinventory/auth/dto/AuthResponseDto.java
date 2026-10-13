package com.smartinventory.auth.dto;

import com.smartinventory.user.dto.UserResponseDto;

public record AuthResponseDto(
        String token,
        String tokenType,
        UserResponseDto user
) {
    public static AuthResponseDto of(String token, UserResponseDto user) {
        return new AuthResponseDto(token, "Bearer", user);
    }
}
