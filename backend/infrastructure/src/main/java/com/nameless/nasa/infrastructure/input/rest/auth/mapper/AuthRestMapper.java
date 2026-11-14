package com.nameless.nasa.infrastructure.input.rest.auth.mapper;

import com.nameless.nasa.domain.command.LoginCommand;
import com.nameless.nasa.domain.command.RegisterUserCommand;
import com.nameless.nasa.domain.model.AuthToken;
import com.nameless.nasa.domain.model.User;
import com.nameless.nasa.infrastructure.input.rest.auth.request.LoginRequest;
import com.nameless.nasa.infrastructure.input.rest.auth.request.RegisterUserRequest;
import com.nameless.nasa.infrastructure.input.rest.auth.response.LoginResponse;
import com.nameless.nasa.infrastructure.input.rest.auth.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface AuthRestMapper {
    @Mapping(source = "password", target = "rawPassword")
    RegisterUserCommand toCommand(RegisterUserRequest request);

    @Mapping(source = "password", target = "rawPassword")
    LoginCommand toCommand(LoginRequest request);

    UserResponse toResponse(User user);

    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(source = "expiresInSeconds", target = "expiresIn")
    LoginResponse toResponse(AuthToken token);
}
