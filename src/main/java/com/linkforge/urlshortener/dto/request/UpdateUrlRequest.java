package com.linkforge.urlshortener.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// DTO for updating an existing URL - all fields are optional, only provided fields are updated
@Getter
@Setter
@NoArgsConstructor
public class UpdateUrlRequest {

    // Optional - update original URL
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    private String originalUrl;

    // Optional - update or change custom alias - PRD BR-31
    @Size(min = 3, max = 50, message = "Custom alias must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Custom alias can only contain letters, numbers, and hyphens")
    private String customAlias;

    // Optional - update title
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    // Optional - update description
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    // Optional - update expiration - must still be in the future - PRD BR-32
    private LocalDateTime expiresAt;

    // Optional - update click limit
    @Min(value = 1, message = "Click limit must be at least 1")
    private Long clickLimit;
}
