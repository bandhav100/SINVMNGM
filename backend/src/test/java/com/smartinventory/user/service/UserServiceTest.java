package com.smartinventory.user.service;

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
import com.smartinventory.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DatabaseLockService databaseLockService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder, databaseLockService);
    }

    @Test
    @DisplayName("getUsers returns paged DTO")
    void getUsers_returnsPagedDto() {
        User user = new User("staff1", "staff1@sims.local", "hash", "Staff One", Role.INVENTORY_MANAGER, true);
        user.setId(2L);
        Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);

        when(userRepository.searchUsers(eq("staff"), eq(Role.INVENTORY_MANAGER), eq(true), any(Pageable.class)))
                .thenReturn(page);

        PageResponseDto<UserResponseDto> response = userService.getUsers("staff", Role.INVENTORY_MANAGER, true, PageRequest.of(0, 10));

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().getFirst().username()).isEqualTo("staff1");
    }

    @Test
    @DisplayName("createUser creates staff user with encoded password")
    void createUser_success() {
        CreateUserRequestDto request = new CreateUserRequestDto(
                "buyer", "buyer@sims.local", "Procurement Head", "Buyer@12345", Role.PURCHASE_MANAGER
        );

        when(userRepository.existsByUsernameIgnoreCase("buyer")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("buyer@sims.local")).thenReturn(false);
        when(passwordEncoder.encode("Buyer@12345")).thenReturn("hashed_pass");

        User saved = new User("buyer", "buyer@sims.local", "hashed_pass", "Procurement Head", Role.PURCHASE_MANAGER, true);
        saved.setId(5L);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponseDto response = userService.createUser(request);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.role()).isEqualTo(Role.PURCHASE_MANAGER);
    }

    @Test
    @DisplayName("createUser throws DuplicateResourceException when username exists")
    void createUser_duplicateUsername_throws() {
        CreateUserRequestDto request = new CreateUserRequestDto(
                "buyer", "buyer@sims.local", "Procurement Head", "Buyer@12345", Role.PURCHASE_MANAGER
        );
        when(userRepository.existsByUsernameIgnoreCase("buyer")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("updateUser throws IllegalArgumentException on self-demotion")
    void updateUser_selfDemotion_throws() {
        User admin = new User("admin", "admin@sims.local", "hash", "Admin", Role.ADMIN, true);
        admin.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        UpdateUserRequestDto request = new UpdateUserRequestDto(
                "admin@sims.local", "Admin Updated", Role.INVENTORY_MANAGER, null
        );

        assertThatThrownBy(() -> userService.updateUser(1L, request, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot change their own role");

        verify(databaseLockService).acquireUserMutationLock();
    }

    @Test
    @DisplayName("updateUser throws LastActiveAdminException when demoting last active admin")
    void updateUser_demoteLastAdmin_throws() {
        User admin = new User("admin2", "admin2@sims.local", "hash", "Admin 2", Role.ADMIN, true);
        admin.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndIsActiveTrue(Role.ADMIN)).thenReturn(1L);

        UpdateUserRequestDto request = new UpdateUserRequestDto(
                "admin2@sims.local", "Admin 2", Role.INVENTORY_MANAGER, null
        );

        assertThatThrownBy(() -> userService.updateUser(2L, request, "superuser"))
                .isInstanceOf(LastActiveAdminException.class)
                .hasMessageContaining("only remaining active Administrator");

        verify(databaseLockService).acquireUserMutationLock();
    }

    @Test
    @DisplayName("toggleUserStatus throws IllegalArgumentException on self-deactivation")
    void toggleUserStatus_selfDeactivation_throws() {
        User admin = new User("admin", "admin@sims.local", "hash", "Admin", Role.ADMIN, true);
        admin.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.toggleUserStatus(1L, false, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot deactivate their own account");

        verify(databaseLockService).acquireUserMutationLock();
    }

    @Test
    @DisplayName("toggleUserStatus throws LastActiveAdminException when deactivating last active admin")
    void toggleUserStatus_lastActiveAdmin_throws() {
        User admin = new User("admin2", "admin2@sims.local", "hash", "Admin 2", Role.ADMIN, true);
        admin.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndIsActiveTrue(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.toggleUserStatus(2L, false, "superuser"))
                .isInstanceOf(LastActiveAdminException.class)
                .hasMessageContaining("only remaining active Administrator");

        verify(databaseLockService).acquireUserMutationLock();
    }
}
