package com.smartinventory.auth.service;

import com.smartinventory.auth.dto.AuthResponseDto;
import com.smartinventory.auth.dto.LoginRequestDto;
import com.smartinventory.auth.dto.SetupAdminRequestDto;
import com.smartinventory.auth.dto.SetupStatusResponseDto;
import com.smartinventory.auth.service.impl.AuthServiceImpl;
import com.smartinventory.common.exception.AccountDisabledException;
import com.smartinventory.common.exception.DuplicateResourceException;
import com.smartinventory.common.exception.InitialSetupCompletedException;
import com.smartinventory.common.exception.InvalidCredentialsException;
import com.smartinventory.security.jwt.JwtService;
import com.smartinventory.security.service.DatabaseLockService;
import com.smartinventory.user.dto.UserResponseDto;
import com.smartinventory.user.entity.User;
import com.smartinventory.user.model.Role;
import com.smartinventory.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private DatabaseLockService databaseLockService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtService, databaseLockService);
    }

    @Test
    @DisplayName("getSetupStatus returns true when no admin exists")
    void getSetupStatus_whenNoAdmin_returnsTrue() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);

        SetupStatusResponseDto status = authService.getSetupStatus();

        assertThat(status.setupRequired()).isTrue();
    }

    @Test
    @DisplayName("getSetupStatus returns false when admin exists")
    void getSetupStatus_whenAdminExists_returnsFalse() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        SetupStatusResponseDto status = authService.getSetupStatus();

        assertThat(status.setupRequired()).isFalse();
    }

    @Test
    @DisplayName("setupInitialAdmin creates admin and returns token when no admin exists")
    void setupInitialAdmin_success() {
        SetupAdminRequestDto request = new SetupAdminRequestDto(
                "admin", "admin@sims.local", "System Admin", "Admin@12345"
        );

        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userRepository.existsByUsernameIgnoreCase("admin")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("admin@sims.local")).thenReturn(false);
        when(passwordEncoder.encode("Admin@12345")).thenReturn("hashed_pass");

        User savedUser = new User("admin", "admin@sims.local", "hashed_pass", "System Admin", Role.ADMIN, true);
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("dummy_jwt_token");

        AuthResponseDto response = authService.setupInitialAdmin(request);

        verify(databaseLockService).acquireAdminSetupLock();
        assertThat(response.token()).isEqualTo("dummy_jwt_token");
        assertThat(response.user().username()).isEqualTo("admin");
        assertThat(response.user().role()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("setupInitialAdmin throws InitialSetupCompletedException when admin already exists")
    void setupInitialAdmin_throwsWhenAdminExists() {
        SetupAdminRequestDto request = new SetupAdminRequestDto(
                "admin", "admin@sims.local", "System Admin", "Admin@12345"
        );
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> authService.setupInitialAdmin(request))
                .isInstanceOf(InitialSetupCompletedException.class)
                .hasMessageContaining("already been completed");

        verify(databaseLockService).acquireAdminSetupLock();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("setupInitialAdmin throws DuplicateResourceException when username taken")
    void setupInitialAdmin_throwsWhenUsernameTaken() {
        SetupAdminRequestDto request = new SetupAdminRequestDto(
                "admin", "admin@sims.local", "System Admin", "Admin@12345"
        );
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userRepository.existsByUsernameIgnoreCase("admin")).thenReturn(true);

        assertThatThrownBy(() -> authService.setupInitialAdmin(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    @DisplayName("login succeeds and returns JWT when credentials valid and user active")
    void login_success() {
        LoginRequestDto request = new LoginRequestDto("admin", "password123");
        User user = new User("admin", "admin@sims.local", "encoded_hash", "System Admin", Role.ADMIN, true);
        user.setId(1L);

        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_hash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt_token_123");

        AuthResponseDto response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt_token_123");
        assertThat(response.user().username()).isEqualTo("admin");
    }

    @Test
    @DisplayName("login throws InvalidCredentialsException when password does not match")
    void login_throwsWhenPasswordIncorrect() {
        LoginRequestDto request = new LoginRequestDto("admin", "wrong_password");
        User user = new User("admin", "admin@sims.local", "encoded_hash", "System Admin", Role.ADMIN, true);

        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "encoded_hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("login throws AccountDisabledException when user isActive is false")
    void login_throwsWhenAccountDeactivated() {
        LoginRequestDto request = new LoginRequestDto("admin", "password123");
        User user = new User("admin", "admin@sims.local", "encoded_hash", "System Admin", Role.ADMIN, false);

        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountDisabledException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    @DisplayName("logout revokes JWT token")
    void logout_revokesToken() {
        authService.logout("valid_jwt_token");
        verify(jwtService).revokeToken("valid_jwt_token");
    }
}
