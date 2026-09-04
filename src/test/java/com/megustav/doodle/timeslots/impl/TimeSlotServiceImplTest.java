package com.megustav.doodle.timeslots.impl;

import com.megustav.doodle.timeslots.CalendarLockRepository;
import com.megustav.doodle.timeslots.TimeSlotRepository;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final Instant EIGHT = Instant.parse("2026-09-10T08:00:00Z");
    private static final Instant NINE = Instant.parse("2026-09-10T09:00:00Z");
    private static final Instant TEN = Instant.parse("2026-09-10T10:00:00Z");
    private static final Instant ELEVEN = Instant.parse("2026-09-10T11:00:00Z");
    private static final Instant TWELVE = Instant.parse("2026-09-10T12:00:00Z");

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private CalendarLockRepository calendarLockRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TimeSlotServiceImpl timeSlotService;

    @Nested
    class CreateSlot {

        @Test
        void persistsAndReturnsDto_whenWindowIsFree() {
            when(userService.exists(USER_ID)).thenReturn(true);
            when(timeSlotRepository.findWithin(USER_ID, NINE, TEN)).thenReturn(List.of());
            var persisted = persistedSlot(NINE, TEN, TimeSlotStatus.AVAILABLE);
            when(timeSlotRepository.saveAndFlush(any(TimeSlotEntity.class))).thenReturn(persisted);

            var result = timeSlotService.createSlot(USER_ID, new TimeSlotCreationRequest(NINE, TEN, null));

            assertThat(result.id()).isEqualTo(persisted.getId());
            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.startsAt()).isEqualTo(NINE);
            assertThat(result.endsAt()).isEqualTo(TEN);
            assertThat(result.status()).isEqualTo(TimeSlotStatus.AVAILABLE);
        }

        @Test
        void throwsUserNotFound_whenUserDoesNotExist() {
            when(userService.exists(USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> timeSlotService.createSlot(USER_ID, new TimeSlotCreationRequest(NINE, TEN, null)))
                    .isInstanceOf(UserNotFoundException.class);

            verify(timeSlotRepository, never()).saveAndFlush(any());
        }

        @Test
        void throwsOverlapping_whenSomethingAlreadySitsInTheWindow() {
            when(userService.exists(USER_ID)).thenReturn(true);
            when(timeSlotRepository.findWithin(USER_ID, NINE, TEN)).thenReturn(List.of(slot(NINE, TEN)));

            assertThatThrownBy(() -> timeSlotService.createSlot(USER_ID, new TimeSlotCreationRequest(NINE, TEN, null)))
                    .isInstanceOf(OverlappingTimeSlotException.class);

            verify(timeSlotRepository, never()).saveAndFlush(any());
        }

        @Test
        void locksTheCalendarBeforeLookingAtAvailability() {
            when(userService.exists(USER_ID)).thenReturn(true);
            when(timeSlotRepository.findWithin(USER_ID, NINE, TEN)).thenReturn(List.of());
            var persisted = persistedSlot(NINE, TEN, TimeSlotStatus.AVAILABLE);
            when(timeSlotRepository.saveAndFlush(any(TimeSlotEntity.class))).thenReturn(persisted);

            timeSlotService.createSlot(USER_ID, new TimeSlotCreationRequest(NINE, TEN, null));

            // The whole no-overlap guarantee rests on this ordering.
            var order = inOrder(calendarLockRepository, timeSlotRepository);
            order.verify(calendarLockRepository).lock(USER_ID);
            order.verify(timeSlotRepository).findWithin(USER_ID, NINE, TEN);
            order.verify(timeSlotRepository).saveAndFlush(any(TimeSlotEntity.class));
        }

        @Test
        void createsABlockedSlot_whenAskedFor() {
            when(userService.exists(USER_ID)).thenReturn(true);
            when(timeSlotRepository.findWithin(USER_ID, NINE, TEN)).thenReturn(List.of());
            var persisted = persistedSlot(NINE, TEN, TimeSlotStatus.BLOCKED);
            when(timeSlotRepository.saveAndFlush(any(TimeSlotEntity.class))).thenReturn(persisted);

            var result = timeSlotService.createSlot(
                    USER_ID, new TimeSlotCreationRequest(NINE, TEN, TimeSlotStatus.BLOCKED));

            assertThat(result.status()).isEqualTo(TimeSlotStatus.BLOCKED);
        }
    }

    @Nested
    class CreateSeries {

        @Test
        void chopsTheWindowIntoSlotsOfTheRequestedDuration() {
            when(userService.exists(USER_ID)).thenReturn(true);
            when(timeSlotRepository.findWithin(USER_ID, NINE, ELEVEN)).thenReturn(List.of());

            var result = timeSlotService.createSeries(USER_ID, new TimeSlotSeriesRequest(NINE, ELEVEN, 30));

            assertThat(result).hasSize(4);
            assertThat(result).allSatisfy(slot -> {
                assertThat(Duration.between(slot.startsAt(), slot.endsAt())).isEqualTo(Duration.ofMinutes(30));
                assertThat(slot.status()).isEqualTo(TimeSlotStatus.AVAILABLE);
            });
            assertThat(result.getFirst().startsAt()).isEqualTo(NINE);
            assertThat(result.getLast().endsAt()).isEqualTo(ELEVEN);
        }

        @Test
        void dropsATrailingRemainderShorterThanTheRequestedDuration() {
            when(userService.exists(USER_ID)).thenReturn(true);
            when(timeSlotRepository.findWithin(USER_ID, NINE, ELEVEN)).thenReturn(List.of());

            var result = timeSlotService.createSeries(USER_ID, new TimeSlotSeriesRequest(NINE, ELEVEN, 45));

            assertThat(result).hasSize(2);
            assertThat(result.getLast().endsAt()).isEqualTo(Instant.parse("2026-09-10T10:30:00Z"));
        }

        @Test
        void throwsTooManySlots_whenTheSeriesBlowsPastTheCap() {
            var to = NINE.plus(Duration.ofDays(30));
            when(userService.exists(USER_ID)).thenReturn(true);
            when(timeSlotRepository.findWithin(USER_ID, NINE, to)).thenReturn(List.of());

            assertThatThrownBy(() -> timeSlotService.createSeries(USER_ID, new TimeSlotSeriesRequest(NINE, to, 15)))
                    .isInstanceOf(TooManySlotsInSeriesException.class);

            verify(timeSlotRepository, never()).saveAll(any());
        }

        @Test
        void throwsOverlapping_whenAnythingSitsInTheWindow() {
            when(userService.exists(USER_ID)).thenReturn(true);
            when(timeSlotRepository.findWithin(USER_ID, NINE, ELEVEN)).thenReturn(List.of(slot(TEN, ELEVEN)));

            assertThatThrownBy(() -> timeSlotService.createSeries(USER_ID, new TimeSlotSeriesRequest(NINE, ELEVEN, 30)))
                    .isInstanceOf(OverlappingTimeSlotException.class);

            verify(timeSlotRepository, never()).saveAll(any());
        }
    }

    @Nested
    class Reschedule {

        @Test
        void movesTheSlot_whenTheNewWindowIsFree() {
            var existing = slot(NINE, TEN);
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(existing));
            when(timeSlotRepository.findWithinExcluding(USER_ID, TEN, ELEVEN, existing.getId())).thenReturn(List.of());
            when(timeSlotRepository.saveAndFlush(existing)).thenReturn(existing);

            var result = timeSlotService.reschedule(SLOT_ID, new TimeSlotRescheduleRequest(TEN, ELEVEN));

            assertThat(result.startsAt()).isEqualTo(TEN);
            assertThat(result.endsAt()).isEqualTo(ELEVEN);
            verify(calendarLockRepository).lock(USER_ID);
        }

        @Test
        void throwsOverlapping_whenTheNewWindowClashesWithAnotherSlot() {
            var existing = slot(NINE, TEN);
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(existing));
            when(timeSlotRepository.findWithinExcluding(USER_ID, TEN, ELEVEN, existing.getId()))
                    .thenReturn(List.of(slot(TEN, ELEVEN)));

            assertThatThrownBy(() -> timeSlotService.reschedule(SLOT_ID, new TimeSlotRescheduleRequest(TEN, ELEVEN)))
                    .isInstanceOf(OverlappingTimeSlotException.class);

            verify(timeSlotRepository, never()).saveAndFlush(any());
        }

        @Test
        void throwsNotEditable_whenTheSlotIsBooked() {
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(bookedSlot()));

            assertThatThrownBy(() -> timeSlotService.reschedule(SLOT_ID, new TimeSlotRescheduleRequest(TEN, ELEVEN)))
                    .isInstanceOf(TimeSlotNotEditableException.class);
        }

        @Test
        void throwsNotFound_whenTheSlotDoesNotExist() {
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> timeSlotService.reschedule(SLOT_ID, new TimeSlotRescheduleRequest(TEN, ELEVEN)))
                    .isInstanceOf(TimeSlotNotFoundException.class);
        }
    }

    @Nested
    class ChangeStatus {

        @Test
        void blocksAnAvailableSlot() {
            var existing = slot(NINE, TEN);
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(existing));
            when(timeSlotRepository.saveAndFlush(existing)).thenReturn(existing);

            var result = timeSlotService.changeStatus(SLOT_ID, new TimeSlotStatusUpdateRequest(TimeSlotStatus.BLOCKED));

            assertThat(result.status()).isEqualTo(TimeSlotStatus.BLOCKED);
        }

        @Test
        void throwsNotEditable_whenTheSlotIsBooked() {
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(bookedSlot()));

            assertThatThrownBy(() ->
                    timeSlotService.changeStatus(SLOT_ID, new TimeSlotStatusUpdateRequest(TimeSlotStatus.AVAILABLE)))
                    .isInstanceOf(TimeSlotNotEditableException.class);
        }
    }

    @Nested
    class DeleteSlot {

        @Test
        void removesAnAvailableSlot() {
            var existing = slot(NINE, TEN);
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(existing));

            timeSlotService.deleteSlot(SLOT_ID);

            verify(timeSlotRepository).delete(existing);
        }

        @Test
        void throwsNotEditable_whenTheSlotIsBooked() {
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(bookedSlot()));

            assertThatThrownBy(() -> timeSlotService.deleteSlot(SLOT_ID))
                    .isInstanceOf(TimeSlotNotEditableException.class);

            verify(timeSlotRepository, never()).delete(any());
        }

        @Test
        void throwsNotFound_whenThereIsNoSuchSlot() {
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> timeSlotService.deleteSlot(SLOT_ID))
                    .isInstanceOf(TimeSlotNotFoundException.class);

            verify(timeSlotRepository, never()).delete(any());
        }
    }

    @Nested
    class FindAvailability {

        @Test
        void returnsTheWindow_whenItIsLongEnough() {
            when(userService.exists(USER_ID)).thenReturn(true);
            when(timeSlotRepository.findByStatusWithin(USER_ID, TimeSlotStatus.AVAILABLE, NINE, TWELVE))
                    .thenReturn(List.of(slot(NINE, TWELVE)));

            var result = timeSlotService.findAvailability(USER_ID, NINE, TWELVE, 90);

            assertThat(result).containsExactly(new AvailabilityWindowDto(NINE, TWELVE));
        }

        @Test
        void dropsTheWindow_whenItIsTooShort() {
            when(userService.exists(USER_ID)).thenReturn(true);
            when(timeSlotRepository.findByStatusWithin(USER_ID, TimeSlotStatus.AVAILABLE, NINE, TEN))
                    .thenReturn(List.of(slot(NINE, TEN)));

            var result = timeSlotService.findAvailability(USER_ID, NINE, TEN, 90);

            assertThat(result).isEmpty();
        }

        @Test
        void mergesTouchingSlots_soTheirCombinedLengthCounts() {
            when(userService.exists(USER_ID)).thenReturn(true);
            // Two 60 minute AVAILABLE slots back to back satisfy a 90 minute request together,
            // even though neither one does alone
            when(timeSlotRepository.findByStatusWithin(USER_ID, TimeSlotStatus.AVAILABLE, NINE, ELEVEN))
                    .thenReturn(List.of(slot(NINE, TEN), slot(TEN, ELEVEN)));

            var result = timeSlotService.findAvailability(USER_ID, NINE, ELEVEN, 90);

            assertThat(result).containsExactly(new AvailabilityWindowDto(NINE, ELEVEN));
        }

        @Test
        void keepsSlotsSeparate_whenAGapSitsBetweenThem() {
            when(userService.exists(USER_ID)).thenReturn(true);
            // A booked slot from 10 to 11 splits what would otherwise be one long window - the
            // repository only ever returns AVAILABLE slots, so the gap shows up on its own here
            when(timeSlotRepository.findByStatusWithin(USER_ID, TimeSlotStatus.AVAILABLE, NINE, TWELVE))
                    .thenReturn(List.of(slot(NINE, TEN), slot(ELEVEN, TWELVE)));

            var result = timeSlotService.findAvailability(USER_ID, NINE, TWELVE, 30);

            assertThat(result).containsExactly(
                    new AvailabilityWindowDto(NINE, TEN),
                    new AvailabilityWindowDto(ELEVEN, TWELVE));
        }

        @Test
        void clipsAWindowPokingOutOfTheRequestedRange() {
            when(userService.exists(USER_ID)).thenReturn(true);
            // The slot itself runs 08:00-12:00, but only 09:00-12:00 was asked about
            when(timeSlotRepository.findByStatusWithin(USER_ID, TimeSlotStatus.AVAILABLE, NINE, TWELVE))
                    .thenReturn(List.of(slot(EIGHT, TWELVE)));

            var result = timeSlotService.findAvailability(USER_ID, NINE, TWELVE, 60);

            assertThat(result).containsExactly(new AvailabilityWindowDto(NINE, TWELVE));
        }

        @Test
        void throwsUserNotFound_whenUserDoesNotExist() {
            when(userService.exists(USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> timeSlotService.findAvailability(USER_ID, NINE, TWELVE, 30))
                    .isInstanceOf(UserNotFoundException.class);

            verify(timeSlotRepository, never()).findByStatusWithin(any(), any(), any(), any());
        }
    }

    @Test
    void getSlot_returnsEmpty_whenTheSlotDoesNotExist() {
        when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.empty());

        assertThat(timeSlotService.getSlot(SLOT_ID)).isEmpty();
    }

    @Test
    void findSlots_returnsEverythingIntersectingTheWindow() {
        when(userService.exists(USER_ID)).thenReturn(true);
        when(timeSlotRepository.findWithin(USER_ID, NINE, ELEVEN))
                .thenReturn(List.of(slot(NINE, TEN), bookedSlot()));

        var result = timeSlotService.findSlots(USER_ID, NINE, ELEVEN);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().status()).isEqualTo(TimeSlotStatus.AVAILABLE);
        assertThat(result.getLast().status()).isEqualTo(TimeSlotStatus.BOOKED);
    }

    private static TimeSlotEntity slot(Instant startsAt, Instant endsAt) {
        return new TimeSlotEntity(USER_ID, startsAt, endsAt, TimeSlotStatus.AVAILABLE);
    }

    private static TimeSlotEntity bookedSlot() {
        return new TimeSlotEntity(USER_ID, TEN, ELEVEN, TimeSlotStatus.BOOKED);
    }

    private static TimeSlotEntity persistedSlot(Instant startsAt, Instant endsAt, TimeSlotStatus status) {
        var slot = mock(TimeSlotEntity.class);
        when(slot.getId()).thenReturn(UUID.randomUUID());
        when(slot.getUserId()).thenReturn(USER_ID);
        when(slot.getStartsAt()).thenReturn(startsAt);
        when(slot.getEndsAt()).thenReturn(endsAt);
        when(slot.getStatus()).thenReturn(status);
        var timestamp = Instant.now();
        when(slot.getCreatedAt()).thenReturn(timestamp);
        when(slot.getUpdatedAt()).thenReturn(timestamp);
        return slot;
    }
}
