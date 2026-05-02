package com.linkforge.urlshortener.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO for returning aggregate account statistics per PRD Section 5.27
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {

    // Total number of short URLs created by the user
    private Long totalUrls;

    // Sum of total_clicks across all user's URLs
    private Long totalClicks;

    // Number of URLs with is_active = true
    private Long activeUrls;

    // Number of URLs with is_active = false
    private Long inactiveUrls;

    // Number of URLs where expires_at is in the past
    private Long expiredUrls;
}
