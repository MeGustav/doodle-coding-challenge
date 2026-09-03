package com.megustav.doodle.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.DateTimeException;
import java.time.ZoneId;

public class ZoneIdValidator implements ConstraintValidator<ValidZoneId, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            // absence is @NotNull's business
            return true;
        }
        try {
            var _ = ZoneId.of(value);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }
}