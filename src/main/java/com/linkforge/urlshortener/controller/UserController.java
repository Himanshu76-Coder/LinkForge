package com.linkforge.urlshortener.controller;

import com.linkforge.urlshortener.dto.request.ChangePasswordRequest;
import com.linkforge.urlshortener.dto.request.RegisterRequest;
import com.linkforge.urlshortener.dto.request.UpdateProfileRequest;
import com.linkforge.urlshortener.dto.response.ApiResponse;
import com.linkforge.urlshortener.dto.response.UserResponse;
import com.linkforge.urlshortener.dto.response.UserStatsResponse;
import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.service.UserService;
import com.linkforge.urlshortener.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// REST controller for user registration and profile management endpoints
@Tag(name = "User Profile", description = "Register, view, update, and delete user account")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // POST /api/v1/auth/register - public endpoint, no auth required
    @Operation(summary = "Register", description = "Create a new user account. No authentication required.")
    @SecurityRequirements
    @PostMapping("/api/v1/auth/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse userResponse = userService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("Registration successful", userResponse));
    }

    // GET /api/v1/users/me - get authenticated user's profile
    @Operation(summary = "Get profile", description = "Retrieve the authenticated user's profile. Password is never returned.")
    @GetMapping("/api/v1/users/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        User currentUser = SecurityUtil.getCurrentUser();
        UserResponse userResponse = userService.getProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", userResponse));
    }

    // PUT /api/v1/users/me - update username and/or email
    @Operation(summary = "Update profile", description = "Update username and/or email. At least one field must be provided.")
    @PutMapping("/api/v1/users/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        User currentUser = SecurityUtil.getCurrentUser();
        UserResponse userResponse = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", userResponse));
    }

    // PATCH /api/v1/users/me/password - change password
    @Operation(summary = "Change password", description = "Change password. Requires current password. Revokes all active sessions.")
    @PatchMapping("/api/v1/users/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        User currentUser = SecurityUtil.getCurrentUser();
        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    // GET /api/v1/users/me/stats - get account statistics
    @Operation(summary = "Get account statistics", description = "Returns aggregate stats: total URLs, total clicks, active/inactive/expired URL counts.")
    @GetMapping("/api/v1/users/me/stats")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getStats() {
        User currentUser = SecurityUtil.getCurrentUser();
        UserStatsResponse stats = userService.getStats(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", stats));
    }

    // DELETE /api/v1/users/me - permanently delete account and all data
    @Operation(summary = "Delete account", description = "Permanently delete the account and all associated URLs, click logs, and tokens.")
    @DeleteMapping("/api/v1/users/me")
    public ResponseEntity<Void> deleteAccount() {
        User currentUser = SecurityUtil.getCurrentUser();
        userService.deleteAccount(currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
