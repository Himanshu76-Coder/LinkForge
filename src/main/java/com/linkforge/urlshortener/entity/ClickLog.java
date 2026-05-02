package com.linkforge.urlshortener.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Entity representing a single click event on a shortened URL
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "click_logs")
public class ClickLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The URL that was clicked
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_id", nullable = false)
    private Url url;

    // IP address of the visitor (IPv4 or IPv6)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // Browser/device user agent string
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    // The page the visitor came from
    @Column(length = 500)
    private String referrer;

    // Two-letter ISO country code (e.g. "US", "GB")
    @Column(length = 2)
    private String country;

    @Column(name = "clicked_at", nullable = false, updatable = false)
    private LocalDateTime clickedAt;

    // Automatically set click timestamp on save
    @PrePersist
    protected void onCreate() {
        clickedAt = LocalDateTime.now();
    }
}
