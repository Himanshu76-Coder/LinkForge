package com.linkforge.urlshortener.repository;

import com.linkforge.urlshortener.entity.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Repository for Url entity database operations
@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    // Find URL by its short code (used for redirection and lookup)
    Optional<Url> findByShortCode(String shortCode);

    // Find all URLs for a user without pagination (used for export)
    List<Url> findByUserId(Long userId, Sort sort);

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

    // Search across originalUrl, shortCode, title, description - PRD BR-54
    @Query("SELECT u FROM Url u WHERE u.user.id = :userId AND (" +
           "LOWER(u.originalUrl) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.shortCode) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.description) LIKE LOWER(CONCAT('%', :q, '%')))")
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

    // Count URLs that have passed their expiration date (used in stats)
    @Query("SELECT COUNT(u) FROM Url u WHERE u.user.id = :userId AND u.expiresAt IS NOT NULL AND u.expiresAt < :now")
    long countExpiredUrlsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    // Increment click count atomically without loading the entity
    @Modifying
    @Query("UPDATE Url u SET u.totalClicks = u.totalClicks + 1 WHERE u.id = :urlId")
    void incrementClickCount(@Param("urlId") Long urlId);
}
