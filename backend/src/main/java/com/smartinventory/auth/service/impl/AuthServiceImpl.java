package com.smartinventory.auth.service.impl;

import com.smartinventory.auth.dto.AuthResponseDto;
import com.smartinventory.auth.dto.LoginRequestDto;
import com.smartinventory.auth.dto.SetupAdminRequestDto;
import com.smartinventory.auth.dto.SetupStatusResponseDto;
import com.smartinventory.auth.service.AuthService;
import com.smartinventory.common.exception.AccountDisabledException;
import com.smartinventory.common.exception.DuplicateResourceException;
import com.smartinventory.common.exception.InitialSetupCompletedException;
import com.smartinventory.common.exception.InvalidCredentialsException;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.security.jwt.JwtService;
import com.smartinventory.security.service.DatabaseLockService;
import com.smartinventory.user.dto.UserResponseDto;
import com.smartinventory.user.entity.User;
import com.smartinventory.user.model.Role;
import com.smartinventory.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DatabaseLockService databaseLockService;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            DatabaseLockService databaseLockService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.databaseLockService = databaseLockService;
    }

    @Override
    @Transactional(readOnly = true)
    public SetupStatusResponseDto getSetupStatus() {
        boolean setupRequired = userRepository.countByRole(Role.ADMIN) == 0;
        return new SetupStatusResponseDto(setupRequired);
    }

    @Override
    @Transactional
    public AuthResponseDto setupInitialAdmin(SetupAdminRequestDto request) {
        // Concurrency-safe: serialize concurrent setup requests via PostgreSQL transaction advisory lock
        databaseLockService.acquireAdminSetupLock();

        if (userRepository.countByRole(Role.ADMIN) > 0) {
            throw new InitialSetupCompletedException("System administrator setup has already been completed. Further public setups are disallowed.");
        }

        String normalizedUsername = request.username().trim().toLowerCase();
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already taken");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("Email '" + request.email() + "' is already in use");
        }

        User admin = new User();
        admin.setUsername(normalizedUsername);
        admin.setEmail(normalizedEmail);
        admin.setFullName(request.fullName().trim());
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRole(Role.ADMIN); // Strictly server-enforced role
        admin.setActive(true);

        User savedAdmin = userRepository.save(admin);
        String token = jwtService.generateToken(savedAdmin);

        return AuthResponseDto.of(token, UserResponseDto.fromEntity(savedAdmin));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {
        String identifier = request.usernameOrEmail().trim();

        User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        if (!user.isActive()) {
            throw new AccountDisabledException("Your account has been deactivated. Please contact an administrator.");
        }

        String token = jwtService.generateToken(user);
        return AuthResponseDto.of(token, UserResponseDto.fromEntity(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        return UserResponseDto.fromEntity(user);
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            jwtService.revokeToken(token);
        }
    }
}
