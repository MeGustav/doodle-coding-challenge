package com.megustav.doodle.timeslots.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "New boundaries for an existing slot")
public record TimeSlotRescheduleRequest(

        @Schema(description = "Start of the slot, inclusive", example = "2026-09-10T10:00:00Z")
        @NotNull
        Instant startsAt,

        @Schema(description = "End of the slot, exclusive", example = "2026-09-10T10:30:00Z")
        @NotNull
        Instant endsAt
) {

    @Schema(hidden = true)
    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isIntervalOrdered() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }
}
