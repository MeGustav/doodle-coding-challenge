package com.megustav.doodle.timeslots.exceptions;

import com.megustav.doodle.common.exceptions.DomainException;
import com.megustav.doodle.timeslots.model.TimeSlotStatus;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class TimeSlotNotEditableException extends DomainException {

    public TimeSlotNotEditableException(UUID slotId, TimeSlotStatus status) {
        super(HttpStatus.CONFLICT, "Time slot '%s' is %s and can only be changed through the meeting holding it".formatted(slotId, status));
    }
}
