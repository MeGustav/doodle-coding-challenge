package com.megustav.doodle.timeslots.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "Payload for creating a single slot in a calendar")
public record TimeSlotCreationRequest(

        @Schema(description = "Start of the slot, inclusive", example = "2026-09-10T09:00:00Z")
        @NotNull
        Instant startsAt,

        @Schema(description = "End of the slot, exclusive", example = "2026-09-10T09:30:00Z")
        @NotNull
        Instant endsAt,

        @Schema(
                description = "AVAILABLE to offer the time, BLOCKED to block it. "
                        + "Defaults to AVAILABLE. BOOKED is not accepted - only a meeting can book a slot.",
                example = "AVAILABLE", defaultValue = "AVAILABLE"
        )
        TimeSlotStatus status
) {

    @Schema(hidden = true)
    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isIntervalOrdered() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }

    @Schema(hidden = true)
    @AssertTrue(message = "status must be AVAILABLE or BLOCKED")
    public boolean isStatusSettable() {
        return status == null || status.isSettableByHand();
    }

    public TimeSlotStatus statusOrDefault() {
        return status == null ? TimeSlotStatus.AVAILABLE : status;
    }
}
