package com.linkforge.urlshortener.repository;

import com.linkforge.urlshortener.entity.ClickLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// Repository for ClickLog entity database operations
@Repository
public interface ClickLogRepository extends JpaRepository<ClickLog, Long> {

    // Get all click logs for a URL with pagination
    Page<ClickLog> findByUrlId(Long urlId, Pageable pageable);

    // Get the 10 most recent clicks for a URL
    List<ClickLog> findTop10ByUrlIdOrderByClickedAtDesc(Long urlId);

    // Count clicks for a URL within a specific date range
    @Query("SELECT COUNT(c) FROM ClickLog c WHERE c.url.id = :urlId AND c.clickedAt BETWEEN :startDate AND :endDate")
    long countClicksByDateRange(@Param("urlId") Long urlId,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);
}
