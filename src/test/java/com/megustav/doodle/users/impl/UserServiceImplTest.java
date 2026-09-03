package com.megustav.doodle.users.impl;

import com.megustav.doodle.users.UserRepository;
import com.megustav.doodle.users.exceptions.EmailAlreadyInUseException;
import com.megustav.doodle.users.model.UserCreationRequest;
import com.megustav.doodle.users.model.UserDto;
import com.megustav.doodle.users.model.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String NAME = "Test";
    private static final String EMAIL = "test@example.com";
    private static final String TIMEZONE = "Europe/Berlin";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createUser_persistsAndReturnsDto_whenEmailIsFree() {
        var request = new UserCreationRequest(NAME, EMAIL, TIMEZONE);
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);

        UserEntity persisted = mockPersistedEntity(NAME, EMAIL, TIMEZONE);
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenReturn(persisted);

        UserDto result = userService.createUser(request);

        assertThat(result.id()).isEqualTo(persisted.getId());
        assertThat(result.name()).isEqualTo(NAME);
        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.timezone()).isEqualTo(TIMEZONE);
        assertThat(result.createdAt()).isEqualTo(persisted.getCreatedAt());
        assertThat(result.updatedAt()).isEqualTo(persisted.getUpdatedAt());
    }

    @Test
    void createUser_throwsEmailAlreadyInUse_whenEmailAlreadyExists() {
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(
                new UserCreationRequest(NAME, EMAIL, TIMEZONE)
        )).isInstanceOf(EmailAlreadyInUseException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void createUser_throwsEmailAlreadyInUse_whenInsertLosesUniqueConstraintRace() {
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint violation"));

        assertThatThrownBy(() -> userService.createUser(
                new UserCreationRequest(NAME, EMAIL, TIMEZONE)))
                .isInstanceOf(EmailAlreadyInUseException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void getUser_returnsDto_whenUserExists() {
        UserEntity entity = mockPersistedEntity(NAME, EMAIL, TIMEZONE);
        when(userRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        Optional<UserDto> result = userService.getUser(entity.getId());

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(entity.getId());
        assertThat(result.get().email()).isEqualTo(EMAIL);
    }

    @Test
    void getUser_returnsEmpty_whenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        Optional<UserDto> result = userService.getUser(id);

        assertThat(result).isEmpty();
    }

    @Test
    void findByEmail_returnsDto_whenUserExists() {
        UserEntity entity = mockPersistedEntity(NAME, EMAIL, TIMEZONE);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(entity));

        Optional<UserDto> result = userService.findByEmail(EMAIL);

        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo(EMAIL);
    }

    @Test
    void findByEmail_returnsEmpty_whenNoUserMatches() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        Optional<UserDto> result = userService.findByEmail("nobody@example.com");

        assertThat(result).isEmpty();
    }

    private static UserEntity mockPersistedEntity(String name, String email, String timezone) {
        UserEntity entity = mock(UserEntity.class);
        when(entity.getId()).thenReturn(UUID.randomUUID());
        when(entity.getName()).thenReturn(name);
        when(entity.getEmail()).thenReturn(email);
        when(entity.getTimezone()).thenReturn(ZoneId.of(timezone));
        Instant timestamp = Instant.now();
        when(entity.getCreatedAt()).thenReturn(timestamp);
        when(entity.getUpdatedAt()).thenReturn(timestamp);
        return entity;
    }
}