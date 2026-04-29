package com.novabank.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DniValidatorTest {

    private final DniValidator validator = new DniValidator();

    @Test
    void dniValidoDevuelveTrue() {
        assertThat(validator.isValid("12345678Z", null)).isTrue();
    }

    @Test
    void dniConLetraIncorrectaDevuelveFalse() {
        assertThat(validator.isValid("12345678A", null)).isFalse();
    }

    @Test
    void dniConFormatoIncorrectoDevuelveFalse() {
        assertThat(validator.isValid("1234A", null)).isFalse();
        assertThat(validator.isValid("123456789A", null)).isFalse();
        assertThat(validator.isValid("12345678-", null)).isFalse();
    }

    @Test
    void dniConMinusculaValidaDevuelveTrue() {
        assertThat(validator.isValid("12345678z", null)).isTrue();
    }

    @Test
    void dniNullDevuelveTrueParaDelegarEnNotBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void dniBlankDevuelveTrueParaDelegarEnNotBlank() {
        assertThat(validator.isValid("   ", null)).isTrue();
    }
}
