package com.linkforge.urlshortener.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Entity representing a refresh token issued to a user on login.
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user this token was issued to.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // SHA-256 hash of the raw token. The raw token is never stored in the database.
    @Column(name = "hashed_token", nullable = false, unique = true)
    private String hashedToken;

    // IP address of the client that requested this token.
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // User-Agent of the client that requested this token.
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    // Date and time when this token expires.
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // True if this token has been revoked (e.g. on logout or password change).
    @Column(nullable = false)
    private Boolean revoked = false;

    // The time this token was revoked. Null if the token is still active.
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Sets the creation timestamp when the record is first saved.
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
