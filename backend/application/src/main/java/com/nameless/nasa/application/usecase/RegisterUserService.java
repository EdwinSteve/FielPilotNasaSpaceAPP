package com.nameless.nasa.application.usecase;

import com.nameless.nasa.application.annotations.UseCase;
import com.nameless.nasa.domain.command.RegisterUserCommand;
import com.nameless.nasa.domain.exception.EmailAlreadyRegisteredException;
import com.nameless.nasa.domain.exception.PhoneAlreadyRegisteredException;
import com.nameless.nasa.domain.exception.PolicyNotAcceptedException;
import com.nameless.nasa.domain.model.User;
import com.nameless.nasa.domain.ports.input.RegisterUserUseCase;
import com.nameless.nasa.domain.ports.output.PasswordHasher;
import com.nameless.nasa.domain.ports.output.UserRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class RegisterUserService implements RegisterUserUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    @Override
    public User register(RegisterUserCommand command) {
        if (!command.acceptsPolicy()) {
            throw new PolicyNotAcceptedException();
        }

        String phone = PhoneNormalizer.normalize(command.phone());
        String email = normalizeEmail(command.email());

        if (userRepository.existsByPhone(phone)) {
            throw new PhoneAlreadyRegisteredException();
        }

        if (email != null && userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        Instant now = Instant.now(clock);
        User user = new User(
                UUID.randomUUID(),
                command.fullName(),
                phone,
                email,
                passwordHasher.hash(command.rawPassword()),
                command.municipality(),
                command.district(),
                command.policyVersion(),
                now,
                now);

        return userRepository.save(user);
    }

    private static String normalizeEmail(String email) {
        return (email == null || email.isBlank()) ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
