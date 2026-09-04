package com.megustav.doodle.users.exceptions;

import com.megustav.doodle.common.exceptions.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class UserNotFoundException extends DomainException {

    public UserNotFoundException(UUID userId) {
        super(HttpStatus.NOT_FOUND, "User '%s' does not exist".formatted(userId));
    }
}
