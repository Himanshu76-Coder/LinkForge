package com.linkforge.urlshortener.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// Returns a JSON 401 response when an unauthenticated request hits a protected endpoint
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        // Build standardized error response matching PRD error envelope
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 401);
        body.put("success", false);
        body.put("message", "Authentication required. Please login to access this resource");

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", "AUTHENTICATION_REQUIRED");
        error.put("details", null);
        body.put("error", error);

        body.put("timestamp", LocalDateTime.now().toString());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
