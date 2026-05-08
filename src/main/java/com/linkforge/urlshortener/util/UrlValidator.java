package com.linkforge.urlshortener.util;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
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
            return ALLOWED_PROTOCOLS.contains(url.getProtocol().toLowerCase(Locale.ROOT));
        } catch (MalformedURLException e) {
            return false;
        }
    }

    // Blocks URLs pointing to localhost, private, or reserved IP ranges (SSRF protection).
    // Resolves the hostname via InetAddress so encoded forms (hex, octal, IPv6) are normalised first.
    public static boolean isSafe(String urlString) {
        try {
            String host = new URL(urlString).getHost().toLowerCase(Locale.ROOT);

            // Strip IPv6 brackets so InetAddress can parse the address
            if (host.startsWith("[") && host.endsWith("]")) {
                host = host.substring(1, host.length() - 1);
            }

            // Resolve the host to a normalised InetAddress
            InetAddress address = InetAddress.getByName(host);

            // Block loopback (127.x.x.x, ::1)
            if (address.isLoopbackAddress()) {
                return false;
            }

            // Block site-local / private ranges (10.x, 172.16-31.x, 192.168.x)
            if (address.isSiteLocalAddress()) {
                return false;
            }

            // Block link-local (169.254.x.x, fe80::/10)
            if (address.isLinkLocalAddress()) {
                return false;
            }

            // Block any-local / unspecified (0.0.0.0, ::)
            if (address.isAnyLocalAddress()) {
                return false;
            }

            // Block multicast
            if (address.isMulticastAddress()) {
                return false;
            }

            return true;

        } catch (MalformedURLException | UnknownHostException e) {
            // If the host cannot be parsed or resolved, treat it as unsafe
            return false;
        }
    }
}
