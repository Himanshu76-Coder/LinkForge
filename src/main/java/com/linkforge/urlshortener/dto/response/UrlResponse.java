package com.linkforge.urlshortener.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// DTO for returning URL details in all URL responses - PRD Section 8.5
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UrlResponse {

    private Long id;
    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private String title;
    private String description;

    // True if the user provided the alias, false if auto-generated
    private Boolean isCustomAlias;

    private Boolean isActive;
    private Long totalClicks;
    private Long clickLimit;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
