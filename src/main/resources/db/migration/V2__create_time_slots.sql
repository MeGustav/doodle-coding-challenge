CREATE TABLE time_slots
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    starts_at  TIMESTAMPTZ NOT NULL,
    ends_at    TIMESTAMPTZ NOT NULL,
    status     VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_time_slots_positive_duration CHECK (ends_at > starts_at)
);

-- Overlap is prevented in the service, which locks the calendar owner before it
-- writes (see CalendarLockRepository). This index is the cheap last line of defense
-- for the most obvious case: the exact same slot inserted twice.
CREATE UNIQUE INDEX ux_time_slots_user_start ON time_slots (user_id, starts_at);
