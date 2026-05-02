package com.linkforge.urlshortener.util;

import jakarta.servlet.http.HttpServletRequest;

// Utility for extracting client information from incoming HTTP requests
public class RequestContextUtil {

    // Extract the real client IP, handling proxies and load balancers
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For can contain multiple IPs; take the first (original client)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    // Extract User-Agent header, truncated to 255 characters
    public static String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > 255) {
            userAgent = userAgent.substring(0, 255);
        }
        return userAgent;
    }

    // Extract Referer header, truncated to 500 characters
    public static String getReferrer(HttpServletRequest request) {
        String referrer = request.getHeader("Referer");
        if (referrer != null && referrer.length() > 500) {
            referrer = referrer.substring(0, 500);
        }
        return referrer;
    }
}
