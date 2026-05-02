package com.linkforge.urlshortener.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO for changing the authenticated user's password
@Getter
@Setter
@NoArgsConstructor
public class ChangePasswordRequest {

    // Must match the stored BCrypt hash - verified in service layer
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    // New password must meet minimum length requirement per PRD BR-91
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be at least 8 characters")
    private String newPassword;
}
