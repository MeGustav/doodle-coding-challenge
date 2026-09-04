package com.megustav.doodle.common.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Anything the API is expected to fail with, as opposed to a bug.
 *
 * Doing this instead of {@code @ResponseStatus} as we need more details in the message
 */
@Getter
public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;

    protected DomainException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    protected DomainException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
