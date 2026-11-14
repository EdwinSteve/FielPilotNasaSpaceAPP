package com.nameless.nasa.domain.command;

public record RegisterUserCommand(
        String fullname,
        String phone,
        String email,
        String rawPassword,
        String municipality,
        String district,
        boolean acceptsPolicy,
        String policyVersion
) {
}
