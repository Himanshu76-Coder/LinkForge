package com.linkforge.urlshortener.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// DTO for returning individual click log data in analytics responses
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClickLogResponse {

    private Long id;
    private String ipAddress;
    private String userAgent;
    private String referrer;
    private String country;
    private LocalDateTime clickedAt;
}
