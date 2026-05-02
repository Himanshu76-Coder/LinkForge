package com.linkforge.urlshortener.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// Returns a JSON 403 response when an authenticated user accesses a resource they are not allowed to
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        // Build standardized error response matching PRD error envelope
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 403);
        body.put("success", false);
        body.put("message", "You do not have permission to perform this action");

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", "ACCESS_DENIED");
        error.put("details", null);
        body.put("error", error);

        body.put("timestamp", LocalDateTime.now().toString());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
