package com.megustav.doodle.timeslots.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "A slice of a user's calendar")
public record TimeSlotDto(

        @Schema(description = "ID", example = "0199b3f1-2c8e-7a11-9c0b-2f4e6a8c1d33")
        UUID id,

        @Schema(description = "Owner of the calendar", example = "7d66d21a-9ade-4cf4-8d0a-0d3e73677546")
        UUID userId,

        @Schema(description = "Start of the slot, inclusive", example = "2026-09-10T09:00:00Z")
        Instant startsAt,

        @Schema(description = "End of the slot, exclusive", example = "2026-09-10T09:30:00Z")
        Instant endsAt,

        @Schema(description = "Current state of the slot", example = "AVAILABLE")
        TimeSlotStatus status,

        @Schema(description = "When the slot was created", example = "2026-09-03T15:06:30Z")
        Instant createdAt,

        @Schema(description = "When the slot was last modified", example = "2026-09-03T15:06:30Z")
        Instant updatedAt
) {

    public static TimeSlotDto fromEntity(TimeSlotEntity entity) {
        return new TimeSlotDto(
                entity.getId(),
                entity.getUserId(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
