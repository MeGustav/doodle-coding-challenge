CREATE TABLE users
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    timezone   VARCHAR(64)  NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX ux_users_email_lower ON users (UPPER(email));