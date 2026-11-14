package com.nameless.nasa.domain.ports.input;

import com.nameless.nasa.domain.command.LoginCommand;
import com.nameless.nasa.domain.model.AuthToken;

public interface LoginUseCase {
    AuthToken login(LoginCommand command);
}
