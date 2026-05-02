package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.dto.request.ChangePasswordRequest;
import com.linkforge.urlshortener.dto.request.RegisterRequest;
import com.linkforge.urlshortener.dto.request.UpdateProfileRequest;
import com.linkforge.urlshortener.dto.response.UserResponse;
import com.linkforge.urlshortener.dto.response.UserStatsResponse;
import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.exception.DuplicateResourceException;
import com.linkforge.urlshortener.exception.InvalidCredentialsException;
import com.linkforge.urlshortener.exception.InvalidRequestException;
import com.linkforge.urlshortener.exception.ResourceNotFoundException;
import com.linkforge.urlshortener.repository.UrlRepository;
import com.linkforge.urlshortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Service handling all user management business logic
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;
    private final PasswordEncoder passwordEncoder;

    // @Lazy breaks the circular dependency: UserService -> RefreshTokenService -> PasswordEncoder <- UserService
    @Lazy
    private final RefreshTokenService refreshTokenService;

    // Register a new user account - PRD Section 5.20
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Check username uniqueness - PRD BR-71
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        // Check email uniqueness - PRD BR-70
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        // Build and save new user with hashed password - PRD BR-73
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setHashedPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    // Get the authenticated user's profile - PRD Section 5.24
    public UserResponse getProfile(Long userId) {
        User user = findUserById(userId);
        return mapToUserResponse(user);
    }

    // Update username and/or email - PRD Section 5.25
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        // Reject request upfront if neither field was provided
        if ((request.getUsername() == null || request.getUsername().isBlank()) &&
            (request.getEmail() == null || request.getEmail().isBlank())) {
            throw new InvalidRequestException("At least one field (username or email) must be provided");
        }

        User user = findUserById(userId);

        // Update username if provided and different from current
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            if (!request.getUsername().equals(user.getUsername())) {
                // Check new username is not already taken - PRD BR-88
                if (userRepository.existsByUsername(request.getUsername())) {
                    throw new DuplicateResourceException("Username already exists");
                }
                user.setUsername(request.getUsername());
            }
        }

        // Update email if provided and different from current
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equals(user.getEmail())) {
                // Check new email is not already taken - PRD BR-87
                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new DuplicateResourceException("Email already exists");
                }
                user.setEmail(request.getEmail());
            }
        }

        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    // Change the authenticated user's password - PRD Section 5.26
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        // Verify current password matches stored hash - PRD BR-90
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getHashedPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // Prevent setting the same password again
        if (passwordEncoder.matches(request.getNewPassword(), user.getHashedPassword())) {
            throw new InvalidRequestException("New password must be different from the current password");
        }

        // Hash and save new password - PRD BR-92
        user.setHashedPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens so existing sessions must re-login - PRD Section 10.3
        refreshTokenService.revokeAllUserTokens(userId);
    }

    // Get aggregate account statistics - PRD Section 5.27
    public UserStatsResponse getStats(Long userId) {
        // Verify user exists
        findUserById(userId);

        // Count total URLs
        long totalUrls = urlRepository.countByUserId(userId);

        // Sum all clicks across user's URLs
        long totalClicks = urlRepository.sumTotalClicksByUserId(userId);

        // Count active URLs (is_active = true)
        long activeUrls = urlRepository.countByUserIdAndIsActiveTrue(userId);

        // Count inactive URLs (is_active = false)
        long inactiveUrls = urlRepository.countByUserIdAndIsActiveFalse(userId);

        // Count expired URLs (expires_at is in the past and not null)
        long expiredUrls = urlRepository.countExpiredUrlsByUserId(userId, LocalDateTime.now());

        return new UserStatsResponse(totalUrls, totalClicks, activeUrls, inactiveUrls, expiredUrls);
    }

    // Permanently delete user account and all associated data - PRD Section 5.28
    @Transactional
    public void deleteAccount(Long userId) {
        User user = findUserById(userId);
        // Cascade deletion handles: click_logs -> urls -> refresh_tokens -> users
        userRepository.delete(user);
    }

    // Find user by ID or throw ResourceNotFoundException
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Map User entity to UserResponse DTO - password hash is never included
    public UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
