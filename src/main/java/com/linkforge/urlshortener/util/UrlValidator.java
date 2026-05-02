package com.linkforge.urlshortener.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

// Utility for validating URLs before shortening
public class UrlValidator {

    private static final List<String> ALLOWED_PROTOCOLS = List.of("http", "https");
    private static final int MAX_URL_LENGTH = 2048;

    // Check that the URL is well-formed and uses http or https
    public static boolean isValid(String urlString) {
        if (urlString == null || urlString.isBlank() || urlString.length() > MAX_URL_LENGTH) {
            return false;
        }
        try {
            URL url = new URL(urlString);
            // Use Locale.ROOT for consistent protocol comparison regardless of system locale
            return ALLOWED_PROTOCOLS.contains(url.getProtocol().toLowerCase(Locale.ROOT));
        } catch (MalformedURLException e) {
            return false;
        }
    }

    // Block URLs pointing to localhost or private IP ranges
    public static boolean isSafe(String urlString) {
        try {
            // Use Locale.ROOT for consistent host comparison regardless of system locale
            String host = new URL(urlString).getHost().toLowerCase(Locale.ROOT);

            // Block localhost
            if (host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1")) {
                return false;
            }

            // Block common private IP ranges
            if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                return false;
            }

            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
