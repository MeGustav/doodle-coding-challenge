package com.megustav.doodle.users;

import com.megustav.doodle.users.model.UserCreationRequest;
import com.megustav.doodle.users.model.UserDto;

import java.util.Optional;
import java.util.UUID;

public interface UserService {

    UserDto createUser(UserCreationRequest request);

    Optional<UserDto> getUser(UUID id);

    Optional<UserDto> findByEmail(String email);

}
