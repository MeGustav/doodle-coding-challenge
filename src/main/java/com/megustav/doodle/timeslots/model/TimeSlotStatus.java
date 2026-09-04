package com.megustav.doodle.timeslots.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "State of a slot inside its owner's calendar")
public enum TimeSlotStatus {

    /** Published availability - the only state a meeting can be booked on. */
    AVAILABLE,

    /** Taken by a meeting. Released again if that meeting is cancelled. */
    BOOKED,

    /** Blocked by the owner. */
    BLOCKED;

    /**
     * BOOKED is owned by the meeting sitting on the slot, so it never arrives from a client.
     */
    public boolean isSettableByHand() {
        return this != BOOKED;
    }
}
