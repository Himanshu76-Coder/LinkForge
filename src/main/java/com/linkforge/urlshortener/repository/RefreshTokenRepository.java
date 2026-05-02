package com.linkforge.urlshortener.repository;

import com.linkforge.urlshortener.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// Repository for RefreshToken entity database operations
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Get all active (non-revoked) tokens for a specific user
    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    // Get all non-revoked, non-expired tokens - used for raw token matching on validate/logout
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findAllActiveTokens(@Param("now") LocalDateTime now);

    // Revoke all active tokens for a user (used on logout-all and password change)
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :revokedAt WHERE rt.user.id = :userId AND rt.revoked = false")
    void revokeAllUserTokens(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);

    // Delete all expired tokens (called by scheduled cleanup task)
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
