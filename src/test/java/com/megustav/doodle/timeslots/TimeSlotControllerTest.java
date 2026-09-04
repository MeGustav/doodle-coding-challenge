package com.megustav.doodle.timeslots;

import com.megustav.doodle.common.web.DomainExceptionHandler;
import com.megustav.doodle.common.web.RequestValidationExceptionHandler;
import com.megustav.doodle.timeslots.exceptions.OverlappingTimeSlotException;
import com.megustav.doodle.timeslots.exceptions.TimeSlotNotEditableException;
import com.megustav.doodle.timeslots.exceptions.TimeSlotNotFoundException;
import com.megustav.doodle.timeslots.model.AvailabilityWindowDto;
import com.megustav.doodle.timeslots.model.TimeSlotCreationRequest;
import com.megustav.doodle.timeslots.model.TimeSlotDto;
import com.megustav.doodle.timeslots.model.TimeSlotRescheduleRequest;
import com.megustav.doodle.timeslots.model.TimeSlotSeriesRequest;
import com.megustav.doodle.timeslots.model.TimeSlotStatus;
import com.megustav.doodle.timeslots.model.TimeSlotStatusUpdateRequest;
import com.megustav.doodle.support.JsonFixtures;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = TimeSlotControllerTest.WebSliceConfig.class)
class TimeSlotControllerTest {

    private static final UUID USER_ID = UUID.fromString("7d66d21a-9ade-4cf4-8d0a-0d3e73677546");
    private static final UUID SLOT_ID = UUID.fromString("0199b3f1-2c8e-7a11-9c0b-2f4e6a8c1d33");
    private static final String NINE = "2026-09-10T09:00:00Z";
    private static final String TEN = "2026-09-10T10:00:00Z";

    private static final String EMPTY_BODY = JsonFixtures.load("timeslots/empty-body.json");
    private static final String RESCHEDULE_SLOT = JsonFixtures.load("timeslots/reschedule-slot.json");
    private static final String CREATE_SLOT_BOOKED = JsonFixtures.load("timeslots/create-slot-booked.json");
    private static final String CREATE_SLOT_BLOCKED = JsonFixtures.load("timeslots/create-slot-blocked.json");
    private static final String CREATE_SERIES = JsonFixtures.load("timeslots/create-series.json");
    private static final String CREATE_SERIES_DURATION_TOO_SHORT =
            JsonFixtures.load("timeslots/create-series-duration-too-short.json");
    private static final String CREATE_SERIES_DURATION_TOO_LONG =
            JsonFixtures.load("timeslots/create-series-duration-too-long.json");
    private static final String STATUS_BLOCKED = JsonFixtures.load("timeslots/status-blocked.json");
    private static final String STATUS_AVAILABLE = JsonFixtures.load("timeslots/status-available.json");
    private static final String STATUS_BOOKED = JsonFixtures.load("timeslots/status-booked.json");
    private static final String STATUS_INVALID = JsonFixtures.load("timeslots/status-invalid.json");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimeSlotService timeSlotService;

    @Nested
    class CreateSlot {

