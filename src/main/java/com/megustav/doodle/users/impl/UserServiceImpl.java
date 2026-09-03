package com.megustav.doodle.users.impl;

import com.megustav.doodle.users.UserRepository;
import com.megustav.doodle.users.UserService;
import com.megustav.doodle.users.exceptions.EmailAlreadyInUseException;
import com.megustav.doodle.users.model.UserCreationRequest;
import com.megustav.doodle.users.model.UserDto;
import com.megustav.doodle.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserDto createUser(UserCreationRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new EmailAlreadyInUseException(request.email());
        }
        try {
            var saved = userRepository.saveAndFlush(new UserEntity(request.name(), request.email(), ZoneId.of(request.timezone())));
            return new UserDto(
                    saved.getId(),
                    saved.getName(),
                    saved.getEmail(),
                    saved.getTimezone().getId(),
                    saved.getCreatedAt(),
                    saved.getUpdatedAt()
            );
        } catch (DataIntegrityViolationException e) {
            // Lost the race between the check above and this insert.
            throw new EmailAlreadyInUseException(request.email(), e);
        }
    }

    @Override
    public Optional<UserDto> getUser(UUID id) {
        return userRepository.findById(id).map(UserServiceImpl::fromEntity);
    }

    @Override
    public Optional<UserDto> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email).map(UserServiceImpl::fromEntity);
    }

    private static UserDto fromEntity(UserEntity entity) {
        return new UserDto(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getTimezone().getId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

