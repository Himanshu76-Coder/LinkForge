package com.linkforge.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Unit tests for UrlValidator
class UrlValidatorTest {

    // ==========================================
    // isValid() tests
    // ==========================================

    @Test
    void isValid_withHttpsUrl_returnsTrue() {
        assertThat(UrlValidator.isValid("https://www.example.com")).isTrue();
    }

    @Test
    void isValid_withHttpUrl_returnsTrue() {
        assertThat(UrlValidator.isValid("http://example.com")).isTrue();
    }

    @Test
    void isValid_withQueryParams_returnsTrue() {
        assertThat(UrlValidator.isValid("https://example.com/path?query=value&other=123")).isTrue();
    }

    @Test
    void isValid_withNullUrl_returnsFalse() {
        assertThat(UrlValidator.isValid(null)).isFalse();
    }

    @Test
    void isValid_withBlankUrl_returnsFalse() {
        assertThat(UrlValidator.isValid("")).isFalse();
        assertThat(UrlValidator.isValid("   ")).isFalse();
    }

    @Test
    void isValid_withFtpUrl_returnsFalse() {
        assertThat(UrlValidator.isValid("ftp://example.com")).isFalse();
    }

    @Test
    void isValid_withJavascriptUrl_returnsFalse() {
        assertThat(UrlValidator.isValid("javascript:alert('xss')")).isFalse();
    }

    @Test
    void isValid_withBareString_returnsFalse() {
        assertThat(UrlValidator.isValid("not-a-url")).isFalse();
    }

    @Test
    void isValid_withUrlExceedingMaxLength_returnsFalse() {
        // Build a URL longer than 2048 characters
        String longUrl = "https://example.com/" + "a".repeat(2050);
        assertThat(UrlValidator.isValid(longUrl)).isFalse();
    }

    // ==========================================
    // isSafe() tests
    // ==========================================

    @Test
    void isSafe_withPublicUrl_returnsTrue() {
        assertThat(UrlValidator.isSafe("https://www.example.com")).isTrue();
        assertThat(UrlValidator.isSafe("https://google.com")).isTrue();
    }

    @Test
    void isSafe_withLocalhost_returnsFalse() {
        assertThat(UrlValidator.isSafe("http://localhost")).isFalse();
        assertThat(UrlValidator.isSafe("http://localhost:8080")).isFalse();
    }

    @Test
    void isSafe_withLoopbackIp_returnsFalse() {
        assertThat(UrlValidator.isSafe("http://127.0.0.1")).isFalse();
        assertThat(UrlValidator.isSafe("http://127.0.0.1:8080/path")).isFalse();
    }

    @Test
    void isSafe_withPrivateIpRange192_returnsFalse() {
        assertThat(UrlValidator.isSafe("http://192.168.1.1")).isFalse();
        assertThat(UrlValidator.isSafe("http://192.168.0.100/admin")).isFalse();
    }

    @Test
    void isSafe_withPrivateIpRange10_returnsFalse() {
        assertThat(UrlValidator.isSafe("http://10.0.0.1")).isFalse();
    }

    @Test
    void isSafe_withPrivateIpRange172_returnsFalse() {
        assertThat(UrlValidator.isSafe("http://172.16.0.1")).isFalse();
    }
}
