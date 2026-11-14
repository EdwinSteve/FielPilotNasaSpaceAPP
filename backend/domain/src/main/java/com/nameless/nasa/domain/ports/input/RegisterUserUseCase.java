package com.nameless.nasa.domain.ports.input;

import com.nameless.nasa.domain.command.RegisterUserCommand;
import com.nameless.nasa.domain.model.User;

public interface RegisterUserUseCase {
    User register(RegisterUserCommand command);
}
