package com.irontrack.api.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final int MIN_LENGTH = 8;
    private static final Pattern HAS_LETTER_AND_DIGIT = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Nulidade é responsabilidade de @NotBlank, aplicado separadamente em cada DTO.
        if (value == null) {
            return true;
        }
        return value.length() >= MIN_LENGTH && HAS_LETTER_AND_DIGIT.matcher(value).matches();
    }
}
