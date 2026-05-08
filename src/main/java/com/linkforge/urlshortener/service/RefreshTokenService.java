package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.entity.RefreshToken;
import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.exception.auth.TokenException;
import com.linkforge.urlshortener.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

// Manages the full lifecycle of refresh tokens: creation, validation, rotation, revocation, and cleanup.
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    // Creates a new refresh token, stores only its SHA-256 hash, and returns the raw token.
    @Transactional
    public String createRefreshToken(User user, String ipAddress, String userAgent) {
        // Generate a cryptographically random token using UUID
        String rawToken = UUID.randomUUID().toString();

        // Calculate the expiry time from the configured TTL
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(refreshTokenExpirationMs / 1000);

        // Only the SHA-256 hash is persisted — the raw token is never stored in the database
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setHashedToken(hashToken(rawToken));
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);

        // Return the raw token to the client — this is the only time it is ever visible
        return rawToken;
    }

    // Validates a raw refresh token by hashing it and looking it up in the database.
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String rawToken) {
        String hashedToken = hashToken(rawToken);

        // Direct hash lookup is O(1) via the unique index on hashed_token
        RefreshToken token = refreshTokenRepository.findByHashedToken(hashedToken)
                .orElseThrow(() -> new TokenException("Refresh token not found or already revoked"));

        // Reject tokens that have been revoked
        if (token.getRevoked()) {
            throw new TokenException("Refresh token not found or already revoked");
        }

        // Reject tokens that have passed their expiry time
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenException("Refresh token has expired");
        }

        return token;
    }

    // Revokes a single refresh token, used when the user logs out from one device.
    @Transactional
    public void revokeToken(String rawToken) {
        RefreshToken token = validateRefreshToken(rawToken);
        token.setRevoked(true);
        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
    }

    // Revokes the old token and issues a new one in a single transaction.
    @Transactional
    public RotationResult rotateRefreshToken(String rawToken, String ipAddress, String userAgent) {
        // Validate the existing token — throws TokenException if invalid, expired, or revoked
        RefreshToken oldToken = validateRefreshToken(rawToken);
        User user = oldToken.getUser();

        // Immediately revoke the old token to prevent reuse
        oldToken.setRevoked(true);
        oldToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(oldToken);

        // Issue a new refresh token for the same user and device
        String newRawToken = createRefreshToken(user, ipAddress, userAgent);
        return new RotationResult(newRawToken, user);
    }

    // Carries the new raw token and the owning user back to the caller after rotation
    public record RotationResult(String newRawToken, User user) {}

    // Revokes all active tokens for a user — used on password change and logout-all
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
    }

    // Deletes all expired tokens from the database. Runs automatically every day at 2 AM.
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }

    // Hashes a raw token with SHA-256 for secure storage and fast lookup
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the Java spec — this should never happen
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
