package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.dto.request.ChangePasswordRequest;
import com.linkforge.urlshortener.dto.request.RegisterRequest;
import com.linkforge.urlshortener.dto.request.UpdateProfileRequest;
import com.linkforge.urlshortener.dto.response.UserResponse;
import com.linkforge.urlshortener.dto.response.UserStatsResponse;
import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.exception.auth.InvalidCredentialsException;
import com.linkforge.urlshortener.exception.input.InvalidRequestException;
import com.linkforge.urlshortener.exception.resource.DuplicateResourceException;
import com.linkforge.urlshortener.exception.resource.ResourceNotFoundException;
import com.linkforge.urlshortener.repository.UrlRepository;
import com.linkforge.urlshortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Handles all user account operations: registration, profile management, password change, and deletion.
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;
    private final PasswordEncoder passwordEncoder;

    // Registers a new user after checking that the username and email are not already taken.
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Reject if the username is already in use
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        // Reject if the email is already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        // Hash the password before saving — plain text passwords are never stored
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setHashedPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    // Returns the authenticated user's profile.
    public UserResponse getProfile(Long userId) {
        User user = findUserById(userId);
        return mapToUserResponse(user);
    }

    // Updates the user's username and/or email. At least one field must be provided.
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        // Reject the request early if neither field was provided
        if ((request.getUsername() == null || request.getUsername().isBlank()) &&
            (request.getEmail() == null || request.getEmail().isBlank())) {
            throw new InvalidRequestException("At least one field (username or email) must be provided");
        }

        User user = findUserById(userId);

        // Only update the username if it differs from the current one
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            if (!request.getUsername().equals(user.getUsername())) {
                // Ensure the new username is not already taken by another account
                if (userRepository.existsByUsername(request.getUsername())) {
                    throw new DuplicateResourceException("Username already exists");
                }
                user.setUsername(request.getUsername());
            }
        }

        // Only update the email if it differs from the current one
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equals(user.getEmail())) {
                // Ensure the new email is not already registered to another account
                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new DuplicateResourceException("Email already exists");
                }
                user.setEmail(request.getEmail());
            }
        }

        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    // Validates the current password and saves the new hashed password.
    // Token revocation is handled by AuthService after this returns successfully.
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        // Verify the submitted current password matches the stored hash
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getHashedPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // Reject if the new password is the same as the current one
        if (passwordEncoder.matches(request.getNewPassword(), user.getHashedPassword())) {
            throw new InvalidRequestException("New password must be different from the current password");
        }

        // Hash and persist the new password
        user.setHashedPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // Returns aggregate statistics for the user's account.
    public UserStatsResponse getStats(Long userId) {
        // Confirm the user exists before running the stat queries
        findUserById(userId);

        long totalUrls = urlRepository.countByUserId(userId);
        long totalClicks = urlRepository.sumTotalClicksByUserId(userId);
        long activeUrls = urlRepository.countByUserIdAndIsActiveTrue(userId);
        long inactiveUrls = urlRepository.countByUserIdAndIsActiveFalse(userId);
        // Count all URLs (active or inactive) whose expiry date is in the past
        long expiredUrls = urlRepository.countExpiredUrlsByUserId(userId, LocalDateTime.now());

        return new UserStatsResponse(totalUrls, totalClicks, activeUrls, inactiveUrls, expiredUrls);
    }

    // Permanently deletes the user account and all associated data.
    @Transactional
    public void deleteAccount(Long userId) {
        User user = findUserById(userId);
        // Cascade deletion removes all URLs, click logs, and refresh tokens for this user
        userRepository.delete(user);
    }

    // Looks up a user by ID and throws ResourceNotFoundException if not found.
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Maps a User entity to a UserResponse DTO. The password hash is never included.
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
