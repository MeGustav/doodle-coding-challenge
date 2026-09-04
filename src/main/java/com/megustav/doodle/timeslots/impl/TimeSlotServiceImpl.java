package com.megustav.doodle.timeslots.impl;

import com.megustav.doodle.timeslots.CalendarLockRepository;
import com.megustav.doodle.timeslots.TimeSlotRepository;
import com.megustav.doodle.timeslots.TimeSlotService;
import com.megustav.doodle.timeslots.exceptions.OverlappingTimeSlotException;
import com.megustav.doodle.timeslots.exceptions.TimeSlotNotEditableException;
import com.megustav.doodle.timeslots.exceptions.TimeSlotNotFoundException;
import com.megustav.doodle.timeslots.exceptions.TooManySlotsInSeriesException;
import com.megustav.doodle.timeslots.model.AvailabilityWindowDto;
import com.megustav.doodle.timeslots.model.TimeSlotCreationRequest;
import com.megustav.doodle.timeslots.model.TimeSlotDto;
import com.megustav.doodle.timeslots.model.TimeSlotEntity;
import com.megustav.doodle.timeslots.model.TimeSlotRescheduleRequest;
import com.megustav.doodle.timeslots.model.TimeSlotSeriesRequest;
import com.megustav.doodle.timeslots.model.TimeSlotStatus;
import com.megustav.doodle.timeslots.model.TimeSlotStatusUpdateRequest;
import com.megustav.doodle.users.UserService;
import com.megustav.doodle.users.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TimeSlotServiceImpl implements TimeSlotService {

    /**
     * Maximum length of a series (limiting it to maximum of 15-minutes slots in a day)
     * (taking 24 hours, people need to work!)
     */
    private static final int MAX_SLOTS_PER_SERIES = 100;

    private final TimeSlotRepository timeSlotRepository;
    private final CalendarLockRepository calendarLockRepository;
    private final UserService userService;

    @Override
    @Transactional
    public TimeSlotDto createSlot(UUID userId, TimeSlotCreationRequest request) {
        requireUserAndLock(userId);
        requireFree(userId, request.startsAt(), request.endsAt());
        var slot = new TimeSlotEntity(userId, request.startsAt(), request.endsAt(), request.statusOrDefault());
        return TimeSlotDto.fromEntity(saveWithinFreeWindow(slot));
    }

    @Override
    @Transactional
    public List<TimeSlotDto> createSeries(UUID userId, TimeSlotSeriesRequest request) {
        requireUserAndLock(userId);
        requireFree(userId, request.from(), request.to());
        var slots = split(userId, request);
        timeSlotRepository.saveAll(slots);
        flushWithinFreeWindow(userId, request.from(), request.to());
        return slots.stream().map(TimeSlotDto::fromEntity).toList();
    }

    @Override
    public List<TimeSlotDto> findSlots(UUID userId, Instant from, Instant to) {
        requireUser(userId);
        return timeSlotRepository.findWithin(userId, from, to).stream().map(TimeSlotDto::fromEntity).toList();
    }

    @Override
    public List<AvailabilityWindowDto> findAvailability(UUID userId, Instant from, Instant to, int durationMinutes) {
        requireUser(userId);
        var minDuration = Duration.ofMinutes(durationMinutes);
        var available = timeSlotRepository.findByStatusWithin(userId, TimeSlotStatus.AVAILABLE, from, to);
        return coalesce(available, from, to).stream()
                .filter(window -> !Duration.between(window.startsAt(), window.endsAt()).minus(minDuration).isNegative())
                .toList();
    }

    @Override
    public Optional<TimeSlotDto> getSlot(UUID slotId) {
        return timeSlotRepository.findById(slotId).map(TimeSlotDto::fromEntity);
    }

    @Override
    @Transactional
    public TimeSlotDto reschedule(UUID slotId, TimeSlotRescheduleRequest request) {
        var slot = requireEditableSlot(slotId);
        calendarLockRepository.lock(slot.getUserId());
        var clashes = timeSlotRepository.findWithinExcluding(
                slot.getUserId(), request.startsAt(), request.endsAt(), slot.getId()
        );
        if (!clashes.isEmpty()) {
            throw new OverlappingTimeSlotException(slot.getUserId(), request.startsAt(), request.endsAt());
        }
        return TimeSlotDto.fromEntity(saveWithinFreeWindow(moveTo(slot, request.startsAt(), request.endsAt())));
    }

    @Override
    @Transactional
    public TimeSlotDto changeStatus(UUID slotId, TimeSlotStatusUpdateRequest request) {
        var slot = requireEditableSlot(slotId);
        slot.setStatus(request.status());
        return TimeSlotDto.fromEntity(timeSlotRepository.saveAndFlush(slot));
    }

    @Override
    @Transactional
    public void deleteSlot(UUID slotId) {
        timeSlotRepository.delete(requireEditableSlot(slotId));
    }

    private static TimeSlotEntity moveTo(TimeSlotEntity slot, Instant startsAt, Instant endsAt) {
        slot.setStartsAt(startsAt);
        slot.setEndsAt(endsAt);
        return slot;
    }

    private TimeSlotEntity requireEditableSlot(UUID slotId) {
        var slot = timeSlotRepository.findById(slotId).orElseThrow(() -> new TimeSlotNotFoundException(slotId));
        if (slot.getStatus() == TimeSlotStatus.BOOKED) {
            throw new TimeSlotNotEditableException(slotId, slot.getStatus());
        }
        return slot;
    }

    private void requireFree(UUID userId, Instant from, Instant to) {
        if (!timeSlotRepository.findWithin(userId, from, to).isEmpty()) {
            throw new OverlappingTimeSlotException(userId, from, to);
        }
    }

    private TimeSlotEntity saveWithinFreeWindow(TimeSlotEntity slot) {
        try {
            return timeSlotRepository.saveAndFlush(slot);
        } catch (DataIntegrityViolationException e) {
            throw new OverlappingTimeSlotException(slot.getUserId(), slot.getStartsAt(), slot.getEndsAt(), e);
        }
    }

    private void flushWithinFreeWindow(UUID userId, Instant from, Instant to) {
        try {
            timeSlotRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new OverlappingTimeSlotException(userId, from, to, e);
        }
    }

    private void requireUser(UUID userId) {
        if (!userService.exists(userId)) {
            throw new UserNotFoundException(userId);
        }
    }

    private void requireUserAndLock(UUID userId) {
        requireUser(userId);
        calendarLockRepository.lock(userId);
    }

    private static List<TimeSlotEntity> split(UUID userId, TimeSlotSeriesRequest request) {
        var duration = Duration.ofMinutes(request.slotDurationMinutes());
        var slots = new ArrayList<TimeSlotEntity>();
        // A trailing remainder shorter than the requested duration is dropped
        for (var start = request.from(); !start.plus(duration).isAfter(request.to()); start = start.plus(duration)) {
            if (slots.size() == MAX_SLOTS_PER_SERIES) {
                throw new TooManySlotsInSeriesException(MAX_SLOTS_PER_SERIES);
            }
            slots.add(new TimeSlotEntity(userId, start, start.plus(duration), TimeSlotStatus.AVAILABLE));
        }
        return slots;
    }

    /**
     * Turns AVAILABLE rows into maximal free windows: slots are clipped to [from, to) first,
     * then consecutive slots that touch end-to-start are folded into one window.
     */
    private static List<AvailabilityWindowDto> coalesce(List<TimeSlotEntity> sortedSlots, Instant from, Instant to) {
        var windows = new ArrayList<AvailabilityWindowDto>();
        for (var slot : sortedSlots) {
            var startsAt = clip(slot.getStartsAt(), from, to);
            var endsAt = clip(slot.getEndsAt(), from, to);
            var last = windows.isEmpty() ? null : windows.getLast();
            if (last != null && last.endsAt().equals(startsAt)) {
                windows.set(windows.size() - 1, new AvailabilityWindowDto(last.startsAt(), endsAt));
            } else {
                windows.add(new AvailabilityWindowDto(startsAt, endsAt));
            }
        }
        return windows;
    }

    private static Instant clip(Instant instant, Instant from, Instant to) {
        if (instant.isBefore(from)) {
            return from;
        }
        return instant.isAfter(to) ? to : instant;
    }
}
