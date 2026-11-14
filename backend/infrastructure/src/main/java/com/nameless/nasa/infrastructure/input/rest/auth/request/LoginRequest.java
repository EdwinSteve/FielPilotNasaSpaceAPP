package com.nameless.nasa.infrastructure.input.rest.auth.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Phone number is required")
    String phone,
    @NotBlank(message = "Password is required")
    String password
) {
}
