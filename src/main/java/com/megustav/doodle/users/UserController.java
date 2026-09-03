package com.megustav.doodle.users;

import com.megustav.doodle.users.model.UserCreationRequest;
import com.megustav.doodle.users.model.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Managing users")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a user", description = "Creates a new user and returns the created resource.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = UserDto.class)
            )),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    public ResponseEntity<UserDto> createUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User data", required = true, content = @Content(
                    schema = @Schema(implementation = UserCreationRequest.class)
            ))
            @Valid @RequestBody UserCreationRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.createUser(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by ID", description = "Returns a single user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User retrieved successfully", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = UserDto.class)
            )),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<UserDto> getUser(
            @Parameter(description = "User ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id
    ) {
        return userService.getUser(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Find user by email", description = "Returns a user matching the specified email address.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User retrieved successfully", content = @Content(
                    mediaType = "application/json", schema = @Schema(implementation = UserDto.class)
            )),
            @ApiResponse(responseCode = "400", description = "Invalid email address", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<UserDto> findUserByEmail(
            @Parameter(description = "Email address of the user to find", required = true, example = "john@example.com")
            @RequestParam @NotBlank @Email String email
    ) {
        return userService.findByEmail(email).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

}
