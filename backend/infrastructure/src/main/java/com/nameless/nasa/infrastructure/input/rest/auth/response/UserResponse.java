package com.nameless.nasa.infrastructure.input.rest.auth.response;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String phone,
        String email,
        String municipality,
        String district) {
}
