package com.smartinventory.settings.service.impl;

import com.smartinventory.settings.dto.SystemSettingsDto;
import com.smartinventory.settings.dto.UpdateSystemSettingsDto;
import com.smartinventory.settings.entity.SystemSettings;
import com.smartinventory.settings.repository.SystemSettingsRepository;
import com.smartinventory.settings.service.SystemSettingsService;
import com.smartinventory.user.entity.User;
import com.smartinventory.user.repository.UserRepository;
import com.smartinventory.company.service.CurrentCompanyProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class SystemSettingsServiceImpl implements SystemSettingsService {

    private final SystemSettingsRepository systemSettingsRepository;
    private final UserRepository userRepository;
    private final CurrentCompanyProvider currentCompanyProvider;

    public SystemSettingsServiceImpl(
            SystemSettingsRepository systemSettingsRepository,
            UserRepository userRepository,
            CurrentCompanyProvider currentCompanyProvider
    ) {
        this.systemSettingsRepository = systemSettingsRepository;
        this.userRepository = userRepository;
        this.currentCompanyProvider = currentCompanyProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public SystemSettingsDto getSettings() {
        Long companyId = currentCompanyProvider != null ? currentCompanyProvider.getCurrentCompanyId() : null;
        SystemSettings settings = (companyId != null ? systemSettingsRepository.findByCompanyId(companyId) : Optional.<SystemSettings>empty())
                .orElseGet(() -> systemSettingsRepository.findSingletonSettings()
                        .orElseGet(this::createDefaultSettings));
        return mapToDto(settings);
    }

    @Override
    public SystemSettingsDto updateSettings(UpdateSystemSettingsDto request, String currentUsername) {
        Long companyId = currentCompanyProvider != null ? currentCompanyProvider.getCurrentCompanyId() : null;
        SystemSettings settings = (companyId != null ? systemSettingsRepository.findByCompanyId(companyId) : Optional.<SystemSettings>empty())
                .orElseGet(() -> systemSettingsRepository.findSingletonSettings()
                        .orElseGet(() -> {
                            SystemSettings s = new SystemSettings();
                            if (currentCompanyProvider != null) {
                                s.setCompany(currentCompanyProvider.getCurrentCompany());
                            }
                            return s;
                        }));

        if (settings.getCompany() == null && currentCompanyProvider != null && currentCompanyProvider.getCurrentCompany() != null) {
            settings.setCompany(currentCompanyProvider.getCurrentCompany());
        }

        settings.setCompanyName(request.companyName().trim());
        settings.setCompanyEmail(request.companyEmail() != null ? request.companyEmail().trim() : null);
        settings.setCompanyPhone(request.companyPhone() != null ? request.companyPhone().trim() : null);
        settings.setCurrency(request.currency().trim().toUpperCase());
        settings.setTimezone(request.timezone().trim());
        settings.setDefaultReorderThreshold(request.defaultReorderThreshold());
        settings.setLowStockAlertsEnabled(request.lowStockAlertsEnabled());
        settings.setOutOfStockAlertsEnabled(request.outOfStockAlertsEnabled());
        settings.setAllowNegativeStock(request.allowNegativeStock());
        settings.setRequirePurchaseApproval(request.requirePurchaseApproval());
        settings.setUpdatedAt(Instant.now());

        if (currentUsername != null) {
            userRepository.findByUsernameIgnoreCase(currentUsername).ifPresent(settings::setUpdatedBy);
        }

        SystemSettings saved = systemSettingsRepository.save(settings);
        return mapToDto(saved);
    }

    private SystemSettings createDefaultSettings() {
        SystemSettings s = new SystemSettings();
        s.setId(1L);
        s.setCompanyName("Smart Inventory Systems");
        s.setCompanyEmail("admin@sims.local");
        s.setCompanyPhone("+1 (555) 019-2834");
        s.setCurrency("INR");
        s.setTimezone("UTC");
        s.setDefaultReorderThreshold(10);
        s.setLowStockAlertsEnabled(true);
        s.setOutOfStockAlertsEnabled(true);
        s.setAllowNegativeStock(false);
        s.setRequirePurchaseApproval(false);
        s.setUpdatedAt(Instant.now());
        return s;
    }

    private SystemSettingsDto mapToDto(SystemSettings s) {
        String actor = "System";
        if (s.getUpdatedBy() != null) {
            User u = s.getUpdatedBy();
            actor = (u.getFullName() != null && !u.getFullName().isBlank()) ? u.getFullName() : u.getUsername();
        }

        return new SystemSettingsDto(
                s.getId(),
                s.getCompanyName(),
                s.getCompanyEmail(),
                s.getCompanyPhone(),
                s.getCurrency(),
                s.getTimezone(),
                s.getDefaultReorderThreshold(),
                s.getLowStockAlertsEnabled(),
                s.getOutOfStockAlertsEnabled(),
                s.getAllowNegativeStock(),
                s.getRequirePurchaseApproval(),
                s.getUpdatedAt(),
                actor
        );
    }
}
