package com.smartinventory.settings.controller;

import com.smartinventory.settings.dto.SystemSettingsDto;
import com.smartinventory.settings.dto.UpdateSystemSettingsDto;
import com.smartinventory.settings.service.SystemSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
public class SystemSettingsController {

    private final SystemSettingsService systemSettingsService;

    public SystemSettingsController(SystemSettingsService systemSettingsService) {
        this.systemSettingsService = systemSettingsService;
    }

    @GetMapping
    public ResponseEntity<SystemSettingsDto> getSettings() {
        return ResponseEntity.ok(systemSettingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<SystemSettingsDto> updateSettings(
            @Valid @RequestBody UpdateSystemSettingsDto request,
            Authentication authentication
    ) {
        String username = (authentication != null) ? authentication.getName() : null;
        return ResponseEntity.ok(systemSettingsService.updateSettings(request, username));
    }
}
