package com.smartinventory.settings.service;

import com.smartinventory.settings.dto.SystemSettingsDto;
import com.smartinventory.settings.dto.UpdateSystemSettingsDto;

public interface SystemSettingsService {

    SystemSettingsDto getSettings();

    SystemSettingsDto updateSettings(UpdateSystemSettingsDto request, String currentUsername);
}
