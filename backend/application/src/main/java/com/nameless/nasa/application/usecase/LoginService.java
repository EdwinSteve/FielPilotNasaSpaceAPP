package com.nameless.nasa.application.usecase;

import com.nameless.nasa.application.annotations.UseCase;
import com.nameless.nasa.domain.command.LoginCommand;
import com.nameless.nasa.domain.exception.InvalidCredentialsException;
import com.nameless.nasa.domain.model.AuthToken;
import com.nameless.nasa.domain.model.User;
import com.nameless.nasa.domain.ports.input.LoginUseCase;
import com.nameless.nasa.domain.ports.output.PasswordHasher;
import com.nameless.nasa.domain.ports.output.TokenIssuer;
import com.nameless.nasa.domain.ports.output.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final String dummyHash = passwordHasher.hash(UUID.randomUUID().toString()); ;

    @Override
    public AuthToken login(LoginCommand command) {
        User user = userRepository.findByPhone(PhoneNormalizer.normalize(command.phone())).orElse(null);
        String hashToCheck = user != null ? user.passwordHash() : dummyHash;
        boolean passwordMatches = passwordHasher.matches(command.rawPassword(), hashToCheck);

        if (user == null || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        return tokenIssuer.issue(user);
    }
}
