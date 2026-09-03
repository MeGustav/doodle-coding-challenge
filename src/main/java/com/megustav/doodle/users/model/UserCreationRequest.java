package com.megustav.doodle.users.model;

import com.megustav.doodle.common.validation.ValidZoneId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for registering a new user")
public record UserCreationRequest(

        @Schema(description = "Display name of the user", example = "John Doe")
        @NotBlank
        @Size(max = 150)
        String name,

        @Schema(description = "Email address", example = "john@example.com")
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Schema(description = "IANA time zone id", example = "Europe/Berlin", defaultValue = "UTC")
        @NotBlank
        @ValidZoneId
        @Size(max = 64)
        String timezone
) {
}
