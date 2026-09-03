package com.megustav.doodle.users.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException(String email) {
        super("Email '%s' is already used".formatted(email));
    }

    public EmailAlreadyInUseException(String email, Throwable cause) {
        super("Email '%s' is already used".formatted(email), cause);
    }
}
