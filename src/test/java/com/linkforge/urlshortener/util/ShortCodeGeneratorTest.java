package com.linkforge.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Unit tests for ShortCodeGenerator
class ShortCodeGeneratorTest {

    @Test
    void generate_defaultLength_returns6CharCode() {
        String code = ShortCodeGenerator.generate();
        assertThat(code).isNotNull();
        assertThat(code).hasSize(6);
    }

    @Test
    void generate_customLength_returnsCorrectSize() {
        assertThat(ShortCodeGenerator.generate(4)).hasSize(4);
        assertThat(ShortCodeGenerator.generate(8)).hasSize(8);
        assertThat(ShortCodeGenerator.generate(10)).hasSize(10);
    }

    @Test
    void generate_containsOnlyAllowedCharacters() {
        String code = ShortCodeGenerator.generate();
        assertThat(code).matches("^[a-zA-Z0-9]+$");
    }

    @Test
    void generate_producesUniqueValues() {
        // Generate 100 codes and verify no duplicates
        long distinctCount = java.util.stream.IntStream.range(0, 100)
                .mapToObj(i -> ShortCodeGenerator.generate())
                .distinct()
                .count();
        assertThat(distinctCount).isEqualTo(100);
    }

    @Test
    void generate_withLengthBelowMinimum_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> ShortCodeGenerator.generate(3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generate_withLengthAboveMaximum_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> ShortCodeGenerator.generate(11))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isValid_withValidCode_returnsTrue() {
        assertThat(ShortCodeGenerator.isValid("abc123")).isTrue();
        assertThat(ShortCodeGenerator.isValid("ABCDEF")).isTrue();
        assertThat(ShortCodeGenerator.isValid("aB3xYz")).isTrue();
    }

    @Test
    void isValid_withNullCode_returnsFalse() {
        assertThat(ShortCodeGenerator.isValid(null)).isFalse();
    }

    @Test
    void isValid_withTooShortCode_returnsFalse() {
        assertThat(ShortCodeGenerator.isValid("abc")).isFalse();
    }

    @Test
    void isValid_withTooLongCode_returnsFalse() {
        assertThat(ShortCodeGenerator.isValid("abcdefghijk")).isFalse();
    }

    @Test
    void isValid_withInvalidCharacters_returnsFalse() {
        assertThat(ShortCodeGenerator.isValid("abc-12")).isFalse();
        assertThat(ShortCodeGenerator.isValid("abc@12")).isFalse();
        assertThat(ShortCodeGenerator.isValid("abc 12")).isFalse();
    }
}
