package com.linkforge.urlshortener.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// Returns a JSON 401 response when an unauthenticated request reaches a protected endpoint
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // Use Spring's configured ObjectMapper so LocalDateTime serializes consistently with the rest of the API
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 401);
        body.put("success", false);
        body.put("message", "Authentication required. Please login to access this resource");

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", "AUTHENTICATION_REQUIRED");
        error.put("details", null);
        body.put("error", error);

        body.put("timestamp", LocalDateTime.now());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
