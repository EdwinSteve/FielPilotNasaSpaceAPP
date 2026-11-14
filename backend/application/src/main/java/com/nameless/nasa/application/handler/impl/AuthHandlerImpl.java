package com.nameless.nasa.application.handler.impl;

import com.nameless.nasa.application.annotations.Handler;
import com.nameless.nasa.application.handler.AuthHandler;
import com.nameless.nasa.domain.command.LoginCommand;
import com.nameless.nasa.domain.command.RegisterUserCommand;
import com.nameless.nasa.domain.model.AuthToken;
import com.nameless.nasa.domain.model.User;
import com.nameless.nasa.domain.ports.input.LoginUseCase;
import com.nameless.nasa.domain.ports.input.RegisterUserUseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Handler
@RequiredArgsConstructor
public class AuthHandlerImpl implements AuthHandler {
    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;

    @Override
    @Transactional
    public User register(RegisterUserCommand command) {
        return registerUserUseCase.register(command);
    }

    @Override
    public AuthToken login(LoginCommand command) {
        return loginUseCase.login(command);
    }
}
