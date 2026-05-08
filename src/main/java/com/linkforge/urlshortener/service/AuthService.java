package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.dto.request.ChangePasswordRequest;
import com.linkforge.urlshortener.dto.request.LoginRequest;
import com.linkforge.urlshortener.dto.response.AuthResponse;
import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.exception.auth.InvalidCredentialsException;
import com.linkforge.urlshortener.repository.UserRepository;
import com.linkforge.urlshortener.security.JwtUtil;
import com.linkforge.urlshortener.service.RefreshTokenService.RotationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Handles authentication: login, token refresh, logout, and password change.
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpirationMs;

    // Verifies credentials and issues a new access token and refresh token pair.
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        // Use a generic error message to avoid revealing whether the email exists
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // Compare the submitted password against the stored BCrypt hash
        if (!passwordEncoder.matches(request.getPassword(), user.getHashedPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Generate a short-lived JWT access token
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getId());

        // Generate and persist a refresh token tied to the user's current device
        String refreshToken = refreshTokenService.createRefreshToken(user, ipAddress, userAgent);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                accessTokenExpirationMs / 1000
        );
    }

    // Rotates the refresh token and issues a new access token.
    @Transactional
    public AuthResponse refresh(String rawRefreshToken, String ipAddress, String userAgent) {
        // Revoke the old token and issue a new one in a single transaction
        RotationResult rotation = refreshTokenService.rotateRefreshToken(rawRefreshToken, ipAddress, userAgent);

        // Issue a new access token for the same user
        String newAccessToken = jwtUtil.generateAccessToken(
                rotation.user().getUsername(), rotation.user().getId());

        return new AuthResponse(
                newAccessToken,
                rotation.newRawToken(),
                "Bearer",
                accessTokenExpirationMs / 1000
        );
    }

    // Revokes the given refresh token, logging the user out of this device.
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeToken(rawRefreshToken);
    }

    // Revokes all refresh tokens for the user, logging them out of every device.
    @Transactional
    public void logoutAll(Long userId) {
        refreshTokenService.revokeAllUserTokens(userId);
    }

    // Changes the user's password, then revokes all active sessions.
    // Password validation is delegated to UserService.
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        // Revoke all refresh tokens so existing sessions are invalidated immediately
        refreshTokenService.revokeAllUserTokens(userId);
    }
}
