package com.megustav.doodle.timeslots.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "Payload for chopping a window into back-to-back slots of a fixed duration")
public record TimeSlotSeriesRequest(

        @Schema(description = "Start of the window, inclusive", example = "2026-09-10T09:00:00Z")
        @NotNull
        Instant from,

        @Schema(description = "End of the window, exclusive", example = "2026-09-10T17:00:00Z")
        @NotNull
        Instant to,

        @Schema(description = "Length of every generated slot, in minutes. 15 minutes to 1 day.", example = "30")
        @NotNull
        @Min(15)
        @Max(24 * 60)
        Integer slotDurationMinutes
) {

    @Schema(hidden = true)
    @AssertTrue(message = "to must be after from")
    public boolean isWindowOrdered() {
        return from == null || to == null || to.isAfter(from);
    }
}
