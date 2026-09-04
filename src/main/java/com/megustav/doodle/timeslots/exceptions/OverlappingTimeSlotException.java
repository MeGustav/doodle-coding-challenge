package com.megustav.doodle.timeslots.exceptions;

import com.megustav.doodle.common.exceptions.DomainException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

public class OverlappingTimeSlotException extends DomainException {

    public OverlappingTimeSlotException(UUID userId, Instant startsAt, Instant endsAt) {
        super(HttpStatus.CONFLICT, message(userId, startsAt, endsAt));
    }

    public OverlappingTimeSlotException(UUID userId, Instant startsAt, Instant endsAt, Throwable cause) {
        super(HttpStatus.CONFLICT, message(userId, startsAt, endsAt), cause);
    }

    private static String message(UUID userId, Instant startsAt, Instant endsAt) {
        return "User '%s' already has a slot overlapping [%s, %s)".formatted(userId, startsAt, endsAt);
    }
}
