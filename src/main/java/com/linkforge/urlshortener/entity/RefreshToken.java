package com.linkforge.urlshortener.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Entity representing a refresh token issued to a user session
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user this token belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // BCrypt hashed version of the token (raw token is never stored)
    @Column(name = "hashed_token", nullable = false, unique = true)
    private String hashedToken;

    // IP address from which the token was issued
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // User agent from which the token was issued
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    // When this token expires
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Whether this token has been revoked (logout)
    @Column(nullable = false)
    private Boolean revoked = false;

    // When this token was revoked (null if still active)
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Automatically set created timestamp on save
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
