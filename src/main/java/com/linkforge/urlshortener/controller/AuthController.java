package com.linkforge.urlshortener.controller;

import com.linkforge.urlshortener.dto.request.LoginRequest;
import com.linkforge.urlshortener.dto.request.RefreshTokenRequest;
import com.linkforge.urlshortener.dto.request.RegisterRequest;
import com.linkforge.urlshortener.dto.response.ApiResponse;
import com.linkforge.urlshortener.dto.response.AuthResponse;
import com.linkforge.urlshortener.dto.response.UserResponse;
import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.service.AuthService;
import com.linkforge.urlshortener.service.UserService;
import com.linkforge.urlshortener.util.RequestContextUtil;
import com.linkforge.urlshortener.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// REST controller for authentication endpoints: register, login, refresh, logout
@Tag(name = "Authentication", description = "Register, login, refresh tokens, and logout")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    // POST /api/v1/auth/register - public endpoint, no auth required
    @Operation(summary = "Register", description = "Create a new user account. No authentication required.")
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse userResponse = userService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("Registration successful", userResponse));
    }

    // POST /api/v1/auth/login - authenticate and receive JWT tokens
    @Operation(summary = "Login", description = "Authenticate with email and password. Returns JWT access token and refresh token.")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = RequestContextUtil.getClientIp(httpRequest);
        String userAgent = RequestContextUtil.getUserAgent(httpRequest);

        AuthResponse authResponse = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    // POST /api/v1/auth/refresh - exchange refresh token for new access token and rotate the old one
    @Operation(summary = "Refresh access token", description = "Exchange a valid refresh token for a new JWT access token. The old refresh token is revoked and a new one is issued.")
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = RequestContextUtil.getClientIp(httpRequest);
        String userAgent = RequestContextUtil.getUserAgent(httpRequest);

        AuthResponse authResponse = authService.refresh(request.getRefreshToken(), ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authResponse));
    }

    // POST /api/v1/auth/logout - revoke the current refresh token.
    // No JWT required so users can log out even after the access token has expired.
    @Operation(summary = "Logout", description = "Revoke the current refresh token. Client must discard the access token. No JWT required.")
    @SecurityRequirements
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {

        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    // POST /api/v1/auth/logout-all - revoke all refresh tokens for current user
    @Operation(summary = "Logout from all devices", description = "Revoke all active refresh tokens for the authenticated user.")
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll() {
        User currentUser = SecurityUtil.getCurrentUser();
        authService.logoutAll(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Logged out from all devices successfully", null));
    }
}


