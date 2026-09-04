package com.megustav.doodle.timeslots;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * The mutex for a single calendar.
 *
 * A calendar is not a row anywhere, so there is nothing to lock directly - but every calendar
 * belongs to exactly one user, so the user row does the job.
 *
 * Locks are per user, so unrelated calendars never wait on each other.
 * Once we have meetings, this class is to be updated to lock on several users simultaneously
 */
@Repository
@RequiredArgsConstructor
public class CalendarLockRepository {

    private static final String LOCK_OWNER = "SELECT id FROM users WHERE id = :id FOR UPDATE";

    private final EntityManager entityManager;

    public void lock(UUID userId) {
        entityManager.createNativeQuery(LOCK_OWNER)
                .setParameter("id", userId)
                .getResultList();
    }
}
