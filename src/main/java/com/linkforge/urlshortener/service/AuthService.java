package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.dto.request.LoginRequest;
import com.linkforge.urlshortener.dto.response.AuthResponse;
import com.linkforge.urlshortener.entity.RefreshToken;
import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.exception.InvalidCredentialsException;
import com.linkforge.urlshortener.repository.UserRepository;
import com.linkforge.urlshortener.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Service handling authentication: login, token refresh, and logout
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpirationMs;

    // Authenticate user and issue access + refresh tokens - PRD Section 5.21
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        // Find user by email - PRD BR-74
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // Verify password against stored BCrypt hash - PRD BR-74
        if (!passwordEncoder.matches(request.getPassword(), user.getHashedPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Generate JWT access token - PRD BR-75
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getId());

        // Generate and persist refresh token - PRD BR-75, BR-76
        String refreshToken = refreshTokenService.createRefreshToken(user, ipAddress, userAgent);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                accessTokenExpirationMs / 1000
        );
    }

    // Issue a new access token using a valid refresh token - PRD Section 5.23
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        // Validate the refresh token - throws TokenException if invalid/expired/revoked
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(rawRefreshToken);

        User user = refreshToken.getUser();

        // Generate new access token
        String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getId());

        // Return new access token - refresh token stays the same (no rotation in v1.0)
        return new AuthResponse(
                newAccessToken,
                rawRefreshToken,
                "Bearer",
                accessTokenExpirationMs / 1000
        );
    }

    // Revoke the refresh token on logout - PRD Section 5.22
    @Transactional
    public void logout(String rawRefreshToken) {
        // Revoke the token - PRD BR-78
        refreshTokenService.revokeToken(rawRefreshToken);
    }

    // Revoke all refresh tokens for a user (logout from all devices)
    @Transactional
    public void logoutAll(Long userId) {
        refreshTokenService.revokeAllUserTokens(userId);
    }
}