        @Test
        void returns201WithBody_onValidRequest() throws Exception {
            when(timeSlotService.createSlot(eq(USER_ID), any(TimeSlotCreationRequest.class))).thenReturn(slotDto());

            mockMvc.perform(post("/api/users/{userId}/slots", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(NINE, TEN)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(SLOT_ID.toString()))
                    .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                    .andExpect(jsonPath("$.startsAt").value(NINE))
                    .andExpect(jsonPath("$.endsAt").value(TEN))
                    .andExpect(jsonPath("$.status").value("AVAILABLE"));
        }

        @Test
        void returns400_whenTheIntervalIsInverted() throws Exception {
            mockMvc.perform(post("/api/users/{userId}/slots", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(TEN, NINE)))
                    .andExpect(status().isBadRequest());

            verify(timeSlotService, never()).createSlot(any(), any());
        }

        @Test
        void returns400_whenTheIntervalIsEmpty() throws Exception {
            mockMvc.perform(post("/api/users/{userId}/slots", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(NINE, NINE)))
                    .andExpect(status().isBadRequest());

            verify(timeSlotService, never()).createSlot(any(), any());
        }

        @Test
        void returns400_whenBoundariesAreMissing() throws Exception {
            mockMvc.perform(post("/api/users/{userId}/slots", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(EMPTY_BODY))
                    .andExpect(status().isBadRequest());

            verify(timeSlotService, never()).createSlot(any(), any());
        }

        @Test
        void returns409_whenTheWindowIsAlreadyTaken() throws Exception {
            when(timeSlotService.createSlot(eq(USER_ID), any(TimeSlotCreationRequest.class)))
                    .thenThrow(new OverlappingTimeSlotException(USER_ID, Instant.parse(NINE), Instant.parse(TEN)));

            mockMvc.perform(post("/api/users/{userId}/slots", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(NINE, TEN)))
                    .andExpect(status().isConflict())
                    // The point of the advice: the client is told which window clashed, not just "409".
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(containsString(USER_ID.toString())));
        }

        @Test
        void returns400_whenAskedToCreateAnAlreadyBookedSlot() throws Exception {
            mockMvc.perform(post("/api/users/{userId}/slots", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_SLOT_BOOKED.formatted(NINE, TEN)))
                    .andExpect(status().isBadRequest());

            verify(timeSlotService, never()).createSlot(any(), any());
        }

        @Test
        void createsABlockedSlot_whenAskedFor() throws Exception {
            when(timeSlotService.createSlot(eq(USER_ID), any(TimeSlotCreationRequest.class)))
                    .thenReturn(slotDto(TimeSlotStatus.BLOCKED));

            mockMvc.perform(post("/api/users/{userId}/slots", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_SLOT_BLOCKED.formatted(NINE, TEN)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("BLOCKED"));
        }
    }

    @Nested
    class CreateSeries {

        @Test
        void returns201WithEveryGeneratedSlot() throws Exception {
            when(timeSlotService.createSeries(eq(USER_ID), any(TimeSlotSeriesRequest.class)))
                    .thenReturn(List.of(slotDto(), slotDto()));

            mockMvc.perform(post("/api/users/{userId}/slots/series", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_SERIES.formatted(NINE, TEN)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        void returns400_whenTheDurationIsBelowTheFifteenMinuteMinimum() throws Exception {
            mockMvc.perform(post("/api/users/{userId}/slots/series", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_SERIES_DURATION_TOO_SHORT.formatted(NINE, TEN)))
                    .andExpect(status().isBadRequest());

            verify(timeSlotService, never()).createSeries(any(), any());
        }

        @Test
        void returns400_whenTheDurationIsAboveTheOneDayMaximum() throws Exception {
            mockMvc.perform(post("/api/users/{userId}/slots/series", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_SERIES_DURATION_TOO_LONG.formatted(NINE, TEN)))
                    .andExpect(status().isBadRequest());

            verify(timeSlotService, never()).createSeries(any(), any());
        }
    }

    @Nested
    class FindSlots {

        @Test
        void returns200WithTheWindowContents() throws Exception {
            when(timeSlotService.findSlots(USER_ID, Instant.parse(NINE), Instant.parse(TEN)))
                    .thenReturn(List.of(slotDto()));

            mockMvc.perform(get("/api/users/{userId}/slots", USER_ID)
                            .queryParam("from", NINE)
                            .queryParam("to", TEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(SLOT_ID.toString()));
        }

        @Test
        void returns400_whenTheWindowIsMissing() throws Exception {
            mockMvc.perform(get("/api/users/{userId}/slots", USER_ID))
                    .andExpect(status().isBadRequest());

            verify(timeSlotService, never()).findSlots(any(), any(), any());
        }
    }

    @Nested
    class FindAvailability {

        @Test
        void returns200WithTheFreeWindows() throws Exception {
            var window = new AvailabilityWindowDto(Instant.parse(NINE), Instant.parse(TEN));
            when(timeSlotService.findAvailability(USER_ID, Instant.parse(NINE), Instant.parse(TEN), 45))
                    .thenReturn(List.of(window));

            mockMvc.perform(get("/api/users/{userId}/slots/availability", USER_ID)
                            .queryParam("from", NINE)
                            .queryParam("to", TEN)
                            .queryParam("durationMinutes", "45"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].startsAt").value(NINE))
                    .andExpect(jsonPath("$[0].endsAt").value(TEN));
        }

        @Test
        void returns400_whenDurationIsNotPositive() throws Exception {
            mockMvc.perform(get("/api/users/{userId}/slots/availability", USER_ID)
                            .queryParam("from", NINE)
                            .queryParam("to", TEN)
                            .queryParam("durationMinutes", "0"))
                    .andExpect(status().isBadRequest());

            verify(timeSlotService, never()).findAvailability(any(), any(), any(), anyInt());
        }

        @Test
        void returns400_whenDurationIsMissing() throws Exception {
            mockMvc.perform(get("/api/users/{userId}/slots/availability", USER_ID)
                            .queryParam("from", NINE)
                            .queryParam("to", TEN))
                    .andExpect(status().isBadRequest());

            verify(timeSlotService, never()).findAvailability(any(), any(), any(), anyInt());
        }
    }

    @Nested
    class GetSlot {

        @Test
        void returns200WithBody_whenFound() throws Exception {
            when(timeSlotService.getSlot(SLOT_ID)).thenReturn(Optional.of(slotDto()));

            mockMvc.perform(get("/api/slots/{slotId}", SLOT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SLOT_ID.toString()));
        }

        @Test
        void returns404_whenNotFound() throws Exception {
            when(timeSlotService.getSlot(SLOT_ID)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/slots/{slotId}", SLOT_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns400_whenIdIsNotAValidUuid() throws Exception {
            mockMvc.perform(get("/api/slots/{slotId}", "not-a-uuid"))
                    .andExpect(status().isBadRequest());

            verify(timeSlotService, never()).getSlot(any());
        }
    }

    @Nested
    class ChangeStatus {

        @Test
        void returns200WithTheNewStatus() throws Exception {
            when(timeSlotService.changeStatus(eq(SLOT_ID), any(TimeSlotStatusUpdateRequest.class)))
                    .thenReturn(slotDto(TimeSlotStatus.BLOCKED));

            mockMvc.perform(patch("/api/slots/{slotId}/status", SLOT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(STATUS_BLOCKED))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("BLOCKED"));
        }

        @Test
        void returns400_onAnUnknownStatus() throws Exception {
            mockMvc.perform(patch("/api/slots/{slotId}/status", SLOT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(STATUS_INVALID))
                    .andExpect(status().isBadRequest());

            verify(timeSlotService, never()).changeStatus(any(), any());
        }

        @Test
        void returns409_whenTheSlotIsBooked() throws Exception {
            when(timeSlotService.changeStatus(eq(SLOT_ID), any(TimeSlotStatusUpdateRequest.class)))
                    .thenThrow(new TimeSlotNotEditableException(SLOT_ID, TimeSlotStatus.BOOKED));

            mockMvc.perform(patch("/api/slots/{slotId}/status", SLOT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(STATUS_AVAILABLE))
                    .andExpect(status().isConflict());
        }

        @Test
        void returns400_whenAskedToBookThroughAStatusChange() throws Exception {
            mockMvc.perform(patch("/api/slots/{slotId}/status", SLOT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(STATUS_BOOKED))
                    .andExpect(status().isBadRequest());

            verify(timeSlotService, never()).changeStatus(any(), any());
        }
    }

    @Nested
    class DeleteSlot {

        @Test
        void returns404_whenThereIsNoSuchSlot() throws Exception {
            doThrow(new TimeSlotNotFoundException(SLOT_ID)).when(timeSlotService).deleteSlot(SLOT_ID);

            mockMvc.perform(delete("/api/slots/{slotId}", SLOT_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns204_whenTheSlotIsGone() throws Exception {
            mockMvc.perform(delete("/api/slots/{slotId}", SLOT_ID))
                    .andExpect(status().isNoContent());

            verify(timeSlotService).deleteSlot(SLOT_ID);
        }
    }

    @Test
    void reschedule_returns200WithTheMovedSlot() throws Exception {
        when(timeSlotService.reschedule(eq(SLOT_ID), any(TimeSlotRescheduleRequest.class))).thenReturn(slotDto());

        mockMvc.perform(patch("/api/slots/{slotId}", SLOT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(NINE, TEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SLOT_ID.toString()));
    }

    private static String body(String startsAt, String endsAt) {
        return RESCHEDULE_SLOT.formatted(startsAt, endsAt);
    }

    private static TimeSlotDto slotDto() {
        return slotDto(TimeSlotStatus.AVAILABLE);
    }

    private static TimeSlotDto slotDto(TimeSlotStatus status) {
        var timestamp = Instant.parse("2026-09-03T15:06:30Z");
        return new TimeSlotDto(
                SLOT_ID, USER_ID, Instant.parse(NINE), Instant.parse(TEN), status, timestamp, timestamp);
    }

    /**
     * Same trick as in UserControllerTest: pointing the slice at its own configuration keeps
     * the JPA machinery on Main out of a test that only cares about HTTP.
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class WebSliceConfig {

        @Bean
        DomainExceptionHandler domainExceptionHandler() {
            return new DomainExceptionHandler();
        }

        @Bean
        RequestValidationExceptionHandler requestValidationExceptionHandler() {
            return new RequestValidationExceptionHandler();
        }

        @Bean
        TimeSlotController timeSlotController(TimeSlotService timeSlotService) {
            return new TimeSlotController(timeSlotService);
        }
    }
}
