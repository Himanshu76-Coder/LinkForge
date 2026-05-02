package com.linkforge.urlshortener.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Entity representing a shortened URL created by a user
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "urls")
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user who created this short URL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Unique short code or custom alias used in the short URL
    @Column(name = "short_code", nullable = false, unique = true, length = 50)
    private String shortCode;

    // The original long URL to redirect to
    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    // Optional display title for the URL - PRD BR-66
    @Column(length = 255)
    private String title;

    // Optional internal note or description - PRD BR-67
    @Column(length = 500)
    private String description;

    // True if user provided the alias, false if auto-generated - PRD Section 9.6
    @Column(name = "is_custom_alias", nullable = false)
    private Boolean isCustomAlias = false;

    // Whether this URL is active and can be redirected - PRD BR-36
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Denormalized total redirect count - PRD Section 9.6
    @Column(name = "total_clicks", nullable = false)
    private Long totalClicks = 0L;

    // Optional maximum number of clicks allowed (null = unlimited) - PRD BR-59
    @Column(name = "click_limit")
    private Long clickLimit;

    // Optional expiration datetime (null = never expires) - PRD BR-19
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // One URL can have many click log entries
    @OneToMany(mappedBy = "url", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClickLog> clickLogs = new ArrayList<>();

    // Automatically set timestamps on first save
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // Automatically update timestamp on every update
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
