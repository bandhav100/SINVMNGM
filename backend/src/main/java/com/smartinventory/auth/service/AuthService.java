package com.smartinventory.auth.service;

import com.smartinventory.auth.dto.AuthResponseDto;
import com.smartinventory.auth.dto.LoginRequestDto;
import com.smartinventory.auth.dto.RegisterCompanyRequestDto;
import com.smartinventory.auth.dto.SetupAdminRequestDto;
import com.smartinventory.auth.dto.SetupStatusResponseDto;
import com.smartinventory.user.dto.UserResponseDto;

public interface AuthService {

    SetupStatusResponseDto getSetupStatus();

    AuthResponseDto setupInitialAdmin(SetupAdminRequestDto request);

    AuthResponseDto registerCompany(RegisterCompanyRequestDto request);

    AuthResponseDto login(LoginRequestDto request);

    UserResponseDto getCurrentUser(String username);

    void logout(String token);
}
