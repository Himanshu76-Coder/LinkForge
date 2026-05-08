package com.linkforge.urlshortener.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Entity representing a single click event recorded when a short URL is visited.
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "click_logs")
public class ClickLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The short URL that was visited.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_id", nullable = false)
    private Url url;

    // IP address of the visitor. Supports both IPv4 and IPv6 (max 45 chars).
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // Browser and device information from the User-Agent header.
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    // The page the visitor came from, taken from the Referer header.
    @Column(length = 500)
    private String referrer;

    @Column(name = "clicked_at", nullable = false, updatable = false)
    private LocalDateTime clickedAt;

    // Sets the click timestamp when the record is first saved.
    @PrePersist
    protected void onCreate() {
        clickedAt = LocalDateTime.now();
    }
}
