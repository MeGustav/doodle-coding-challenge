package com.megustav.doodle.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ZoneIdValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidZoneId {

    String message() default "must be a valid IANA time zone id";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}