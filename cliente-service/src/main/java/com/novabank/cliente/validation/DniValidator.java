package com.novabank.cliente.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DniValidator implements ConstraintValidator<ValidDni, String> {

    private static final String DNI_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";

    /**
     * Deja que @NotBlank gestione la obligatoriedad y limita esta validacion
     * a comprobar formato y letra de control.
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String normalized = value.trim().toUpperCase();

        if (!normalized.matches("^\\d{8}[A-Z]$")) {
            return false;
        }

        int number = Integer.parseInt(normalized.substring(0, 8));
        char expectedLetter = DNI_LETTERS.charAt(number % 23);
        char actualLetter = normalized.charAt(8);

        return expectedLetter == actualLetter;
    }
}
