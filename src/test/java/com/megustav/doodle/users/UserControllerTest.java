package com.megustav.doodle.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.megustav.doodle.users.model.UserCreationRequest;
import com.megustav.doodle.users.model.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@ContextConfiguration(classes = UserControllerTest.WebSliceConfig.class)
class UserControllerTest {

    private static final String NAME = "Test";
    private static final String EMAIL = "test@example.com";
    private static final String TIMEZONE = "Europe/Berlin";
    private static final String UNKNOWN_EMAIL = "nobody@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    void createUser_returns201WithBody_onValidRequest() throws Exception {
        var request = new UserCreationRequest(NAME, EMAIL, TIMEZONE);
        var response = new UserDto(UUID.randomUUID(), NAME, EMAIL, TIMEZONE, Instant.now(), Instant.now());
        when(userService.createUser(request)).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.name").value(NAME))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.timezone").value(TIMEZONE));
    }

    @Test
    void createUser_returns400_onInvalidRequestBody() throws Exception {
        String invalidBody = """
                {"name": "", "email": "not-an-email", "timezone": "%s"}
                """.formatted(TIMEZONE);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any());
    }

    @Test
    void getUser_returns200WithBody_whenFound() throws Exception {
        UUID id = UUID.randomUUID();
        var response = new UserDto(id, NAME, EMAIL, TIMEZONE, Instant.now(), Instant.now());
        when(userService.getUser(id)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void getUser_returns404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getUser(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUser_returns400_whenIdIsNotAValidUuid() throws Exception {
        mockMvc.perform(get("/api/users/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getUser(any());
    }

    @Test
    void findUserByEmail_returns200WithBody_whenFound() throws Exception {
        var response = new UserDto(UUID.randomUUID(), NAME, EMAIL, TIMEZONE, Instant.now(), Instant.now());
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/users").queryParam("email", EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void findUserByEmail_returns404_whenNoUserMatches() throws Exception {
        when(userService.findByEmail(UNKNOWN_EMAIL)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users").queryParam("email", UNKNOWN_EMAIL))
                .andExpect(status().isNotFound());
    }

    /**
     * Jpa stuff keep popping up (although excluded), trying this inclusive approach instead
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = UserController.class)
    static class WebSliceConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }

    }
}