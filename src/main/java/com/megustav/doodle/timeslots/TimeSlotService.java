package com.megustav.doodle.timeslots;

import com.megustav.doodle.timeslots.model.AvailabilityWindowDto;
import com.megustav.doodle.timeslots.model.TimeSlotCreationRequest;
import com.megustav.doodle.timeslots.model.TimeSlotDto;
import com.megustav.doodle.timeslots.model.TimeSlotRescheduleRequest;
import com.megustav.doodle.timeslots.model.TimeSlotSeriesRequest;
import com.megustav.doodle.timeslots.model.TimeSlotStatusUpdateRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeSlotService {

    TimeSlotDto createSlot(UUID userId, TimeSlotCreationRequest request);

    List<TimeSlotDto> createSeries(UUID userId, TimeSlotSeriesRequest request);

    List<TimeSlotDto> findSlots(UUID userId, Instant from, Instant to);

    List<AvailabilityWindowDto> findAvailability(UUID userId, Instant from, Instant to, int durationMinutes);

    Optional<TimeSlotDto> getSlot(UUID slotId);

    TimeSlotDto reschedule(UUID slotId, TimeSlotRescheduleRequest request);

    TimeSlotDto changeStatus(UUID slotId, TimeSlotStatusUpdateRequest request);

    void deleteSlot(UUID slotId);
}
