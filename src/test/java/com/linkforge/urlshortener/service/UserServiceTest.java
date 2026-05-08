package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.dto.request.ChangePasswordRequest;
import com.linkforge.urlshortener.dto.request.RegisterRequest;
import com.linkforge.urlshortener.dto.request.UpdateProfileRequest;
import com.linkforge.urlshortener.dto.response.UserResponse;
import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.exception.auth.InvalidCredentialsException;
import com.linkforge.urlshortener.exception.input.InvalidRequestException;
import com.linkforge.urlshortener.exception.resource.DuplicateResourceException;
import com.linkforge.urlshortener.exception.resource.ResourceNotFoundException;
import com.linkforge.urlshortener.repository.UrlRepository;
import com.linkforge.urlshortener.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Unit tests for UserService - repositories and encoder are mocked
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john_doe");
        testUser.setEmail("john@example.com");
        testUser.setHashedPassword("$2a$10$hashedpassword");
    }

    // ==========================================
    // register() tests
    // ==========================================

    @Test
    void register_withValidData_createsUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse response = userService.register(request);

        assertThat(response.getUsername()).isEqualTo("john_doe");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        // Password must be hashed before saving — raw password must never be stored
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_withDuplicateEmail_throwsDuplicateResourceException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withDuplicateUsername_throwsDuplicateResourceException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("john_doe")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any());
    }

    // ==========================================
    // getProfile() tests
    // ==========================================

    @Test
    void getProfile_withValidUser_returnsUserResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse response = userService.getProfile(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("john_doe");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void getProfile_withNonExistentUser_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ==========================================
    // updateProfile() tests
    // ==========================================

    @Test
    void updateProfile_withValidData_returnsUpdatedProfile() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("newemail@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse response = userService.updateProfile(1L, request);

        assertThat(response).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfile_withTakenEmail_throwsDuplicateResourceException() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("taken@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void updateProfile_withNoFieldsProvided_throwsInvalidRequestException() {
        UpdateProfileRequest request = new UpdateProfileRequest();

        assertThatThrownBy(() -> userService.updateProfile(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("At least one field");

        verify(userRepository, never()).findById(any());
    }

    // ==========================================
    // changePassword() tests
    // Token revocation is handled by AuthService, not UserService.
    // ==========================================

    @Test
    void changePassword_withCorrectCurrentPassword_updatesHash() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", testUser.getHashedPassword())).thenReturn(true);
        when(passwordEncoder.matches("newPassword123", testUser.getHashedPassword())).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$10$newhash");

        userService.changePassword(1L, request);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_withWrongCurrentPassword_throwsInvalidCredentialsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getHashedPassword())).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_withSamePassword_throwsInvalidRequestException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("samePassword");
        request.setNewPassword("samePassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("samePassword", testUser.getHashedPassword())).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("different from the current password");
    }

    // ==========================================
    // getStats() tests
    // ==========================================

    @Test
    void getStats_returnsCorrectAggregates() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(urlRepository.countByUserId(1L)).thenReturn(10L);
        when(urlRepository.sumTotalClicksByUserId(1L)).thenReturn(500L);
        when(urlRepository.countByUserIdAndIsActiveTrue(1L)).thenReturn(7L);
        when(urlRepository.countByUserIdAndIsActiveFalse(1L)).thenReturn(3L);
        when(urlRepository.countExpiredUrlsByUserId(eq(1L), any(LocalDateTime.class))).thenReturn(2L);

        var stats = userService.getStats(1L);

        assertThat(stats.getTotalUrls()).isEqualTo(10L);
        assertThat(stats.getTotalClicks()).isEqualTo(500L);
        assertThat(stats.getActiveUrls()).isEqualTo(7L);
        assertThat(stats.getInactiveUrls()).isEqualTo(3L);
        assertThat(stats.getExpiredUrls()).isEqualTo(2L);
    }

    @Test
    void getStats_withNonExistentUser_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getStats(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ==========================================
    // deleteAccount() tests
    // ==========================================

    @Test
    void deleteAccount_removesUserAndAllData() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.deleteAccount(1L);

        verify(userRepository).delete(testUser);
    }
}
