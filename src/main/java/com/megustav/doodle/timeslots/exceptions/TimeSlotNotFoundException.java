package com.megustav.doodle.timeslots.exceptions;

import com.megustav.doodle.common.exceptions.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class TimeSlotNotFoundException extends DomainException {

    public TimeSlotNotFoundException(UUID slotId) {
        super(HttpStatus.NOT_FOUND, "Time slot '%s' does not exist".formatted(slotId));
    }
}
