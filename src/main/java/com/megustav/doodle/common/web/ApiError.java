package com.megustav.doodle.common.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An error response")
public record ApiError(

        @Schema(description = "What went wrong", example = "User 'a1b2c3d4-...' does not exist")
        String message
) {
}
