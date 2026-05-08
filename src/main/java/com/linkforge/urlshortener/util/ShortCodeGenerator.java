package com.linkforge.urlshortener.util;

import java.security.SecureRandom;

// Utility for generating random short codes used in shortened URLs
public class ShortCodeGenerator {

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int DEFAULT_LENGTH = 6;
    private static final SecureRandom random = new SecureRandom();

    // Generate a random short code with the default length of 6
    public static String generate() {
        return generate(DEFAULT_LENGTH);
    }

    // Generate a random short code with a custom length (4-10 characters)
    public static String generate(int length) {
        if (length < 4 || length > 10) {
            throw new IllegalArgumentException("Short code length must be between 4 and 10");
        }

        StringBuilder shortCode = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            shortCode.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return shortCode.toString();
    }
}
