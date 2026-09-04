package com.megustav.doodle.timeslots.exceptions;

import com.megustav.doodle.common.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class TooManySlotsInSeriesException extends DomainException {

    public TooManySlotsInSeriesException(int limit) {
        super(HttpStatus.BAD_REQUEST, "A single series is capped at %d slots".formatted(limit));
    }
}
