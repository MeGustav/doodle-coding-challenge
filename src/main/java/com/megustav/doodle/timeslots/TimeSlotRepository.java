package com.megustav.doodle.timeslots;

import com.megustav.doodle.timeslots.model.TimeSlotEntity;
import com.megustav.doodle.timeslots.model.TimeSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlotEntity, UUID> {

    /**
     * Half-open intervals: a slot ending at 10:00 and one starting at 10:00 do not overlap.
     */
    @Query("""
            select s from TimeSlotEntity s
            where s.userId = :userId and s.startsAt < :to and s.endsAt > :from
            order by s.startsAt
            """
    )
    List<TimeSlotEntity> findWithin(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    /**
     * Same window, ignoring one slot.
     * Used to exclude the slot that is being rescheduled
     */
    @Query("""
            select s from TimeSlotEntity s
            where s.userId = :userId and s.startsAt < :to and s.endsAt > :from and s.id <> :excludedId
            order by s.startsAt
            """
    )
    List<TimeSlotEntity> findWithinExcluding(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("excludedId") UUID excludedId
    );

    /**
     * What the availability query merges into free windows.
     * Ordered, so touching slots sit next to each other and the merge is a single pass.
     */
    @Query("""
            select s from TimeSlotEntity s
            where s.userId = :userId and s.status = :status and s.startsAt < :to and s.endsAt > :from
            order by s.startsAt
            """
    )
    List<TimeSlotEntity> findByStatusWithin(
            @Param("userId") UUID userId,
            @Param("status") TimeSlotStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
