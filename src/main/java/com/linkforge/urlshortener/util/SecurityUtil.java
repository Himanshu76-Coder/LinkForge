package com.linkforge.urlshortener.util;

import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.exception.auth.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// Utility for accessing the currently authenticated user from the Spring Security context
public class SecurityUtil {

    // Get the authenticated User entity from the security context
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Ensure authentication exists and principal is our User entity
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof User)) {
            throw new UnauthorizedException("No authenticated user found");
        }

        return (User) principal;
    }

    // Get the authenticated user's ID
    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
