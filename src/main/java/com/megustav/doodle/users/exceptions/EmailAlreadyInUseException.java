package com.megustav.doodle.users.exceptions;

import com.megustav.doodle.common.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyInUseException extends DomainException {

    public EmailAlreadyInUseException(String email) {
        super(HttpStatus.CONFLICT, message(email));
    }

    public EmailAlreadyInUseException(String email, Throwable cause) {
        super(HttpStatus.CONFLICT, message(email), cause);
    }

    private static String message(String email) {
        return "Email '%s' is already used".formatted(email);
    }
}
