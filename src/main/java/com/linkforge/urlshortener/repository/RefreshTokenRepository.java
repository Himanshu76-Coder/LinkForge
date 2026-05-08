package com.linkforge.urlshortener.repository;

import com.linkforge.urlshortener.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

// Repository for RefreshToken entity database operations
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Direct lookup by SHA-256 hash - O(1) via unique index on hashed_token
    Optional<RefreshToken> findByHashedToken(String hashedToken);

    // Revoke all active tokens for a user (used on logout-all and password change)
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :revokedAt WHERE rt.user.id = :userId AND rt.revoked = false")
    void revokeAllUserTokens(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);

    // Delete all expired tokens (called by scheduled cleanup task)
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
