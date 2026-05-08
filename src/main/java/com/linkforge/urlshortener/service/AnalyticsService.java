package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.dto.response.ClickLogResponse;
import com.linkforge.urlshortener.dto.response.ClickStatisticsResponse;
import com.linkforge.urlshortener.entity.ClickLog;
import com.linkforge.urlshortener.entity.Url;
import com.linkforge.urlshortener.exception.input.InvalidRequestException;
import com.linkforge.urlshortener.exception.resource.ResourceNotFoundException;
import com.linkforge.urlshortener.repository.ClickLogRepository;
import com.linkforge.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// Service handling click analytics: statistics, paginated logs, and recent clicks
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UrlRepository urlRepository;
    private final ClickLogRepository clickLogRepository;

    // Allowed sort fields for click log pagination
    private static final List<String> ALLOWED_SORT_FIELDS = List.of("clickedAt", "ipAddress");

    // Max page size for click log pagination
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    // Returns aggregated click statistics (total, today, week, month) for a URL.
    public ClickStatisticsResponse getClickStatistics(Long urlId, Long userId) {
        Url url = findUrlAndVerifyOwnership(urlId, userId);

        ClickStatisticsResponse stats = new ClickStatisticsResponse();

        // Total clicks from the denormalized counter
        stats.setTotalClicks(url.getTotalClicks());

        // Today's clicks: from midnight today to end of today
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfToday = startOfToday.plusDays(1);
        stats.setTodayClicks(clickLogRepository.countClicksByDateRange(urlId, startOfToday, endOfToday));

        // Last 7 calendar days: from midnight 7 days ago to end of today
        LocalDateTime startOf7DaysAgo = LocalDateTime.now().toLocalDate().minusDays(6).atStartOfDay();
        stats.setWeekClicks(clickLogRepository.countClicksByDateRange(urlId, startOf7DaysAgo, endOfToday));

        // Last 30 calendar days: from midnight 30 days ago to end of today
        LocalDateTime startOf30DaysAgo = LocalDateTime.now().toLocalDate().minusDays(29).atStartOfDay();
        stats.setMonthClicks(clickLogRepository.countClicksByDateRange(urlId, startOf30DaysAgo, endOfToday));

        return stats;
    }

    // Returns paginated click logs for a URL, sorted and clamped to MAX_PAGE_SIZE.
    public Page<ClickLogResponse> getClickLogs(Long urlId, Long userId,
                                                int page, int size,
                                                String sortBy, String sortDir) {
        // Validate sortBy against the whitelist to prevent JPA errors from arbitrary field names
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new InvalidRequestException(
                    "Invalid sortBy value. Allowed values: clickedAt, ipAddress");
        }

        // Clamp page size to prevent unbounded queries
        int clampedSize = Math.min(size, MAX_PAGE_SIZE);
        if (clampedSize <= 0) clampedSize = DEFAULT_PAGE_SIZE;

        // Spring Data throws IllegalArgumentException for page < 0, so reject it early
        if (page < 0) {
            throw new InvalidRequestException("Page number must not be negative");
        }

        findUrlAndVerifyOwnership(urlId, userId);
        Sort sort = "ASC".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, clampedSize, sort);
        return clickLogRepository.findByUrlId(urlId, pageable)
                .map(this::mapToClickLogResponse);
    }

    // Returns the 10 most recent click log entries for a URL.
    public List<ClickLogResponse> getRecentClicks(Long urlId, Long userId) {
        findUrlAndVerifyOwnership(urlId, userId);
        return clickLogRepository.findTop10ByUrlIdOrderByClickedAtDesc(urlId)
                .stream()
                .map(this::mapToClickLogResponse)
                .toList();
    }

    // Returns 404 for both "not found" and "wrong owner" to avoid leaking resource existence.
    private Url findUrlAndVerifyOwnership(Long urlId, Long userId) {
        return urlRepository.findByIdAndUserId(urlId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with id: " + urlId));
    }

    // Maps a ClickLog entity to a ClickLogResponse DTO.
    private ClickLogResponse mapToClickLogResponse(ClickLog clickLog) {
        return new ClickLogResponse(
                clickLog.getId(),
                clickLog.getIpAddress(),
                clickLog.getUserAgent(),
                clickLog.getReferrer(),
                clickLog.getClickedAt()
        );
    }
}
