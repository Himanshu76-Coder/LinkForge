package com.linkforge.urlshortener.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Entity representing a shortened URL created by a user.
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "urls")
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user who owns this short URL.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The short code or custom alias that appears in the shortened link.
    @Column(name = "short_code", nullable = false, unique = true, length = 50)
    private String shortCode;

    // The original long URL that visitors are redirected to.
    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    // Optional display title for the URL.
    @Column(length = 255)
    private String title;

    // Optional internal note or description for the URL.
    @Column(length = 500)
    private String description;

    // True if the user provided the alias manually, false if it was auto-generated.
    @Column(name = "is_custom_alias", nullable = false)
    private Boolean isCustomAlias = false;

    // Controls whether this link can be redirected. Inactive links return 410 Gone.
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Denormalized click counter kept in sync by an atomic DB UPDATE on each redirect.
    @Column(name = "total_clicks", nullable = false)
    private Long totalClicks = 0L;

    // Maximum number of allowed redirects. Null means unlimited.
    @Column(name = "click_limit")
    private Long clickLimit;

    // Date and time when this link expires. Null means it never expires.
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // All click events recorded for this URL. Deleting the URL cascades to its click logs.
    @OneToMany(mappedBy = "url", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClickLog> clickLogs = new ArrayList<>();

    // Sets both timestamps when the record is first created.
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // Updates the timestamp every time the record is modified.
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
