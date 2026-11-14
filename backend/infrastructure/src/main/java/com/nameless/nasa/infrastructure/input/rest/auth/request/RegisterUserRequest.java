package com.nameless.nasa.infrastructure.input.rest.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
    @NotBlank(message = "Full name is required")
    @Size(max = 120, message = "Full name must not exceed 120 characters")
    String fullname,
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,20}$", message = "Phone number is invalid")
    String phone,
    @Email
    @Size(max = 160, message = "Email must not exceed 160 characters")
    String email,
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    String password,
    @NotBlank(message = "Municipality is required")
    @Size(max = 80, message = "Municipality must not exceed 80 characters")
    String municipality,
    @NotBlank(message = "District is required")
    @Size(max = 80, message = "District must not exceed 80 characters")
    String district,
    boolean acceptsPolicy,
    @NotBlank(message = "Policy version is required")
    @Size(max = 20, message = "Policy version must not exceed 20 characters")
    String policyVersion) {
}
