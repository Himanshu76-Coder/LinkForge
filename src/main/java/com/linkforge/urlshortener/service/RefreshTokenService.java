package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.entity.RefreshToken;
import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.exception.TokenException;
import com.linkforge.urlshortener.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// Service handling all refresh token lifecycle: creation, validation, revocation, and cleanup
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    // Create a new refresh token for the user and persist its hash - PRD BR-76
    @Transactional
    public String createRefreshToken(User user, String ipAddress, String userAgent) {
        // Generate a cryptographically random raw token
        String rawToken = UUID.randomUUID().toString();

        // Calculate expiration time
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(refreshTokenExpirationMs / 1000);

        // Store only the BCrypt hash - raw token is never persisted - PRD Section 10.3
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setHashedToken(passwordEncoder.encode(rawToken));
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);

        // Return the raw token to the client - this is the only time it is visible
        return rawToken;
    }

    // Validate a raw refresh token and return the matching entity - PRD BR-82
    public RefreshToken validateRefreshToken(String rawToken) {
        // Load all active tokens and find the one matching the raw token via BCrypt
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllActiveTokens(LocalDateTime.now());

        RefreshToken matched = activeTokens.stream()
                .filter(rt -> passwordEncoder.matches(rawToken, rt.getHashedToken()))
                .findFirst()
                .orElseThrow(() -> new TokenException("Refresh token not found or already revoked"));

        // Check expiration - PRD BR-82
        if (matched.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenException("Refresh token has expired");
        }

        return matched;
    }

    // Revoke a single refresh token on logout - PRD BR-78
    @Transactional
    public void revokeToken(String rawToken) {
        RefreshToken token = validateRefreshToken(rawToken);
        token.setRevoked(true);
        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
    }

    // Revoke all tokens for a user - used on password change and logout-all
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
    }

    // Scheduled cleanup of expired tokens - runs every day at 2 AM
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
