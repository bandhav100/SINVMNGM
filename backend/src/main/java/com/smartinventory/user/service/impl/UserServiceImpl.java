package com.smartinventory.user.service.impl;

import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.DuplicateResourceException;
import com.smartinventory.common.exception.LastActiveAdminException;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.security.service.DatabaseLockService;
import com.smartinventory.user.dto.CreateUserRequestDto;
import com.smartinventory.user.dto.UpdateUserRequestDto;
import com.smartinventory.user.dto.UserResponseDto;
import com.smartinventory.user.entity.User;
import com.smartinventory.user.model.Role;
import com.smartinventory.user.repository.UserRepository;
import com.smartinventory.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DatabaseLockService databaseLockService;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            DatabaseLockService databaseLockService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.databaseLockService = databaseLockService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<UserResponseDto> getUsers(String search, Role role, Boolean active, Pageable pageable) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<User> page = userRepository.searchUsers(cleanSearch, role, active, pageable);
        return PageResponseDto.from(page.map(UserResponseDto::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserResponseDto.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponseDto createUser(CreateUserRequestDto request) {
        String normalizedUsername = request.username().trim().toLowerCase();
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already in use");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("Email '" + request.email() + "' is already in use");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setActive(true);

        User savedUser = userRepository.save(user);
        return UserResponseDto.fromEntity(savedUser);
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(Long id, UpdateUserRequestDto request, String currentUsername) {
        // Concurrency-safe: serialize role and admin modifications via transaction advisory lock
        databaseLockService.acquireUserMutationLock();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        String normalizedEmail = request.email().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(normalizedEmail) && userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("Email '" + request.email() + "' is already in use by another account");
        }

        // Prevent self-demotion
        if (user.getUsername().equalsIgnoreCase(currentUsername) && request.role() != user.getRole()) {
            throw new IllegalArgumentException("Administrators cannot change their own role. Another administrator must perform this action.");
        }

        // Prevent demoting the last active administrator
        if (user.getRole() == Role.ADMIN && request.role() != Role.ADMIN) {
            long activeAdmins = userRepository.countByRoleAndIsActiveTrue(Role.ADMIN);
            if (activeAdmins <= 1 && user.isActive()) {
                throw new LastActiveAdminException("Cannot demote the only remaining active Administrator");
            }
        }

        user.setEmail(normalizedEmail);
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());

        if (request.password() != null && !request.password().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        }

        User updatedUser = userRepository.save(user);
        return UserResponseDto.fromEntity(updatedUser);
    }

    @Override
    @Transactional
    public UserResponseDto toggleUserStatus(Long id, boolean active, String currentUsername) {
        // Concurrency-safe: serialize role and admin modifications via transaction advisory lock
        databaseLockService.acquireUserMutationLock();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Prevent self-deactivation
        if (user.getUsername().equalsIgnoreCase(currentUsername) && !active) {
            throw new IllegalArgumentException("Administrators cannot deactivate their own account");
        }

        // Prevent deactivating the last active administrator
        if (user.getRole() == Role.ADMIN && !active && user.isActive()) {
            long activeAdmins = userRepository.countByRoleAndIsActiveTrue(Role.ADMIN);
            if (activeAdmins <= 1) {
                throw new LastActiveAdminException("Cannot deactivate the only remaining active Administrator");
            }
        }

        user.setActive(active);
        User updated = userRepository.save(user);
        return UserResponseDto.fromEntity(updated);
    }
}
