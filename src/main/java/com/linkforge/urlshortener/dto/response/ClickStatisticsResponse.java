package com.linkforge.urlshortener.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

// DTO for returning aggregated click statistics for a short URL
@Getter
@Setter
@NoArgsConstructor
public class ClickStatisticsResponse {

    // Total clicks from the denormalized counter on the urls table
    private Long totalClicks;

    // Clicks recorded today (midnight to now)
    private Long todayClicks;

    // Clicks in the last 7 days
    private Long weekClicks;

    // Clicks in the last 30 days
    private Long monthClicks;

    // Click counts grouped by ISO 3166-1 alpha-2 country code
    private Map<String, Long> clicksByCountry;
}
