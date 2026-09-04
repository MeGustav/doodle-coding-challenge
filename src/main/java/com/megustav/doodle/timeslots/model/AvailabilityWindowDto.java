package com.megustav.doodle.timeslots.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "A stretch of free time")
public record AvailabilityWindowDto(

        @Schema(description = "Start of the free window, inclusive", example = "2026-09-10T09:00:00Z")
        Instant startsAt,

        @Schema(description = "End of the free window, exclusive", example = "2026-09-10T12:00:00Z")
        Instant endsAt
) {
}
