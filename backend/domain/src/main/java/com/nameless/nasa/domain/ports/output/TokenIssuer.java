package com.nameless.nasa.domain.ports.output;

import com.nameless.nasa.domain.model.AuthToken;
import com.nameless.nasa.domain.model.User;

public interface TokenIssuer {
    AuthToken issue(User user);
}
