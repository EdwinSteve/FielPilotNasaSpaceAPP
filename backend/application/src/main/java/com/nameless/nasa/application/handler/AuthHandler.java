package com.nameless.nasa.application.handler;

import com.nameless.nasa.domain.command.LoginCommand;
import com.nameless.nasa.domain.command.RegisterUserCommand;
import com.nameless.nasa.domain.model.AuthToken;
import com.nameless.nasa.domain.model.User;

public interface AuthHandler {
    User register(RegisterUserCommand command);
    AuthToken login(LoginCommand command);
}
