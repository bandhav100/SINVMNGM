package com.smartinventory.auth.controller;

import com.smartinventory.auth.dto.AuthResponseDto;
import com.smartinventory.auth.dto.LoginRequestDto;
import com.smartinventory.auth.dto.SetupAdminRequestDto;
import com.smartinventory.auth.dto.SetupStatusResponseDto;
import com.smartinventory.auth.service.AuthService;
import com.smartinventory.user.dto.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication, onboarding, and sessions")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/setup-status")
    @Operation(summary = "Check first-time setup status", description = "Returns whether an initial administrator account needs to be created")
    public ResponseEntity<SetupStatusResponseDto> getSetupStatus() {
        return ResponseEntity.ok(authService.getSetupStatus());
    }

    @GetMapping("/csrf")
    @Operation(summary = "Initialize CSRF cookie", description = "Ensures the XSRF-TOKEN cookie is set for SPA state-changing requests")
    public ResponseEntity<Map<String, String>> initCsrf() {
        return ResponseEntity.ok(Map.of("message", "CSRF token initialized"));
    }

    @PostMapping("/setup-admin")
    @Operation(summary = "Initial administrator setup", description = "Creates the primary system administrator account when no admin exists")
    public ResponseEntity<AuthResponseDto> setupInitialAdmin(
            @Valid @RequestBody SetupAdminRequestDto request,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            HttpServletResponse response
    ) {
        AuthResponseDto authResult = authService.setupInitialAdmin(request);
        String token = authResult.token();

        // 1. Set HttpOnly Cookie for browser sessions
        setJwtCookie(response, token);
        // 2. Set Authorization header for API / test client inspection
        response.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        // 3. For browsers, do NOT expose token in JSON body; for explicit API clients (e.g. Karate/cURL), include it
        boolean isApiClient = "api".equalsIgnoreCase(clientType);
        AuthResponseDto responseBody = isApiClient ? authResult : AuthResponseDto.of(null, authResult.user());

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    @PostMapping("/register-company")
    @Operation(summary = "Register new company workspace", description = "Creates a new company workspace and its initial master administrator")
    public ResponseEntity<AuthResponseDto> registerCompany(
            @Valid @RequestBody com.smartinventory.auth.dto.RegisterCompanyRequestDto request,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            HttpServletResponse response
    ) {
        AuthResponseDto authResult = authService.registerCompany(request);
        String token = authResult.token();

        setJwtCookie(response, token);
        response.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        boolean isApiClient = "api".equalsIgnoreCase(clientType);
        AuthResponseDto responseBody = isApiClient ? authResult : AuthResponseDto.of(null, authResult.user());

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user credentials and issues a JWT token")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            HttpServletResponse response
    ) {
        AuthResponseDto authResult = authService.login(request);
        String token = authResult.token();

        // 1. Set HttpOnly Cookie for browser sessions
        setJwtCookie(response, token);
        // 2. Set Authorization header for API / test client inspection
        response.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        // 3. For browsers, do NOT expose token in JSON body; for explicit API clients (e.g. Karate/cURL), include it
        boolean isApiClient = "api".equalsIgnoreCase(clientType);
        AuthResponseDto responseBody = isApiClient ? authResult : AuthResponseDto.of(null, authResult.user());

        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile", description = "Retrieves profile of the authenticated user")
    public ResponseEntity<UserResponseDto> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserResponseDto user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Tears down the user session and revokes the JWT")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = extractToken(request);
        if (token != null) {
            authService.logout(token);
        }

        clearJwtCookie(response);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(false) // Localhost HTTP friendly
                .path("/")
                .maxAge(86400)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
