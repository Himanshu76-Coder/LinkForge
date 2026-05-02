package com.linkforge.urlshortener.controller;

import com.linkforge.urlshortener.dto.response.ApiResponse;
import com.linkforge.urlshortener.dto.response.ClickLogResponse;
import com.linkforge.urlshortener.dto.response.ClickStatisticsResponse;
import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.service.AnalyticsService;
import com.linkforge.urlshortener.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// REST controller for click analytics endpoints
@Tag(name = "Analytics", description = "Click statistics and click log retrieval for short URLs")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // GET /api/v1/analytics/urls/{urlId}/statistics - aggregated click stats
    @Operation(summary = "Get click statistics", description = "Returns total, today, week, and month click counts plus breakdown by country.")
    @GetMapping("/urls/{urlId}/statistics")
    public ResponseEntity<ApiResponse<ClickStatisticsResponse>> getClickStatistics(
            @PathVariable Long urlId) {

        User currentUser = SecurityUtil.getCurrentUser();
        ClickStatisticsResponse stats = analyticsService.getClickStatistics(urlId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", stats));
    }

    // GET /api/v1/analytics/urls/{urlId}/clicks - paginated click logs
    @Operation(summary = "Get click logs", description = "Returns paginated click log entries for a short URL.")
    @GetMapping("/urls/{urlId}/clicks")
    public ResponseEntity<ApiResponse<Page<ClickLogResponse>>> getClickLogs(
            @PathVariable Long urlId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "clickedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        User currentUser = SecurityUtil.getCurrentUser();

        Sort sort = "ASC".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ClickLogResponse> clickLogs = analyticsService.getClickLogs(urlId, currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Click logs retrieved successfully", clickLogs));
    }

    // GET /api/v1/analytics/urls/{urlId}/recent-clicks - last 10 clicks
    @Operation(summary = "Get recent clicks", description = "Returns the 10 most recent click log entries for a short URL.")
    @GetMapping("/urls/{urlId}/recent-clicks")
    public ResponseEntity<ApiResponse<List<ClickLogResponse>>> getRecentClicks(
            @PathVariable Long urlId) {

        User currentUser = SecurityUtil.getCurrentUser();
        List<ClickLogResponse> recentClicks = analyticsService.getRecentClicks(urlId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Recent clicks retrieved successfully", recentClicks));
    }
}
