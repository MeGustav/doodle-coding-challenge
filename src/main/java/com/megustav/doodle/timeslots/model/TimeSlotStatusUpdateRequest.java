package com.megustav.doodle.timeslots.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Marks a slot busy or free")
public record TimeSlotStatusUpdateRequest(

        @Schema(
                description = "Target state. BOOKED is not accepted - it belongs to the meeting "
                        + "holding the slot, and nothing would be responsible for releasing it.",
                example = "BLOCKED"
        )
        @NotNull
        TimeSlotStatus status
) {

    @Schema(hidden = true)
    @AssertTrue(message = "status must be AVAILABLE or BLOCKED")
    public boolean isStatusSettable() {
        return status == null || status.isSettableByHand();
    }
}
