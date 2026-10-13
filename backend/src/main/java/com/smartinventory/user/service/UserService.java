package com.smartinventory.user.service;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.user.dto.CreateUserRequestDto;
import com.smartinventory.user.dto.UpdateUserRequestDto;
import com.smartinventory.user.dto.UserResponseDto;
import com.smartinventory.user.model.Role;
import org.springframework.data.domain.Pageable;

public interface UserService {

    PageResponseDto<UserResponseDto> getUsers(String search, Role role, Boolean active, Pageable pageable);

    UserResponseDto getUserById(Long id);

    UserResponseDto createUser(CreateUserRequestDto request);

    UserResponseDto updateUser(Long id, UpdateUserRequestDto request, String currentUsername);

    UserResponseDto toggleUserStatus(Long id, boolean active, String currentUsername);
}
