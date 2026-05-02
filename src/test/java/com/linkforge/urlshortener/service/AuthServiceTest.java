package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.dto.request.LoginRequest;
import com.linkforge.urlshortener.dto.response.AuthResponse;
import com.linkforge.urlshortener.entity.RefreshToken;
import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.exception.InvalidCredentialsException;
import com.linkforge.urlshortener.exception.TokenException;
import com.linkforge.urlshortener.repository.UserRepository;
import com.linkforge.urlshortener.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Unit tests for AuthService - all dependencies are mocked
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpirationMs", 900000L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john_doe");
        testUser.setEmail("john@example.com");
        testUser.setHashedPassword("$2a$10$hashedpassword");
    }

    // ==========================================
    // login() tests
    // ==========================================

    @Test
    void login_withValidCredentials_returnsTokens() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getHashedPassword())).thenReturn(true);
        when(jwtUtil.generateAccessToken("john_doe", 1L)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any(), anyString(), anyString())).thenReturn("refresh-token");

        AuthResponse response = authService.login(request, "127.0.0.1", "Mozilla/5.0");

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900L);
    }

    @Test
    void login_withNonExistentEmail_throwsInvalidCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "Mozilla/5.0"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("wrongpassword");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getHashedPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "Mozilla/5.0"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    // ==========================================
    // refresh() tests
    // ==========================================

    @Test
    void refresh_withValidToken_returnsNewAccessToken() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(testUser);
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        when(refreshTokenService.validateRefreshToken("valid-refresh-token")).thenReturn(refreshToken);
        when(jwtUtil.generateAccessToken("john_doe", 1L)).thenReturn("new-access-token");

        AuthResponse response = authService.refresh("valid-refresh-token");

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("valid-refresh-token");
    }

    @Test
    void refresh_withExpiredToken_throwsTokenException() {
        when(refreshTokenService.validateRefreshToken("expired-token"))
                .thenThrow(new TokenException("Refresh token has expired"));

        assertThatThrownBy(() -> authService.refresh("expired-token"))
                .isInstanceOf(TokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void refresh_withRevokedToken_throwsTokenException() {
        when(refreshTokenService.validateRefreshToken("revoked-token"))
                .thenThrow(new TokenException("Refresh token not found or already revoked"));

        assertThatThrownBy(() -> authService.refresh("revoked-token"))
                .isInstanceOf(TokenException.class)
                .hasMessageContaining("revoked");
    }

    // ==========================================
    // logout() tests
    // ==========================================

    @Test
    void logout_withValidToken_revokesRefreshToken() {
        doNothing().when(refreshTokenService).revokeToken("valid-refresh-token");

        authService.logout("valid-refresh-token");

        verify(refreshTokenService).revokeToken("valid-refresh-token");
    }

    // ==========================================
    // logoutAll() tests
    // ==========================================

    @Test
    void logoutAll_revokesAllUserTokens() {
        doNothing().when(refreshTokenService).revokeAllUserTokens(1L);

        authService.logoutAll(1L);

        verify(refreshTokenService).revokeAllUserTokens(1L);
    }
}
