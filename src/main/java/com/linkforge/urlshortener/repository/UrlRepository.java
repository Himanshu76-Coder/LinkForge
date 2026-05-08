package com.linkforge.urlshortener.repository;

import com.linkforge.urlshortener.entity.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Repository for Url entity database operations
@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    // Find URL by its short code (used for redirection and lookup)
    Optional<Url> findByShortCode(String shortCode);

    // Find URL by ID and owner in one query - avoids lazy-loading User just to check ownership
    Optional<Url> findByIdAndUserId(Long id, Long userId);

    // Finds URLs for export ordered by createdAt DESC, capped at EXPORT_ROW_CAP rows via Pageable.
    @Query("SELECT u FROM Url u WHERE u.user.id = :userId ORDER BY u.createdAt DESC")
    List<Url> findByUserIdForExport(@Param("userId") Long userId, Pageable pageable);

    // Check if a short code is already in use
    boolean existsByShortCode(String shortCode);

    // Check if the same original URL already exists for this user (duplicate detection - PRD BR-44)
    Optional<Url> findByUserIdAndOriginalUrl(Long userId, String originalUrl);

    // Get all URLs for a user with pagination (default listing)
    Page<Url> findByUserId(Long userId, Pageable pageable);

    // Filter by active status
    Page<Url> findByUserIdAndIsActive(Long userId, Boolean isActive, Pageable pageable);

    // Filter by expiry range
    @Query("SELECT u FROM Url u WHERE u.user.id = :userId AND u.expiresAt BETWEEN :from AND :to")
    Page<Url> findByUserIdAndExpiresAtBetween(@Param("userId") Long userId,
                                               @Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to,
                                               Pageable pageable);

    // Full-text search across originalUrl, shortCode, title, and description.
    // Relies on the DB collation for case-insensitive matching instead of LOWER().
    @Query("SELECT u FROM Url u WHERE u.user.id = :userId AND (" +
           "u.originalUrl LIKE CONCAT('%', :q, '%') OR " +
           "u.shortCode LIKE CONCAT('%', :q, '%') OR " +
           "u.title LIKE CONCAT('%', :q, '%') OR " +
           "u.description LIKE CONCAT('%', :q, '%'))")
    Page<Url> searchByUserId(@Param("userId") Long userId, @Param("q") String q, Pageable pageable);

    // Count total URLs for a user (used in stats)
    long countByUserId(Long userId);

    // Count active URLs for a user
    long countByUserIdAndIsActiveTrue(Long userId);

    // Count inactive URLs for a user
    long countByUserIdAndIsActiveFalse(Long userId);

    // Sum total clicks across all URLs for a user (used in stats)
    @Query("SELECT COALESCE(SUM(u.totalClicks), 0) FROM Url u WHERE u.user.id = :userId")
    long sumTotalClicksByUserId(@Param("userId") Long userId);

    // Count all URLs (active or inactive) that have passed their expiration date (used in stats).
    @Query("SELECT COUNT(u) FROM Url u WHERE u.user.id = :userId AND u.expiresAt IS NOT NULL AND u.expiresAt < :now")
    long countExpiredUrlsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    // Atomically increments the click counter only when the click limit has not been reached.
    // Returns 1 on success, 0 if the limit was already hit (no limit = always increments).
    @Modifying
    @Transactional
    @Query("UPDATE Url u SET u.totalClicks = u.totalClicks + 1 " +
           "WHERE u.id = :urlId AND (u.clickLimit IS NULL OR u.totalClicks < u.clickLimit)")
    int incrementClickCountIfAllowed(@Param("urlId") Long urlId);
}
