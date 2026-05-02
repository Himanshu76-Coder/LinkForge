package com.linkforge.urlshortener.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// DTO for creating a new shortened URL - PRD Section 5.1
@Getter
@Setter
@NoArgsConstructor
public class CreateUrlRequest {

    // Original URL is required - validated further in service layer
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    private String originalUrl;

    // Optional custom alias - 3-50 chars, alphanumeric and hyphens only - PRD BR-15
    @Size(min = 3, max = 50, message = "Custom alias must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Custom alias can only contain letters, numbers, and hyphens")
    private String customAlias;

    // Optional display title - PRD BR-66
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    // Optional description - PRD BR-67
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    // Optional expiration datetime - must be in the future - PRD BR-20
    private LocalDateTime expiresAt;

    // Optional maximum click count - must be >= 1 if provided - PRD BR-59
    @Min(value = 1, message = "Click limit must be at least 1")
    private Long clickLimit;
}
