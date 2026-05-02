package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.dto.response.ClickLogResponse;
import com.linkforge.urlshortener.dto.response.ClickStatisticsResponse;
import com.linkforge.urlshortener.entity.ClickLog;
import com.linkforge.urlshortener.entity.Url;
import com.linkforge.urlshortener.exception.ResourceNotFoundException;
import com.linkforge.urlshortener.exception.UnauthorizedException;
import com.linkforge.urlshortener.repository.ClickLogRepository;
import com.linkforge.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Service handling click analytics: statistics, paginated logs, and recent clicks
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UrlRepository urlRepository;
    private final ClickLogRepository clickLogRepository;

    // Get aggregated click statistics for a URL
    public ClickStatisticsResponse getClickStatistics(Long urlId, Long userId) {
        Url url = findUrlAndVerifyOwnership(urlId, userId);

        ClickStatisticsResponse stats = new ClickStatisticsResponse();

        // Total clicks from the denormalized counter
        stats.setTotalClicks(url.getTotalClicks());

        // Today's clicks (midnight to now)
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfToday = startOfToday.plusDays(1);
        stats.setTodayClicks(clickLogRepository.countClicksByDateRange(urlId, startOfToday, endOfToday));

        // Last 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        stats.setWeekClicks(clickLogRepository.countClicksByDateRange(urlId, sevenDaysAgo, LocalDateTime.now()));

        // Last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        stats.setMonthClicks(clickLogRepository.countClicksByDateRange(urlId, thirtyDaysAgo, LocalDateTime.now()));

        // Clicks grouped by country
        List<Object[]> countryData = clickLogRepository.getClicksByCountry(urlId);
        Map<String, Long> clicksByCountry = new HashMap<>();
        for (Object[] row : countryData) {
            String country = (String) row[0];
            Long count = (Long) row[1];
            // Skip null country entries (clicks with no geo data)
            if (country != null) {
                clicksByCountry.put(country, count);
            }
        }
        stats.setClicksByCountry(clicksByCountry);

        return stats;
    }

    // Get paginated click logs for a URL
    public Page<ClickLogResponse> getClickLogs(Long urlId, Long userId, Pageable pageable) {
        findUrlAndVerifyOwnership(urlId, userId);
        return clickLogRepository.findByUrlId(urlId, pageable)
                .map(this::mapToClickLogResponse);
    }

    // Get the 10 most recent clicks for a URL
    public List<ClickLogResponse> getRecentClicks(Long urlId, Long userId) {
        findUrlAndVerifyOwnership(urlId, userId);
        return clickLogRepository.findTop10ByUrlIdOrderByClickedAtDesc(urlId)
                .stream()
                .map(this::mapToClickLogResponse)
                .toList();
    }

    // Find URL by ID and verify it belongs to the requesting user
    private Url findUrlAndVerifyOwnership(Long urlId, Long userId) {
        Url url = urlRepository.findById(urlId)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with id: " + urlId));

        if (!url.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to view analytics for this URL");
        }

        return url;
    }

    // Map ClickLog entity to ClickLogResponse DTO
    private ClickLogResponse mapToClickLogResponse(ClickLog clickLog) {
        return new ClickLogResponse(
                clickLog.getId(),
                clickLog.getIpAddress(),
                clickLog.getUserAgent(),
                clickLog.getReferrer(),
                clickLog.getCountry(),
                clickLog.getClickedAt()
        );
    }
}
