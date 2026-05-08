package com.linkforge.urlshortener.util;

import jakarta.servlet.http.HttpServletRequest;

// Utility for extracting client information from incoming HTTP requests
public class RequestContextUtil {

    // Returns the real client IP. Trusts X-Forwarded-For only when the connection
    // comes from a local proxy (127.0.0.1 or ::1) to prevent IP spoofing.
    public static String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        // Only trust proxy headers when the direct connection is from localhost
        boolean fromLocalProxy = "127.0.0.1".equals(remoteAddr) || "::1".equals(remoteAddr);

        if (fromLocalProxy) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank() && !"unknown".equalsIgnoreCase(forwarded)) {
                // X-Forwarded-For may contain multiple IPs; the first is the original client
                return forwarded.split(",")[0].trim();
            }

            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) {
                return realIp.trim();
            }
        }

        return remoteAddr;
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
