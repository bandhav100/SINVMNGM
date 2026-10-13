package com.smartinventory.auth.service.impl;

import com.smartinventory.auth.dto.AuthResponseDto;
import com.smartinventory.auth.dto.LoginRequestDto;
import com.smartinventory.auth.dto.RegisterCompanyRequestDto;
import com.smartinventory.auth.dto.SetupAdminRequestDto;
import com.smartinventory.auth.dto.SetupStatusResponseDto;
import com.smartinventory.auth.service.AuthService;
import com.smartinventory.common.exception.AccountDisabledException;
import com.smartinventory.common.exception.DuplicateResourceException;
import com.smartinventory.common.exception.InitialSetupCompletedException;
import com.smartinventory.common.exception.InvalidCredentialsException;
import com.smartinventory.common.exception.ResourceNotFoundException;
import com.smartinventory.company.entity.Company;
import com.smartinventory.company.repository.CompanyRepository;
import com.smartinventory.security.jwt.JwtService;
import com.smartinventory.security.service.DatabaseLockService;
import com.smartinventory.settings.entity.SystemSettings;
import com.smartinventory.settings.repository.SystemSettingsRepository;
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
    private final CompanyRepository companyRepository;
    private final SystemSettingsRepository systemSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DatabaseLockService databaseLockService;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthServiceImpl(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            SystemSettingsRepository systemSettingsRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            DatabaseLockService databaseLockService
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.systemSettingsRepository = systemSettingsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.databaseLockService = databaseLockService;
    }

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            DatabaseLockService databaseLockService
    ) {
        this(userRepository, null, null, passwordEncoder, jwtService, databaseLockService);
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

        Company defaultCompany = null;
        if (companyRepository != null) {
            defaultCompany = companyRepository.findById(1L).orElseGet(() -> {
                Company comp = new Company("Smart Inventory Systems", "admin@sims.local", "+1 (555) 019-2834");
                comp.setId(1L);
                return companyRepository.save(comp);
            });
        }

        User admin = new User();
        if (defaultCompany != null) {
            admin.setCompany(defaultCompany);
        }
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
    @Transactional
    public AuthResponseDto registerCompany(RegisterCompanyRequestDto request) {
        String companyName = request.companyName().trim();
        if (companyRepository.existsByNameIgnoreCase(companyName)) {
            throw new DuplicateResourceException("Company name '" + companyName + "' is already registered");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("Email '" + request.email() + "' is already registered");
        }

        String rawUsername = request.username();
        String normalizedUsername;
        if (rawUsername != null && !rawUsername.trim().isBlank()) {
            normalizedUsername = rawUsername.trim().toLowerCase();
            if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
                throw new DuplicateResourceException("Username '" + rawUsername.trim() + "' is already taken");
            }
        } else {
            // Derive username from email prefix
            String prefix = normalizedEmail.contains("@") ? normalizedEmail.substring(0, normalizedEmail.indexOf('@')) : normalizedEmail;
            prefix = prefix.replaceAll("[^a-zA-Z0-9_.-]", "_");
            normalizedUsername = prefix;
            int counter = 1;
            while (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
                normalizedUsername = prefix + counter++;
            }
        }

        // 1. Create and persist new Company
        Company company = new Company();
        company.setName(companyName);
        company.setEmail(normalizedEmail);
        if (request.phone() != null && !request.phone().trim().isBlank()) {
            company.setPhone(request.phone().trim());
        }
        company.setCurrency("INR");
        company.setTimezone("Asia/Kolkata");
        Company savedCompany = companyRepository.save(company);

        // 2. Initialize default SystemSettings for the new Company
        SystemSettings settings = new SystemSettings();
        settings.setId(savedCompany.getId());
        settings.setCompany(savedCompany);
        settings.setCompanyName(savedCompany.getName());
        settings.setCompanyEmail(savedCompany.getEmail());
        settings.setCompanyPhone(savedCompany.getPhone());
        settings.setCurrency("INR");
        settings.setTimezone("Asia/Kolkata");
        settings.setDefaultReorderThreshold(10);
        settings.setLowStockAlertsEnabled(true);
        settings.setOutOfStockAlertsEnabled(true);
        settings.setAllowNegativeStock(false);
        settings.setRequirePurchaseApproval(false);
        systemSettingsRepository.save(settings);

        // 3. Create initial master administrator for this company
        User admin = new User();
        admin.setCompany(savedCompany);
        admin.setUsername(normalizedUsername);
        admin.setEmail(normalizedEmail);
        admin.setFullName(request.fullName().trim());
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRole(Role.ADMIN);
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
        User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return UserResponseDto.fromEntity(user);
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            jwtService.revokeToken(token);
        }
    }
}
