package com.megustav.doodle.users.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "A user")
public record UserDto(

        @Schema(description = "Server-generated identifier", example = "7d66d21a-9ade-4cf4-8d0a-0d3e73677546")
        UUID id,

        @Schema(description = "Display name of the user", example = "John Doe")
        String name,

        @Schema(description = "Email address", example = "john@example.com")
        String email,

        @Schema(description = "IANA time zone id", example = "Europe/Berlin")
        String timezone,

        @Schema(description = "When the user was created", example = "2026-09-03T15:06:30Z")
        Instant createdAt,

        @Schema(description = "When the user was last modified", example = "2026-09-03T15:06:30Z")
        Instant updatedAt
) {}
