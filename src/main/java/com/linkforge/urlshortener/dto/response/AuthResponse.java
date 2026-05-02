package com.linkforge.urlshortener.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO for authentication response - returned on login and token refresh
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    // Short-lived JWT access token (15 minutes)
    private String accessToken;

    // Long-lived refresh token (7 days) - only returned on login, not on refresh
    private String refreshToken;

    // Always "Bearer"
    private String tokenType;

    // Access token TTL in seconds
    private long expiresIn;
}
