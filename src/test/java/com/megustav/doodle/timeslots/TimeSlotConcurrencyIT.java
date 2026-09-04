package com.megustav.doodle.timeslots;

import com.megustav.doodle.Main;
import com.megustav.doodle.timeslots.exceptions.OverlappingTimeSlotException;
import com.megustav.doodle.timeslots.model.TimeSlotCreationRequest;
import com.megustav.doodle.timeslots.model.TimeSlotSeriesRequest;
import com.megustav.doodle.timeslots.model.TimeSlotStatus;
import com.megustav.doodle.users.UserService;
import com.megustav.doodle.users.model.UserCreationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The main slots constraint is that it should not allow for double bookings.
 * To be totally sure - creating a proper Testcontainers test to test solely this mechanism
 */
@SpringBootTest(classes = Main.class)
@Testcontainers
@EnabledIf("dockerAvailable")
class TimeSlotConcurrencyIT {

    private static final int CONTENDERS = 8;
    private static final Instant NINE = Instant.parse("2026-09-10T09:00:00Z");
    private static final Instant TEN = Instant.parse("2026-09-10T10:00:00Z");
    private static final Instant ELEVEN = Instant.parse("2026-09-10T11:00:00Z");
    private static final String CONTENDER_PREFIX = "contender";
    private static final String OTHER_PREFIX = "other";
    private static final String TIMEZONE = "Europe/Berlin";

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("doodle")
            .withUsername("doodle")
            .withPassword("doodle");

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private UserService userService;

    private UUID userId;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void freshUser() {
        userId = userService.createUser(new UserCreationRequest(
                CONTENDER_PREFIX, "%s-%s@example.com".formatted(CONTENDER_PREFIX, UUID.randomUUID()), TIMEZONE
        )).id();
    }

    @Nested
    class CreateSlot {

        @Test
        void letsExactlyOneCallerWin_whenEveryoneAsksForTheSameWindow() throws Exception {
            var outcomes = race(() -> timeSlotService.createSlot(userId, new TimeSlotCreationRequest(NINE, TEN, null)));

            assertThat(outcomes.successes()).isEqualTo(1);
            assertThat(outcomes.conflicts()).isEqualTo(CONTENDERS - 1);
            assertThat(outcomes.unexpected()).isEmpty();
            assertThat(timeSlotRepository.findWithin(userId, NINE, TEN)).hasSize(1);
        }

        /**
         * Different but overlapping slots, database index does not help here
         */
        @Test
        void letsExactlyOneCallerWin_whenTheRequestedWindowsMerelyOverlap() throws Exception {
            var offsets = new AtomicInteger();
            var outcomes = race(() -> {
                var startsAt = NINE.plus(Duration.ofMinutes(5L * offsets.getAndIncrement()));
                return timeSlotService.createSlot(
                        userId, new TimeSlotCreationRequest(startsAt, startsAt.plusSeconds(3600), null));
            });

            assertThat(outcomes.successes()).isEqualTo(1);
            assertThat(outcomes.conflicts()).isEqualTo(CONTENDERS - 1);
            assertThat(outcomes.unexpected()).isEmpty();
            assertThat(timeSlotRepository.findWithin(userId, NINE, ELEVEN)).hasSize(1);
        }

        @Test
        void allowsBackToBackSlots_becauseIntervalsAreHalfOpen() {
            timeSlotService.createSlot(userId, new TimeSlotCreationRequest(NINE, TEN, null));
            timeSlotService.createSlot(userId, new TimeSlotCreationRequest(TEN, ELEVEN, null));

            var slots = timeSlotRepository.findWithin(userId, NINE, ELEVEN);

            assertThat(slots).hasSize(2);
            assertThat(slots).allSatisfy(slot -> assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.AVAILABLE));
        }

        @Test
        void keepsCalendarsIndependent_whenDifferentUsersWantTheSameHour() {
            var otherUserId = userService.createUser(new UserCreationRequest(
                    OTHER_PREFIX, "%s-%s@example.com".formatted(OTHER_PREFIX, UUID.randomUUID()), TIMEZONE
            )).id();

            timeSlotService.createSlot(userId, new TimeSlotCreationRequest(NINE, TEN, null));
            timeSlotService.createSlot(otherUserId, new TimeSlotCreationRequest(NINE, TEN, null));

            assertThat(timeSlotRepository.findWithin(userId, NINE, TEN)).hasSize(1);
            assertThat(timeSlotRepository.findWithin(otherUserId, NINE, TEN)).hasSize(1);
        }
    }

    @Test
    void createSeries_letsExactlyOneCallerWin_whenEveryoneGeneratesTheSameDay() throws Exception {
        var outcomes = race(() -> timeSlotService.createSeries(
                userId, new TimeSlotSeriesRequest(NINE, ELEVEN, 30)
        ));

        assertThat(outcomes.successes()).isEqualTo(1);
        assertThat(outcomes.conflicts()).isEqualTo(CONTENDERS - 1);
        assertThat(outcomes.unexpected()).isEmpty();
        // Four slots from the single winner, nothing half-applied from the losers.
        assertThat(timeSlotRepository.findWithin(userId, NINE, ELEVEN)).hasSize(4);
    }

    /**
     * Runs all attempts to acquire the slot at the same exact time and collects the outcomes.
     */
    private Outcomes race(Callable<?> attempt) throws Exception {
        var barrier = new CyclicBarrier(CONTENDERS);
        var successes = new AtomicInteger();
        var conflicts = new AtomicInteger();
        var unexpected = new ArrayList<Throwable>();

        try (var executor = Executors.newFixedThreadPool(CONTENDERS)) {
            var futures = IntStream.range(0, CONTENDERS)
                    .mapToObj(_ -> executor.submit(() -> {
                        barrier.await(10, TimeUnit.SECONDS);
                        return attempt.call();
                    })).toList();

            for (var future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                    successes.incrementAndGet();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof OverlappingTimeSlotException) {
                        conflicts.incrementAndGet();
                    } else {
                        unexpected.add(e.getCause());
                    }
                }
            }
        }
        return new Outcomes(successes.get(), conflicts.get(), List.copyOf(unexpected));
    }

    private record Outcomes(int successes, int conflicts, List<Throwable> unexpected) {
    }

    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException e) {
            return false;
        }
    }
}
