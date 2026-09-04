package com.megustav.doodle.timeslots;

import com.megustav.doodle.timeslots.model.AvailabilityWindowDto;
import com.megustav.doodle.timeslots.model.TimeSlotCreationRequest;
import com.megustav.doodle.timeslots.model.TimeSlotDto;
import com.megustav.doodle.timeslots.model.TimeSlotRescheduleRequest;
import com.megustav.doodle.timeslots.model.TimeSlotSeriesRequest;
import com.megustav.doodle.timeslots.model.TimeSlotStatusUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Time slots", description = "Managing the raw availability a calendar is made of")
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    @PostMapping("/users/{userId}/slots")
    @Operation(
            summary = "Create a slot",
            description = "Adds a single slot to a calendar, AVAILABLE unless asked for BLOCKED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Slot created", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = TimeSlotDto.class)
            )),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Overlaps an existing slot", content = @Content)
    })
    public ResponseEntity<TimeSlotDto> createSlot(
            @Parameter(description = "Owner of the calendar", required = true)
            @PathVariable UUID userId,
            @Valid @RequestBody TimeSlotCreationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timeSlotService.createSlot(userId, request));
    }

    @PostMapping("/users/{userId}/slots/series")
    @Operation(
            summary = "Create a series of slots",
            description = "Chops a window into back-to-back slots of the given duration (15 minutes to 1 day)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Slots created", content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = TimeSlotDto.class))
            )),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Some slot overlaps an existing one", content = @Content)
    })
    public ResponseEntity<List<TimeSlotDto>> createSeries(
            @Parameter(description = "Owner of the calendar", required = true)
            @PathVariable UUID userId,
            @Valid @RequestBody TimeSlotSeriesRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timeSlotService.createSeries(userId, request));
    }

    @GetMapping("/users/{userId}/slots")
    @Operation(summary = "List slots", description = "Returns every slot intersecting the requested window.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Slots returned", content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = TimeSlotDto.class))
            )),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public List<TimeSlotDto> findSlots(
            @Parameter(description = "Owner of the calendar", required = true)
            @PathVariable UUID userId,
            @Parameter(description = "Window start, inclusive", required = true, example = "2026-09-10T00:00:00Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Window end, exclusive", required = true, example = "2026-09-11T00:00:00Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return timeSlotService.findSlots(userId, from, to);
    }

    @GetMapping("/users/{userId}/slots/availability")
    @Operation(
            summary = "Find availability",
            description = "Returns the free windows in the requested range that are at least as long as the "
                    + "requested meeting duration. Touching AVAILABLE slots are merged into one window first, "
                    + "so a 90 minute request can be satisfied by two back-to-back 45 minute slots or 3 30 minutes ones."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Windows returned", content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AvailabilityWindowDto.class))
            )),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public List<AvailabilityWindowDto> findAvailability(
            @Parameter(description = "Owner of the calendar", required = true)
            @PathVariable UUID userId,
            @Parameter(description = "Window start, inclusive", required = true, example = "2026-09-10T00:00:00Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Window end, exclusive", required = true, example = "2026-09-11T00:00:00Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @Parameter(description = "Length of the meeting requested", required = true, example = "45")
            @RequestParam @Min(1) @Max(24 * 60) int durationMinutes
    ) {
        return timeSlotService.findAvailability(userId, from, to, durationMinutes);
    }

    @GetMapping("/slots/{slotId}")
    @Operation(summary = "Get a slot by ID", description = "Returns a single slot by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Slot returned", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = TimeSlotDto.class)
            )),
            @ApiResponse(responseCode = "404", description = "Slot not found", content = @Content)
    })
    public ResponseEntity<TimeSlotDto> getSlot(
            @Parameter(description = "Slot ID", required = true)
            @PathVariable UUID slotId
    ) {
        return timeSlotService.getSlot(slotId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/slots/{slotId}")
    @Operation(summary = "Move a slot", description = "Changes the boundaries of a slot that is not booked.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Slot moved", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = TimeSlotDto.class)
            )),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Slot not found", content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "Slot is booked, or the new interval overlaps another slot", content = @Content)
    })
    public TimeSlotDto reschedule(
            @Parameter(description = "Slot ID", required = true)
            @PathVariable UUID slotId,
            @Valid @RequestBody TimeSlotRescheduleRequest request
    ) {
        return timeSlotService.reschedule(slotId, request);
    }

    @PatchMapping("/slots/{slotId}/status")
    @Operation(
            summary = "Mark a slot free or busy",
            description = "Moves a slot between AVAILABLE and BLOCKED. BOOKED is rejected - it belongs to the meeting holding the slot."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = TimeSlotDto.class)
            )),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Slot not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Slot is booked", content = @Content)
    })
    public TimeSlotDto changeStatus(
            @Parameter(description = "Slot ID", required = true)
            @PathVariable UUID slotId,
            @Valid @RequestBody TimeSlotStatusUpdateRequest request
    ) {
        return timeSlotService.changeStatus(slotId, request);
    }

    @DeleteMapping("/slots/{slotId}")
    @Operation(summary = "Delete a slot", description = "Removes a slot that is not booked.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Slot deleted", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such slot", content = @Content),
            @ApiResponse(responseCode = "409", description = "Slot is booked", content = @Content)
    })
    public ResponseEntity<Void> deleteSlot(
            @Parameter(description = "Slot ID", required = true)
            @PathVariable UUID slotId
    ) {
        timeSlotService.deleteSlot(slotId);
        return ResponseEntity.noContent().build();
    }
}
