package com.linkforge.urlshortener.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Entity representing a registered user account.
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique username used for login and display.
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // Unique email address used for authentication.
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    // BCrypt hash of the user's password — the raw password is never stored.
    @Column(name = "hashed_password", nullable = false)
    private String hashedPassword;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // All URLs owned by this user. Deleting the user cascades to all their URLs.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Url> urls = new ArrayList<>();

    // All refresh tokens issued to this user across all devices.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefreshToken> refreshTokens = new ArrayList<>();

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
