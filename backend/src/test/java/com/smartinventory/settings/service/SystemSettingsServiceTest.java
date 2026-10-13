package com.smartinventory.settings.service;

import com.smartinventory.settings.dto.SystemSettingsDto;
import com.smartinventory.settings.dto.UpdateSystemSettingsDto;
import com.smartinventory.settings.entity.SystemSettings;
import com.smartinventory.settings.repository.SystemSettingsRepository;
import com.smartinventory.settings.service.impl.SystemSettingsServiceImpl;
import com.smartinventory.user.entity.User;
import com.smartinventory.user.model.Role;
import com.smartinventory.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemSettingsServiceTest {

    @Mock
    private SystemSettingsRepository systemSettingsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SystemSettingsServiceImpl systemSettingsService;

    @Test
    @DisplayName("getSettings returns existing settings when present")
    void testGetSettingsExisting() {
        SystemSettings s = new SystemSettings();
        s.setId(1L);
        s.setCompanyName("Existing Corp");
        s.setCurrency("EUR");
        s.setTimezone("Europe/Paris");
        s.setDefaultReorderThreshold(15);
        s.setLowStockAlertsEnabled(true);
        s.setOutOfStockAlertsEnabled(false);
        s.setAllowNegativeStock(false);
        s.setRequirePurchaseApproval(true);

        when(systemSettingsRepository.findSingletonSettings()).thenReturn(Optional.of(s));

        SystemSettingsDto result = systemSettingsService.getSettings();

        assertThat(result.companyName()).isEqualTo("Existing Corp");
        assertThat(result.currency()).isEqualTo("EUR");
        assertThat(result.timezone()).isEqualTo("Europe/Paris");
        assertThat(result.defaultReorderThreshold()).isEqualTo(15);
        assertThat(result.lowStockAlertsEnabled()).isTrue();
        assertThat(result.outOfStockAlertsEnabled()).isFalse();
        assertThat(result.requirePurchaseApproval()).isTrue();
    }

    @Test
    @DisplayName("getSettings returns default settings when row is absent")
    void testGetSettingsDefault() {
        when(systemSettingsRepository.findSingletonSettings()).thenReturn(Optional.empty());

        SystemSettingsDto result = systemSettingsService.getSettings();

        assertThat(result.companyName()).isEqualTo("Smart Inventory Systems");
        assertThat(result.currency()).isEqualTo("INR");
        assertThat(result.timezone()).isEqualTo("UTC");
        assertThat(result.defaultReorderThreshold()).isEqualTo(10);
    }

    @Test
    @DisplayName("updateSettings persists changes and tracks actor")
    void testUpdateSettings() {
        SystemSettings existing = new SystemSettings();
        existing.setId(1L);
        when(systemSettingsRepository.findSingletonSettings()).thenReturn(Optional.of(existing));

        User adminUser = new User();
        adminUser.setUsername("admin1");
        adminUser.setFullName("Admin User");
        adminUser.setRole(Role.ADMIN);
        when(userRepository.findByUsernameIgnoreCase("admin1")).thenReturn(Optional.of(adminUser));

        when(systemSettingsRepository.save(any(SystemSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateSystemSettingsDto updateDto = new UpdateSystemSettingsDto(
                "Acme Global",
                "ops@acme.com",
                "+1-555-8899",
                "GBP",
                "Europe/London",
                25,
                true,
                true,
                false,
                true
        );

        SystemSettingsDto result = systemSettingsService.updateSettings(updateDto, "admin1");

        assertThat(result.companyName()).isEqualTo("Acme Global");
        assertThat(result.companyEmail()).isEqualTo("ops@acme.com");
        assertThat(result.companyPhone()).isEqualTo("+1-555-8899");
        assertThat(result.currency()).isEqualTo("GBP");
        assertThat(result.timezone()).isEqualTo("Europe/London");
        assertThat(result.defaultReorderThreshold()).isEqualTo(25);
        assertThat(result.requirePurchaseApproval()).isTrue();
        assertThat(result.updatedBy()).isEqualTo("Admin User");
    }
}
