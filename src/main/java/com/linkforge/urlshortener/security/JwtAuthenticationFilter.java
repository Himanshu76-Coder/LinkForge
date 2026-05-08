package com.linkforge.urlshortener.security;

import com.linkforge.urlshortener.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Intercepts every request, validates the JWT, and sets the User entity as the authentication principal
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Extract the Authorization header
        String authHeader = request.getHeader("Authorization");

        // Skip filter if no Bearer token is present
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the raw token from the header
        String token = authHeader.substring(7);

        try {
            // Validate token signature and expiry
            if (!jwtUtil.validateToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Only set authentication if not already set in this request
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // Look up the user by ID from the token claim — faster than a username lookup.
                // If the user was deleted after the token was issued, the exception is caught below.
                Long userId = jwtUtil.extractUserId(token);
                User user = userDetailsService.loadUserEntityById(userId);

                // Build the authentication token with the User entity as the principal
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(user, null, List.of());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Store authentication in the security context for this request
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (UsernameNotFoundException ex) {
            // User was deleted after the JWT was issued — treat the request as unauthenticated
            log.debug("JWT references a user that no longer exists: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            // Unexpected error during token processing — clear context and let Spring return 401
            log.warn("JWT authentication failed: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
