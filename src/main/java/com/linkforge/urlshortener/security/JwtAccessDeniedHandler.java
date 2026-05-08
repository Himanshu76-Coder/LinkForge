package com.linkforge.urlshortener.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// Returns a JSON 403 response when an authenticated user accesses a resource they don't own
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    // Use Spring's configured ObjectMapper so LocalDateTime serializes consistently with the rest of the API
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 403);
        body.put("success", false);
        body.put("message", "You do not have permission to perform this action");

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", "ACCESS_DENIED");
        error.put("details", null);
        body.put("error", error);

        body.put("timestamp", LocalDateTime.now());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
